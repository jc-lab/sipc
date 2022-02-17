import * as bson from 'bson';
import {ConnectInfo, ConnectInfoBase, BasicConnectInfo, TcpConnectInfo} from './types';

export class ConnectInfoCodec {
  public serialize(data: ConnectInfoBase): string {
    return bson.serialize(data.toData()).toString('base64')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');
  }

  public decode(input: string): ConnectInfoBase {
    let b64input = input
      .replace(/-/g, '+')
      .replace(/_/g, '/');
    const pad = b64input.length % 4;
    if (pad) {
      b64input += '='.repeat(4 - pad);
    }
    const doc = bson.deserialize(Buffer.from(b64input, 'base64')) as ConnectInfo;
    if (doc.channel_type === TcpConnectInfo.CHANNEL_TYPE_TCP4) {
      return new TcpConnectInfo(doc);
    } else if (doc.channel_type === TcpConnectInfo.CHANNEL_TYPE_TCP6) {
      return new TcpConnectInfo(doc);
    } else {
      return new BasicConnectInfo(doc);
    }
  }
}
