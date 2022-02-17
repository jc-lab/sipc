export interface IpcChannelListener {
  onHandshake(): void;
  onClose(err: any | null): void;
}
