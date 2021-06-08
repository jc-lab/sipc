package kr.jclab.javautils.sipc;

import kr.jclab.javautils.sipc.channel.ClientContext;
import kr.jclab.javautils.sipc.channel.stdio.StdioChannelHost;
import kr.jclab.javautils.sipc.crypto.CryptoException;
import kr.jclab.javautils.sipc.crypto.DefaultEphemeralKeyAlgorithmsFactory;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyAlgorithmFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

public class ProcessStdioSipcHost extends ProcessSipcHost {
    private ProcessStdioSipcHost(
            EphemeralKeyAlgorithmFactory keyPairGenerator,
            Executor executor
    ) throws CryptoException {
        super(StdioChannelHost.getInstance(), keyPairGenerator, executor);
        this.channel.attachClientContext(this.clientContext);
    }

    @Override
    public void attachProcess(Process process) {
        super.attachProcess(process);
        this.ioThread.start();
    }
    
    private final Thread ioThread = new Thread(() -> {
        InputStream stdoutStream = this.process.getInputStream();
        InputStream stderrStream = this.process.getErrorStream();

        byte[] buffer = new byte[2048];

        try {
            boolean tick = false;
            while (this.process.isAlive() || true) {
                boolean processed = false;
                int readLength;

                if (stdoutStream.available() > 0) {
                    processed = true;
                    readLength = stdoutStream.read(buffer);
                    this.clientContext.feedData(buffer, readLength);
                }
                if (stderrStream.available() > 0) {
                    processed = true;
                    readLength = stderrStream.read(buffer);
                    System.err.println(new String(buffer, 0, readLength));
                }

                if (!processed) {
                    if (tick) {
                        Thread.yield();
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                tick = !tick;
            }
            this.feedDone();
        } catch (IOException e) {
            this.feedError(e);
        }
    });

    private class StdioClientContext extends ClientContext {
        public StdioClientContext() {
            super();
            this.recvSetFrameHandlers(ProcessStdioSipcHost.this.channel);
        }

        public void feedData(byte[] data, int length) throws IOException {
            this.receivingBuffer.put(data, 0, length);
            this.recvAfterReadRaw();
        }

        @Override
        protected void recvDecideRoute(String channelId) {}

        @Override
        protected void sendRaw(byte[] data) throws IOException {
            process.getOutputStream().write(data);
        }
    }
    private final StdioClientContext clientContext = new StdioClientContext();

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EphemeralKeyAlgorithmFactory keyPairGenerator = null;
        private Executor executor = null;

        public Builder keyPairGenerator(EphemeralKeyAlgorithmFactory keyPairGenerator) {
            this.keyPairGenerator = keyPairGenerator;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public ProcessStdioSipcHost build() throws CryptoException {
            if (this.keyPairGenerator == null) {
                this.keyPairGenerator = DefaultEphemeralKeyAlgorithmsFactory.getInstance();
            }
            return new ProcessStdioSipcHost(
                    this.keyPairGenerator,
                    this.executor
            );
        }
    }
}
