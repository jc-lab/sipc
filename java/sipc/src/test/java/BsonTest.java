import kr.jclab.javautils.sipc.DefaultChannelType;
import kr.jclab.javautils.sipc.bson.SipcBsonHelper;
import kr.jclab.javautils.sipc.bson.entity.ConnectInfo;
import kr.jclab.javautils.sipc.bson.entity.TcpConnectInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

public class BsonTest {
    @Test
    public void stdioConnectInfoTest() {
        byte[] publicKey = new byte[] { 1, 2, 3, 4, 0, (byte)0xff };

        ConnectInfo connectInfo = ConnectInfo.basicBuilder()
                .channelType(DefaultChannelType.Stdio.value())
                .algo("x25519")
                .publicKey(publicKey)
                .build();

        SipcBsonHelper bsonHelper = SipcBsonHelper.getInstance();

        byte[] encoded = bsonHelper.encode(connectInfo);

        System.out.println("encoded : " + Base64.getEncoder().encodeToString(encoded));

        assert "RQAAAAJhbGdvAAcAAAB4MjU1MTkAAmNoYW5uZWxfdHlwZQAGAAAAc3RkaW8ABXB1YmxpY19rZXkABgAAAAABAgMEAP8A"
                .equals(Base64.getEncoder().encodeToString(encoded));

        ConnectInfo decodeConnectInfo = bsonHelper.decodeConnectInfo(encoded);

        assert connectInfo.getChannelType().equals((decodeConnectInfo.getChannelType()));
        assert connectInfo.getAlgo().equals((decodeConnectInfo.getAlgo()));
        assert Arrays.equals(connectInfo.getPublicKey(), decodeConnectInfo.getPublicKey());

        assert connectInfo.toString().equals(decodeConnectInfo.toString());
    }

    @Test
    public void tcpConnectInfoTest() {
        byte[] publicKey = new byte[] { 1, 2, 3, 4, 0, (byte)0xff };

        TcpConnectInfo connectInfo = TcpConnectInfo.builder()
                .channelType(TcpConnectInfo.CHANNEL_TYPE_TCP4)
                .algo("x25519")
                .address("1.2.3.4")
                .port(1234)
                .publicKey(publicKey)
                .build();

        SipcBsonHelper bsonHelper = SipcBsonHelper.getInstance();

        byte[] encoded = bsonHelper.encode(connectInfo);

        System.out.println("encoded : " + Base64.getEncoder().encodeToString(encoded));

        assert "YwAAAAJhZGRyZXNzAAgAAAAxLjIuMy40AAJhbGdvAAcAAAB4MjU1MTkAAmNoYW5uZWxfdHlwZQAFAAAAdGNwNAAQcG9ydADSBAAABXB1YmxpY19rZXkABgAAAAABAgMEAP8A"
                .equals(Base64.getEncoder().encodeToString(encoded));

        ConnectInfo decodeConnectInfo = bsonHelper.decodeConnectInfo(encoded);

        assert (decodeConnectInfo instanceof TcpConnectInfo);

        assert connectInfo.getChannelType().equals((decodeConnectInfo.getChannelType()));
        assert connectInfo.getAlgo().equals((decodeConnectInfo.getAlgo()));
        assert Arrays.equals(connectInfo.getPublicKey(), decodeConnectInfo.getPublicKey());
        assert connectInfo.getAddress().equals(((TcpConnectInfo) decodeConnectInfo).getAddress());
        assert connectInfo.getPort() == ((TcpConnectInfo) decodeConnectInfo).getPort();

        assert connectInfo.toString().equals(decodeConnectInfo.toString());
    }
}
