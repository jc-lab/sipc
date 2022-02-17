import * as cc from 'commons-crypto';
import * as events from 'events';
import {WrappedData} from '../proto/sipc_frames_pb';
import {SipcPeer} from './sipc-host';
import {Message} from 'google-protobuf';

export interface IpcChannel extends events.EventEmitter {
  readonly channelId: string;
  readonly connectInfo: string;

  on(eventName: string | symbol, listener: (...args: any[]) => void): this;
  on(eventName: 'error', listener: (err: any) => void): this;
  on(eventName: 'handshake', listener: (peer: SipcPeer) => void): this;

  sendWrappedData(frame: WrappedData): Promise<void>;
  registerWrappedData<T extends Message>(defaultObject: T, deserializer: (payload: Uint8Array) => T | Promise<T>, handler: (payload: T) => Promise<void>);

  close(): Promise<void>;
}

export interface ChannelHost {
  createChannel(key: cc.AsymmetricKeyObject): IpcChannel;
  close(): Promise<void>;
}

