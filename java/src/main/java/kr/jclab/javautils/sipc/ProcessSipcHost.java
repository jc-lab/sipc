package kr.jclab.javautils.sipc;

import kr.jclab.javautils.sipc.channel.ChannelHost;
import kr.jclab.javautils.sipc.crypto.CryptoException;
import kr.jclab.javautils.sipc.crypto.DefaultEphemeralKeyAlgorithmsFactory;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyAlgorithmFactory;

import java.util.concurrent.Executor;

public class ProcessSipcHost extends SipcHost {
    protected Process process = null;

    protected ProcessSipcHost(
            ChannelHost channelHost,
            EphemeralKeyAlgorithmFactory keyPairGenerator,
            Executor executor
    ) throws CryptoException {
        super(channelHost, keyPairGenerator, executor);
    }

    public void attachProcess(Process process) {
        this.process = process;
    }

    public static Builder builder(ChannelHost channelHost) {
        return new Builder(channelHost);
    }

    public static class Builder {
        private final ChannelHost channelHost;
        private EphemeralKeyAlgorithmFactory keyPairGenerator = null;
        private Executor executor = null;

        public Builder(ChannelHost channelHost) {
            this.channelHost = channelHost;
        }

        public Builder keyPairGenerator(EphemeralKeyAlgorithmFactory keyPairGenerator) {
            this.keyPairGenerator = keyPairGenerator;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public ProcessSipcHost build() throws CryptoException {
            if (this.keyPairGenerator == null) {
                this.keyPairGenerator = DefaultEphemeralKeyAlgorithmsFactory.getInstance();
            }
            return new ProcessSipcHost(
                    this.channelHost,
                    this.keyPairGenerator,
                    this.executor
            );
        }
    }
}
