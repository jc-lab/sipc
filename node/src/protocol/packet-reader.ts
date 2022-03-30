export class PacketReader {
  private offset: number = 0;
  private chunk: Buffer | null = null;
  private chunkLength: number = 0;

  public addChunk(chunk: Buffer): void {
    if (!this.chunk || this.offset === this.chunkLength) {
      this.chunk = chunk;
      this.chunkLength = chunk.length;
      this.offset = 0;
      return;
    }

    const newChunkLength = chunk.length;
    const newLength = this.chunkLength + newChunkLength;

    if (newLength > this.chunk.length) {
      let newBufferLength = this.chunk.length * 2;
      while (newLength >= newBufferLength) {
        newBufferLength *= 2;
      }
      const newBuffer = Buffer.alloc(newBufferLength);
      this.chunk.copy(newBuffer);
      this.chunk = newBuffer;
    }
    chunk.copy(this.chunk, this.chunkLength);
    this.chunkLength = newLength;
  }

  public read(): [number, Buffer] | false {
    if(this.chunkLength < (4 + this.offset)) {
      return false
    }
    if (!this.chunk) throw new Error('Illegal state');

    let length = this.chunk.readUInt8(this.offset);
    length |= this.chunk.readUInt16LE(this.offset + 1) << 8;
    const frameType = this.chunk.readUInt8(this.offset + 3);

    //next item spans more chunks than we have
    const remaining = this.chunkLength - this.offset;
    if (length > remaining) {
      return false;
    }

    length -= 4;

    this.offset += 4;
    const result = this.chunk.slice(this.offset, this.offset + length);
    this.offset += length;

    return [frameType, result];
  }
}
