// package: kr.jclab.sipc
// file: sipc.proto

import * as jspb from "google-protobuf";

export class ConnectInfo extends jspb.Message {
  getConnectionId(): string;
  setConnectionId(value: string): void;

  getTransportType(): TransportTypeMap[keyof TransportTypeMap];
  setTransportType(value: TransportTypeMap[keyof TransportTypeMap]): void;

  getTransportAddress(): string;
  setTransportAddress(value: string): void;

  getPublicKey(): Uint8Array | string;
  getPublicKey_asU8(): Uint8Array;
  getPublicKey_asB64(): string;
  setPublicKey(value: Uint8Array | string): void;

  getAllowReconnect(): boolean;
  setAllowReconnect(value: boolean): void;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): ConnectInfo.AsObject;
  static toObject(includeInstance: boolean, msg: ConnectInfo): ConnectInfo.AsObject;
  static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
  static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
  static serializeBinaryToWriter(message: ConnectInfo, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): ConnectInfo;
  static deserializeBinaryFromReader(message: ConnectInfo, reader: jspb.BinaryReader): ConnectInfo;
}

export namespace ConnectInfo {
  export type AsObject = {
    connectionId: string,
    transportType: TransportTypeMap[keyof TransportTypeMap],
    transportAddress: string,
    publicKey: Uint8Array | string,
    allowReconnect: boolean,
  }
}

export class ClientHelloPayload extends jspb.Message {
  getConnectionId(): string;
  setConnectionId(value: string): void;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): ClientHelloPayload.AsObject;
  static toObject(includeInstance: boolean, msg: ClientHelloPayload): ClientHelloPayload.AsObject;
  static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
  static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
  static serializeBinaryToWriter(message: ClientHelloPayload, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): ClientHelloPayload;
  static deserializeBinaryFromReader(message: ClientHelloPayload, reader: jspb.BinaryReader): ClientHelloPayload;
}

export namespace ClientHelloPayload {
  export type AsObject = {
    connectionId: string,
  }
}

export interface TransportTypeMap {
  KUNKNOWN: 0;
  KUNIXDOMAINSOCKET: 1;
  KWINDOWSNAMEDPIPE: 2;
  KTCP: 101;
}

export const TransportType: TransportTypeMap;

