package kr.jclab.javautils.sipc.bson.entity;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@lombok.Getter
@lombok.ToString
public class TcpConnectInfo extends ConnectInfo {
    public static final String CHANNEL_TYPE_TCP4 = "tcp4";
    public static final String CHANNEL_TYPE_TCP6 = "tcp6";
    public static final Set<String> CHANNEL_TYPES = Collections.unmodifiableSet(new HashSet<String>() {{
        add(CHANNEL_TYPE_TCP4);
        add(CHANNEL_TYPE_TCP6);
    }});

    @BsonProperty(value = "address")
    private final String address;

    @BsonProperty(value = "port")
    private final int port;

    @BsonCreator
    @lombok.Builder(builderClassName = "Builder", builderMethodName = "builder")
    public TcpConnectInfo(
            @BsonProperty(value = "channel_id") String channelId,
            @BsonProperty(value = "channel_type") String channelType,
            @BsonProperty(value = "algo") String algo,
            @BsonProperty(value = "public_key") byte[] publicKey,
            @BsonProperty(value = "address") String address,
            @BsonProperty(value = "port") int port
    ) {
        super(channelId, channelType, algo, publicKey);
        this.address = address;
        this.port = port;
    }
}
