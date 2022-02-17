export interface EphemeralKeyPair {

}

export interface EphemeralKeyPairGenerator {
  readonly algorithm: string;
  generate(): Promise<EphemeralKeyPair>;
}

export interface EphemeralKeyAlgorithmFactory {
  getAlgorithms(): string[];
  //
  // EphemeralKeyPairGenerator getKeyPairGenerator(String algorithm);
  // EphemeralKeyPairGenerator getHostKeyPairGenerator();
}
