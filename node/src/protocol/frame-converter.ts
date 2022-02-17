import {
  AlertFrame,
  ClientHelloFrame,
  ServerHelloFrame,
  EncryptedWrappedData
} from '../proto/sipc_frames_pb';
import * as jspb from 'google-protobuf';

export interface FrameHandlers {
  onAlertFrame?(frame: AlertFrame): Promise<void>;
  onServerHello?(frame: ServerHelloFrame): Promise<void>;
  onClientHello?(frame: ClientHelloFrame): Promise<void>;
  onWrappedData?(frame: EncryptedWrappedData): Promise<void>;
  onClose?(err?: Error | null): void;
}

export interface FrameHandlerHolder<T extends jspb.Message> {
  type: number;
  supplier: (bytes: Uint8Array) => T;
  getHandler: (handlers: FrameHandlers) => Function | undefined;
  class: typeof jspb.Message;
}

export class FrameConverter {
  private readonly _mappings: Record<number, FrameHandlerHolder<any>> = {};

  public static getInstance(): FrameConverter {
    return instance;
  }

  constructor() {
    this._mappings[0xf1] = {
      type: 0xf1,
      supplier: (data) => AlertFrame.deserializeBinary(data),
      getHandler: (handlers) => handlers.onAlertFrame,
      class: AlertFrame
    };
    this._mappings[0x11] = {
      type: 0x11,
      supplier: (data) => ClientHelloFrame.deserializeBinary(data),
      getHandler: (handlers) => handlers.onClientHello,
      class: ClientHelloFrame
    };
    this._mappings[0x12] = {
      type: 0x12,
      supplier: (data) => ServerHelloFrame.deserializeBinary(data),
      getHandler: (handlers) => handlers.onServerHello,
      class: ServerHelloFrame
    };
    this._mappings[0x1a] = {
      type: 0x1a,
      supplier: (data) => EncryptedWrappedData.deserializeBinary(data),
      getHandler: (handlers) => handlers.onWrappedData,
      class: EncryptedWrappedData
    };
  }

  public handleFrame(handlers: FrameHandlers, type: number, message: Buffer): Promise<void> {
    const holder = this._mappings[type];
    if (!holder) return Promise.reject(new Error('unknown frame type: ' + type));
    const frame = holder.supplier(message);
    const handler = holder.getHandler(handlers);
    if (!handler) return Promise.reject(new Error('no handler for frame type: ' + type));
    return handler.apply(handlers, [frame]) as Promise<void>;
  }

  public findTypeFrom(message: jspb.Message): number {
    const item = Object.values(this._mappings)
      .find(v => message instanceof v.class);
    if (!item) return -1;
    return item.type;
  }
}

const instance = new FrameConverter();
