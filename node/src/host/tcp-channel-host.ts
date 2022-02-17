import * as net from 'net';
import * as uuid from 'uuid';
import * as cc from 'commons-crypto';
import {ConnectInfoCodec, TcpConnectInfo} from '../connect-info';
import {ChannelHost, IpcChannel} from './channel-host';
import {ClientContext} from './client-context';
import {AbstractIpcChannel} from './abstract-ipc-channel';

export interface TcpChannelHostOptions {
  host?: string;
  port?: number;
}

export class TcpIpcChannel extends AbstractIpcChannel {
  private readonly _channelId: string;
  private readonly _channelInfo: TcpConnectInfo;
  private readonly _privateKey: cc.AsymmetricKeyObject;

  constructor(host: string, port: number, algo: string, privateKey: cc.AsymmetricKeyObject, publicKey: Buffer) {
    super();
    this._channelId = uuid.v4();
    this._channelInfo = new TcpConnectInfo({
      channel_type: TcpConnectInfo.CHANNEL_TYPE_TCP4,
      channel_id: this._channelId,
      address: host,
      port: port,
      algo,
      public_key: publicKey
    });
    this._privateKey = privateKey;
  }

  public get channelId(): string {
    return this._channelId;
  }

  public get connectInfo(): string {
    return new ConnectInfoCodec().serialize(this._channelInfo);
  }

  protected get privateKey(): cc.AsymmetricKeyObject {
    return this._privateKey;
  }
}

export class TcpChannelHost implements ChannelHost {
  private readonly _options: TcpChannelHostOptions;
  private _server!: net.Server;
  private _host!: string;
  private _port!: number;

  private _channelMap: Record<string, TcpIpcChannel> = {};

  public static create(options?: TcpChannelHostOptions): Promise<TcpChannelHost> {
    const instance = new TcpChannelHost(options);
    return instance.start();
  }

  constructor(options?: TcpChannelHostOptions) {
    this._options = options || {};
  }

  public createChannel(key: cc.AsymmetricKeyObject): IpcChannel {
    const data = key.toPublicKey().export({
      type: 'specific-public',
      format: 'der'
    });
    const ipcChannel = new TcpIpcChannel(this._host, this._port, 'x25519', key, data);
    this._channelMap[ipcChannel.channelId] = ipcChannel;
    return ipcChannel;
  }

  public close(): Promise<void> {
    return new Promise<void>((resolve, reject) => this._server.close((err) => {
      if (err) reject(err);
      else resolve();
    }));
  }

  private start(): Promise<this> {
    const maxRetries = 10;
    return new Promise<net.Server>((resolve, reject) => {
      const tryBind = (triedCount: number, prevErr: any) => {
        const host = this._options.host || '127.0.0.2';
        const optionPort = this._options.port;
        const port = optionPort || (4096 + Math.round(60904 * Math.random()));
        if ((triedCount >= maxRetries) || (triedCount > 0 && !optionPort)) {
          reject(prevErr);
          return ;
        }

        const server = net.createServer();
        server
          .on('error', (err: any) => {
            if (err.code === 'EADDRINUSE') {
              tryBind(triedCount + 1, err);
            } else {
              reject(err);
            }
          })
          .on('listening', () => {
            this._server = server;
            this._host = host;
            this._port = port;
            resolve(server);
          })
          .listen(port, host);
      };
      tryBind(0, null);
    })
      .then((server) => {
        server
          .on('connection', (socket) => {
            this.newClient(socket);
          });
      })
      .then(() => this);
  }

  private newClient(socket: net.Socket) {
    const clientContext = new ClientContext(
      (channelId) => {
        const foundIpcChannel = this._channelMap[channelId];
        if (!foundIpcChannel) {
          return Promise.reject(new Error('Unknown channel id'));
        }
        foundIpcChannel.attachClientContext(clientContext);
        return Promise.resolve(foundIpcChannel);
      },
      () => {
        socket.end();
      },
      (chunk) => new Promise<void>((resolve, reject) => {
        socket.write(chunk, (err) => {
          if (err) reject(err);
          else resolve();
        });
      })
    );
    socket
      .on('data', (chunk) => clientContext.addChunk(chunk))
      .on('close', (hadError) => {
        if (!hadError) {
          clientContext.feedClose();
        }
      })
      .on('error', (err) => clientContext.feedError(err));
  }
}
