import {Any} from 'google-protobuf/google/protobuf/any_pb';
import {Message} from 'google-protobuf';
import {CalleeRequestContext, CallerRequestContext} from './host/request-context';
import {
  EventComplete,
  EventProgress,
  EventRequest,
  EventStatus,
  WrappedData
} from './proto/sipc_frames_pb';
import {ProtoMessageHouse} from './proto-message-house';
import {IpcChannel} from './host/channel-host';

export type RequestDeserializer<T extends Message> = (data: Uint8Array) => T | Promise<T>;

export interface RequestDefinition<T extends Message> {
  name: string;
  deserializer: RequestDeserializer<T>;
  handler: (context: CalleeRequestContext<T>) => void;
}

export interface RequestParam<R extends Message, P extends Message> {
  name: string;
  requestData: Message;
  progressDeserializer?: RequestDeserializer<P>;
  progressHandler?: (message: P) => void;
  responseDeserializer: RequestDeserializer<R>;
}

export class EventChannel {
  private readonly transport: IpcChannel;

  private readonly _requestMethods: Record<string, RequestDefinition<any>> = {};
  private readonly _runningCalls: Record<string, CallerRequestContext<any, any>> = {};

  public constructor(transport: IpcChannel) {
    this.transport = transport;

    transport.registerWrappedData(
      new EventRequest(),
      (data) => EventRequest.deserializeBinary(data),
      (payload) => {
        const requestMethod = this._requestMethods[payload.getMethodName()];
        if (!requestMethod) {
          console.warn(new Error('NOT EXISTS METHOD: ' + payload.getMethodName()));
          const responseData = new EventComplete();
          responseData.setStreamId(payload.getStreamId());
          responseData.setStatus(EventStatus.METHOD_NOT_EXIST);
          return this.sendApplicationData(responseData);
        }
        return Promise.resolve(requestMethod.deserializer(payload.getData_asU8()))
          .then((data) => {
            const context = new CalleeRequestContext(data);
            requestMethod.handler(context);
          });
      }
    );
    transport.registerWrappedData(
      new EventProgress(),
      (data) => EventProgress.deserializeBinary(data),
      (payload) => {
        const context = this._runningCalls[payload.getStreamId()];
        if (!context) {
          return Promise.reject(new Error('NOT EXISTS STREAM ID: ' + payload.getStreamId()));
        }
        context.progress(payload.getData_asU8());
        return Promise.resolve();
      }
    );
    transport.registerWrappedData(
      new EventComplete(),
      (data) => EventComplete.deserializeBinary(data),
      (payload) => {
        const context = this._runningCalls[payload.getStreamId()];
        if (!context) {
          return Promise.reject(new Error('NOT EXISTS STREAM ID: ' + payload.getStreamId()));
        }
        switch (payload.getStatus()) {
          case EventStatus.OK:
            context.resolve(payload.getData_asU8());
            break;
          case EventStatus.METHOD_NOT_EXIST:
            context.reject(new Error('Method not exist'));
            break;
          case EventStatus.CANCELLED:
            context.reject(new Error('Cancelled by remote'));
            break;
        }
        return Promise.resolve();
      }
    );
  }

  public onRequest<T extends Message>(def: RequestDefinition<T>): this {
    this._requestMethods[def.name] = def;
    return this;
  }

  public request<R extends Message, P extends Message>(param: RequestParam<R, P>): Promise<R> {
    return new Promise<R>((resolve, reject) => {
      const requestContext = new CallerRequestContext<R, P>(param, {resolve, reject});
      this._runningCalls[requestContext.streamId] = requestContext;

      const eventRequest = new EventRequest();
      eventRequest.setStreamId(requestContext.streamId);
      eventRequest.setMethodName(param.name);
      eventRequest.setData(param.requestData.serializeBinary());
      this.sendApplicationData(eventRequest)
        .catch((err) => {
          delete this._runningCalls[requestContext.streamId];
          requestContext.reject(err);
        });
    });
  }

  public cancelAll(): void {
    Object.keys(this._runningCalls)
      .forEach((key) => {
        const context = this._runningCalls[key];
        delete this._runningCalls[key];
        context.reject(new Error('Request is cancelled'));
      });
  }

  protected sendApplicationData(data: Message): Promise<void> {
    const wrappedData = new WrappedData();
    wrappedData.setVersion(1);

    const message = new Any();
    message.setTypeUrl(ProtoMessageHouse.getTypeUrl(data));
    message.setValue(data.serializeBinary());
    wrappedData.setMessage(message);

    return this.transport.sendWrappedData(wrappedData);
  }
}
