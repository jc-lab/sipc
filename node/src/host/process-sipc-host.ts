import {ChannelHost, IpcChannel} from './channel-host';
import {ChildProcess} from 'child_process';
import * as cc from 'commons-crypto';
import {SipcHost} from './sipc-host';
import {EventChannel} from '../event-channel';

export type ProcessSupplier = (connectInfo: string) => ChildProcess | Promise<ChildProcess>;

export interface ProcessSipcHost {
  on(eventName: string | symbol, listener: (...args: any[]) => void): this;
  emit(eventName: string | symbol, ...args: any[]): boolean;

  on(eventName: 'exit', listener: (code: number | null, signal: NodeJS.Signals | null) => void): this;
  emit(eventName: 'exit', code: number | null, signal: NodeJS.Signals | null): this;
}

export class ProcessSipcHost extends SipcHost {
  private _ipcChannel!: IpcChannel;
  private _process!: ChildProcess;
  private _eventChannel!: EventChannel;

  public static builder(channelHost: ChannelHost): ProcessSipcHostBuilder {
    return new ProcessSipcHostBuilder(channelHost);
  }

  public static create(
    channelHost: ChannelHost,
    processSupplier: ProcessSupplier,
  ): ProcessSipcHost {
    const instance = new ProcessSipcHost(
      channelHost,
      processSupplier
    );
    process.nextTick(() => {
      instance.start()
        .catch((err) => instance.emit('error', err));
    });
    return instance;
  }

  constructor(
    private readonly channelHost: ChannelHost,
    private readonly processSupplier: ProcessSupplier,
  ) {
    super();
  }

  public getEventChannel(): EventChannel {
    return this._eventChannel;
  }

  public close(): Promise<void> {
    this._eventChannel.cancelAll();
    return Promise.resolve()
      .then(() => {
        if (this._ipcChannel) {
          return this._ipcChannel.close();
        }
        return Promise.resolve();
      })
      .then(() => this.kill());
  }

  public kill(): Promise<void> {
    if (!this._process.killed) {
      this._process.kill(9);
    }
    return Promise.resolve();
  }

  private start(): Promise<this> {
    const algorithm = cc.createAsymmetricAlgorithm(cc.AsymmetricAlgorithmType.x25519);
    return Promise.resolve(algorithm.generateKeyPair())
      .then((res) => {
        const { privateKey, publicKey } = res;
        const channel = this.channelHost.createChannel(privateKey);
        channel
          .on('handshake', (peer) => this.emit('handshake', peer));
        this._ipcChannel = channel;
        this._eventChannel = new EventChannel(channel);
        return Promise.resolve(this.processSupplier(channel.connectInfo))
          .then((process) => {
            this._process = process;
            process
              .on('error', (err) => {
                this.emit('error', err);
              })
              .on('exit', (code: number | null, signal: NodeJS.Signals | null) => {
                this.emit('exit', code, signal);
              });
          });
      })
      .then(() => this);
  }
}

export class ProcessSipcHostBuilder {
  private processSupplier: ProcessSupplier | null = null;

  constructor(
    private readonly channelHost: ChannelHost
  ) {
  }

  public process(processSupplier: ProcessSupplier): this {
    this.processSupplier = processSupplier;
    return this;
  }

  public build(): ProcessSipcHost {
    if (!this.processSupplier) throw new Error('no process supplier');
    return ProcessSipcHost.create(
      this.channelHost,
      this.processSupplier
    );
  }
}
