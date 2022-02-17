export interface ConnectInfo {
  channel_id: string;
  channel_type: string;
  algo: string;
  public_key: Buffer;
}

export abstract class ConnectInfoBase implements ConnectInfo {
  public readonly algo!: string;
  public readonly channel_id!: string;
  public readonly channel_type!: string;
  public readonly public_key!: Buffer;

  public abstract toData(): ConnectInfo;
}

export class BasicConnectInfo extends ConnectInfoBase {
  constructor(data: ConnectInfo) {
    super();
    Object.assign(this, data);
  }

  toData(): ConnectInfo & Record<string, any> {
    return {
      channel_id: this.channel_id,
      channel_type: this.channel_type,
      algo: this.algo,
      public_key: this.public_key
    };
  }
}

export class TcpConnectInfo extends ConnectInfoBase {
  public static readonly CHANNEL_TYPE_TCP4: string = 'tcp4';
  public static readonly CHANNEL_TYPE_TCP6: string = 'tcp6';
  public readonly address!: string;
  public readonly port!: number;

  constructor(data: ConnectInfo | TcpConnectInfo) {
    super();
    Object.assign(this, data);
  }

  toData(): ConnectInfo & Record<string, any> {
    return {
      channel_id: this.channel_id,
      channel_type: this.channel_type,
      algo: this.algo,
      public_key: this.public_key,
      address: this.address,
      port: this.port
    };
  }
}
