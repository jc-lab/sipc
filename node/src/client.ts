import * as net from 'net';
import * as streams from 'stream';
import duplexify from 'duplexify';
import * as noise from 'node-noise';
import {
  ConnectInfo,
  ClientHelloPayload
} from './proto/sipc_pb';

export interface ClientOptions {
  handshakeTimeout: number;
}

export class Client {
  private readonly _connectInfo: ConnectInfo;
  private readonly _options: ClientOptions;
  private readonly _noise: noise.Noise;

  private _noiseConnection!: noise.SecuredConnection;

  private _duplex = new duplexify();

  constructor(connectInfo: string);
  constructor(connectInfo: string, options: Partial<ClientOptions>);
  constructor(options: Partial<ClientOptions> & {connectInfo: string});
  constructor(
    arg1: any,
    arg2?: Partial<ClientOptions>
  ) {
    let connectInfo: ConnectInfo | null = null;
    let options: ClientOptions = {
      handshakeTimeout: 3000
    };
    if (typeof arg1 === 'string') {
      connectInfo = ConnectInfo.deserializeBinary(Buffer.from(arg1, 'base64'));
    } else {
      options = arg1 as ClientOptions;
      if ('connectInfo' in arg1) {
        connectInfo = ConnectInfo.deserializeBinary(Buffer.from(arg1._connectInfo as string, 'base64'));
      }
    }
    if (!connectInfo) {
      throw new Error('require connectInfo');
    }
    this._connectInfo = connectInfo;
    this._options = options;
    this._noise = new noise.Noise({
      protocol: 'Noise_XK_25519_ChaChaPoly_SHA256'
    });
  }

  public connect(): Promise<streams.Duplex> {
    const socket = net.connect(this._connectInfo.getTransportAddress());
    const pbStream = noise.createPbStream();

    return new Promise<streams.Duplex>((resolve, reject) => {
      let handshaked = false;
      socket
        .on('error', (err) => {
          if (handshaked) {
            console.log('ERROR', err);
            this._duplex.emit('error', err);
          } else {
            handshaked = true;
            reject(err);
          }
        })
        .on('connect', () => {
          pbStream
            .pipe(socket)
            .pipe(pbStream)
            .on('close', () => {
              console.error('closed');
            });

          const hello = new ClientHelloPayload();
          hello.setConnectionId(this._connectInfo.getConnectionId());

          let handshakeStep = 0;
          this._noise.secureConnection({
            connection: pbStream,
            isInitiator: true,
            remoteStaticPublicKey: this._connectInfo.getPublicKey() as Uint8Array,
            handshakeHandler: {
              onReadMessage(session, payload) {
                return Promise.resolve(true);
              },
              beforeWriteMessage(session) {
                handshakeStep++;
                if (handshakeStep === 1) {
                  return Promise.resolve(hello.serializeBinary());
                }
                return Promise.resolve(null);
              }
            }
          })
            .then((connection) => {
              handshaked = true;
              this._noiseConnection = connection;

              this._duplex.setReadable(connection.conn);
              this._duplex.setWritable(connection.conn);

              resolve(this._duplex);
            })
            .catch((err) => {
              socket.destroy(err);
            });
        });
    });
  }
}
