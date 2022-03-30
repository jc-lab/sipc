import {FrameConverter, FrameHandlers} from '../protocol/frame-converter';
import {PacketReader} from '../protocol/packet-reader';
import {ClientHelloFrame} from '../proto/sipc_frames_pb';
import * as jspb from 'google-protobuf';

const frameConverter = FrameConverter.getInstance();
export class ClientContext {
  private readonly _reader: PacketReader = new PacketReader();
  private readonly _preRoutedFrameHandlers: FrameHandlers;
  private readonly _doClose: () => void;
  private readonly _doWrite: (chunk: Uint8Array) => Promise<void>;
  private _frameHandlers: FrameHandlers | null = null;

  constructor(
    decideRoute: (channelId: string) => Promise<FrameHandlers>,
    doClose: () => void,
    doWrite: (chunk: Uint8Array) => Promise<void>
  ) {
    this._doClose = doClose;
    this._doWrite = doWrite;
    this._preRoutedFrameHandlers = {
      onClientHello: (frame: ClientHelloFrame): Promise<void> => {
        return decideRoute(frame.getChannelId())
          .then((frameHandlers) => {
            this._frameHandlers = frameHandlers;
            if (!frameHandlers.onClientHello) {
              return Promise.reject(new Error('no handler for ClientHello'));
            }
            return frameHandlers.onClientHello(frame);
          })
          .catch((err) => this.feedError(err));
      }
    };
  }

  public addChunk(chunk: Buffer): void {
    this._reader.addChunk(chunk);

    const next = () => {
      const parsed = this._reader.read();
      if (parsed) {
        const [frameType, payload] = parsed;
        frameConverter.handleFrame(this._frameHandlers || this._preRoutedFrameHandlers, frameType, payload)
          .then(() => {
            next();
          })
          .catch((err) => {
            this._doClose();
            this.feedError(err);
          });
      }
    };
    next();
  }

  public sendFrame(message: jspb.Message): Promise<void> {
    const type = frameConverter.findTypeFrom(message);
    const serialized = message.serializeBinary();
    const buffer = Buffer.alloc(4);
    const totalSize = serialized.length + 4;
    buffer.writeUInt8(totalSize & 0xff, 0);
    buffer.writeUInt16LE(totalSize >> 8, 1);
    buffer.writeUInt8(type, 3);
    return this._doWrite(Buffer.concat([buffer, serialized]));
  }

  public feedError(err: any) {
    if (this._frameHandlers && this._frameHandlers.onClose) {
      this._frameHandlers.onClose(err);
    } else {
      console.error(err);
    }
  }

  public feedClose() {
    if (this._frameHandlers && this._frameHandlers.onClose) {
      this._frameHandlers.onClose();
    }
  }

  public doClose() {
    this._doClose();
  }
}
