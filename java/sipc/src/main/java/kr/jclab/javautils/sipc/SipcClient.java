package kr.jclab.javautils.sipc;

import com.google.protobuf.GeneratedMessageV3;
import kr.jclab.javautils.sipc.bson.SipcBsonHelper;
import kr.jclab.javautils.sipc.bson.entity.ConnectInfo;
import kr.jclab.javautils.sipc.bson.entity.TcpConnectInfo;
import kr.jclab.javautils.sipc.channel.IpcChannelListener;
import kr.jclab.javautils.sipc.channel.IpcChannelStatus;
import kr.jclab.javautils.sipc.channel.tcp.TcpChannelClient;
import kr.jclab.javautils.sipc.crypto.*;
import kr.jclab.javautils.sipc.event.EventChannel;
import kr.jclab.javautils.sipc.handler.HandshakeHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SipcClient implements IpcChannelListener {
    private final ConnectInfo connectInfo;
    private final EphemeralKeyPair ephemeralKeyPair;
    private final TcpChannelClient channelClient;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final EventChannel eventChannel;
    private HandshakeHandler handshakeHandler = null;

    public SipcClient(String connectInfo) throws CryptoException, IOException {
        connectInfo = connectInfo
                .replaceAll("-", "+")
                .replaceAll("_", "/");
        int pad = connectInfo.length() % 4;
        if (pad > 0) {
            StringBuilder stringBuilder = new StringBuilder(connectInfo);
            for (int i=0; i<pad; i++) stringBuilder.append("=");
            connectInfo = stringBuilder.toString();
        }
        this.connectInfo = SipcBsonHelper.getInstance().decodeConnectInfo(Base64.getDecoder().decode(connectInfo));

        EphemeralKeyAlgorithmFactory ephemeralKeyAlgorithmFactory = DefaultEphemeralKeyAlgorithmsFactory.getInstance();
        EphemeralKeyPairGenerator ephemeralKeyPairGenerator = ephemeralKeyAlgorithmFactory.getKeyPairGenerator(this.connectInfo.getAlgo());
        this.ephemeralKeyPair = ephemeralKeyPairGenerator.generate();

        TcpChannelClient channelClient = null;
        if (this.connectInfo instanceof TcpConnectInfo) {
            TcpConnectInfo tcpConnectInfo = (TcpConnectInfo) this.connectInfo;
            channelClient = TcpChannelClient.connect(tcpConnectInfo, this.ephemeralKeyPair, this);
        } else {
            throw new IOException("Not supported connect info: " + this.connectInfo);
        }
        this.eventChannel = new EventChannel(
                this.executorService,
                channelClient.getIpcChannel()
        );
        this.channelClient = channelClient;
    }

    public static SipcClient connect(String connectInfo) throws CryptoException, IOException {
        return new SipcClient(connectInfo);
    }

    public int run() throws IOException {
        this.channelClient.run();
        return 0;
    }

    public void close() {
        if (this.channelClient == null) {
            return ;
        }
        this.channelClient.close();
    }

    public SipcClient onHandshaked(HandshakeHandler handshakeHandler) {
        this.handshakeHandler = handshakeHandler;
        return this;
    }

    public <T extends GeneratedMessageV3> SipcClient onRequest(
            String requestName,
            T requestMessageDefaultInstance,
            EventChannel.RequestHandler<T> handler
    ) {
        this.eventChannel.onRequest(
                requestName,
                requestMessageDefaultInstance,
                handler
        );
        return this;
    }

    @Override
    public void onChangeChannelStatus(IpcChannelStatus channelStatus) {
        if (IpcChannelStatus.Established.equals(channelStatus))
        if (this.handshakeHandler != null) {
            this.handshakeHandler.handshake();
        }
    }

    @Override
    public void onError(Throwable e) {
        System.err.println("onError");
        e.printStackTrace();
    }
}
