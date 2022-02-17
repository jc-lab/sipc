import {IpcChannel} from './channel-host';
import {FrameHandlers} from '../protocol/frame-converter';
import {ClientContext} from './client-context';
import * as crypto from 'crypto';
import * as events from 'events';
import * as cc from 'commons-crypto';
import {
  AlertFrame,
  ClientHelloFrame,
  EncryptedWrappedData,
  ServerHelloEncrpytedData,
  ServerHelloFrame, WrappedData
} from '../proto/sipc_frames_pb';
import * as jspb from 'google-protobuf';
import {Message} from 'google-protobuf';
import {ProtoMessageHouse} from '../proto-message-house';
import {Any} from 'google-protobuf/google/protobuf/any_pb';

const safeBufferConcat = (buffers: (Buffer | null)[]) => Buffer.concat(buffers.filter(v => !!v) as Buffer[]);

class WrappedDataHandler<T extends Message> {
  constructor(
    public readonly deserializer: (payload: Uint8Array) => T | Promise<T>,
    public readonly handler: (payload: T) => Promise<void>
  ) {}
}

export abstract class AbstractIpcChannel extends events.EventEmitter implements IpcChannel, FrameHandlers {
  public abstract get channelId(): string;
  public abstract get connectInfo(): string;

  protected abstract get privateKey(): cc.AsymmetricKeyObject;

  private _clientContext: ClientContext | null = null;
  private _sharedMasterSecret!: Buffer;

  private _myCounter: bigint = 0n;
  private _peerCounter: bigint = 0n;

  private _wrappedDataHandlers: Record<string, WrappedDataHandler<any>> = {};

  constructor() {
    super();
  }

  attachClientContext(clientContext: ClientContext): void {
    this._clientContext = clientContext;
  }

  onAlertFrame(frame: AlertFrame): Promise<void> {
    return Promise.resolve(undefined);
  }

  onClientHello(frame: ClientHelloFrame): Promise<void> {
    return new Promise<Buffer>((resolve, reject) => {
      crypto.randomFill(Buffer.alloc(32), (err, buf) => {
        if (err) {
          reject(err);
          return ;
        }
        resolve(buf);
      });
    })
      .then((serverNonce) => {
        const remotePublicKey = this.privateKey.getKeyAlgorithm().keyImport(
          Buffer.from(frame.getEphemeralPublicKey_asU8()), {
            format: 'der'
          }
        );
        const derivedSecret = this.privateKey.dhComputeSecret(remotePublicKey);

        const mac = crypto.createHmac('sha256', derivedSecret);
        mac.update(serverNonce);
        mac.update(frame.getClientNonce_asU8());
        this._sharedMasterSecret = mac.digest();

        const encryptedData = new ServerHelloEncrpytedData();
        encryptedData.setVersion(1);
        const serverHelloFrame = new ServerHelloFrame();
        serverHelloFrame.setServerNonce(serverNonce);
        return this.wrapDataToSend(encryptedData.serializeBinary())
          .then((wrappedData) => {
            serverHelloFrame.setEncryptedData(wrappedData);
            return this.sendFrame(serverHelloFrame);
          });
      })
      .then(() => {
        this.emit('handshake', {} as any);
      });
  }

  onServerHello(frame: ServerHelloFrame): Promise<void> {
    return Promise.resolve(undefined);
  }

  onWrappedData(frame: EncryptedWrappedData): Promise<void> {
    return this.unwrapDataToRecv(frame)
      .then((plainText) => {
        const wrappedData = WrappedData.deserializeBinary(plainText);
        const packedMessage = wrappedData.getMessage() as Any;
        const typeUrl = packedMessage.getTypeUrl();
        const handler = this._wrappedDataHandlers[typeUrl];
        if (!handler) return Promise.reject(new Error('unknown type url: ' + typeUrl));
        return Promise.resolve(handler.deserializer(packedMessage.getValue_asU8()))
          .then((message) => {
            return handler.handler(message);
          });
      });
  }

  onClose(err?: Error | null): void {
    if (err) {
      this.emit('error', err);
    }
  }

  public sendWrappedData(message: WrappedData): Promise<void> {
    return this.wrapDataToSend(message.serializeBinary())
      .then((wrapped) => this.sendFrame(wrapped));
  }

  public registerWrappedData<T extends Message>(defaultObject: T, deserializer: (payload: Uint8Array) => T | Promise<T>, handler: (payload: T) => Promise<void>) {
    const typeUrl = ProtoMessageHouse.getTypeUrl(defaultObject);
    this._wrappedDataHandlers[typeUrl] = new WrappedDataHandler<any>(deserializer, handler);
  }

  public close(): Promise<void> {
    if (this._clientContext) {
      this._clientContext.doClose();
    }
    return Promise.resolve();
  }

  private sendFrame(message: jspb.Message): Promise<void> {
    if (!this._clientContext) return Promise.reject(new Error('Not connected yet'));
    return this._clientContext.sendFrame(message);
  }

  private wrapDataToSend(plainText: Uint8Array): Promise<EncryptedWrappedData> {
    const counter = this._myCounter++;
    try {
      const iv = this.generateSecret('siv', counter, 16);
      const key = this.generateSecret('sky', counter, 32);
      const aes = cc.createCipher('aes-256-gcm');
      if (!aes) return Promise.reject(new Error('cipher error'));
      aes.init({
        key, iv, authTagLength: 16
      });
      const cipherText = safeBufferConcat([
        aes.update(Buffer.from(plainText)),
        aes.final()
      ]);
      const authTag = aes.getAuthTag();
      const wrappedData = new EncryptedWrappedData();
      wrappedData.setVersion(1);
      wrappedData.setCipherText(cipherText);
      wrappedData.setAuthTag(authTag);
      return Promise.resolve(wrappedData);
    } catch (err) {
      return Promise.reject(err);
    }
  }

  private unwrapDataToRecv(cipherFrame: EncryptedWrappedData): Promise<Buffer> {
    const counter = this._peerCounter++;
    try {
      const iv = this.generateSecret('civ', counter, 16);
      const key = this.generateSecret('cky', counter, 32);
      const aes = cc.createDecipher('aes-256-gcm');
      if (!aes) return Promise.reject(new Error('cipher error'));
      aes.init({
        key, iv, authTagLength: 16
      });
      aes.setAuthTag(Buffer.from(cipherFrame.getAuthTag_asU8()));
      const plainText = safeBufferConcat([
        aes.update(Buffer.from(cipherFrame.getCipherText())),
        aes.final()
      ]);
      return Promise.resolve(plainText);
    } catch (err) {
      return Promise.reject(err);
    }
  }

  private generateSecret(type: string, counter: bigint, size: number): Buffer {
    const buf = Buffer.alloc(type.length + 8);
    buf.write(type, 0, 'utf8');
    buf.writeBigUint64LE(counter, type.length);
    const mac = crypto.createHmac('sha256', this._sharedMasterSecret);
    mac.update(buf);
    const output = mac.digest();
    if (output.length > size) {
      return output.slice(0, size);
    }
    return output;
  }
}
