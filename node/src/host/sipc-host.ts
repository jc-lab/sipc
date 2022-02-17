import * as events from 'events';
import {EventChannel} from '../event-channel';

export interface SipcPeer {
  channelId: string;
}

export interface SipcHost extends events.EventEmitter {
  on(eventName: string | symbol, listener: (...args: any[]) => void): this;
  emit(eventName: string | symbol, ...args: any[]): boolean;

  on(eventName: 'error', listener: (err: any) => void): this;
  emit(eventName: 'error', err: any): boolean;

  on(eventName: 'handshake', listener: (peer: SipcPeer) => void): this;
  emit(eventName: 'handshake', peer: SipcPeer): boolean;

  getEventChannel(): EventChannel;
  close(): Promise<void>;
}

export abstract class SipcHost extends events.EventEmitter implements SipcHost {
}
