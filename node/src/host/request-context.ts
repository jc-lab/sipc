import {
  Message
} from 'google-protobuf';
import * as uuid from 'uuid';
import {RequestParam} from '../event-channel';

export interface PromiseHandler<R> {
  resolve: (r: R) => void;
  reject: (err: any) => void;
}

export class CalleeRequestContext<T extends Message> {
  constructor(
    public readonly request: T
  ) {
  }
}

export class CallerRequestContext<R extends Message, P extends Message> {
  public readonly streamId: string;
  private _resolved: boolean = false;

  constructor(
    public readonly param: RequestParam<R, P>,
    private readonly promiseHandler: PromiseHandler<R>
  ) {
    this.streamId = uuid.v4();
  }

  public get resolved(): boolean {
    return this._resolved;
  }

  public progress(data: Uint8Array): void {
    if (this.param.progressDeserializer && this.param.progressHandler) {
      const handler = this.param.progressHandler;
      Promise.resolve(this.param.progressDeserializer(data))
        .then((message) => {
          handler(message);
        })
        .catch((err) => console.warn(err));
    }
  }

  public resolve(data: Uint8Array): void {
    if (this._resolved) return ;
    this._resolved = true;
    Promise.resolve(this.param.responseDeserializer(data))
      .then((message) => {
        this.promiseHandler.resolve(message);
      })
      .catch((err) => this.promiseHandler.reject(err));
  }

  public reject(err: any): void {
    if (this._resolved) return ;
    this._resolved = true;
    this.promiseHandler.reject(err);
  }
}
