package kr.jclab.javautils.sipc.bson.entity;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

@lombok.Getter
@lombok.ToString
public class ConnectInfo {
    @BsonProperty(value = "channel_id")
    private final String channelId;

    @BsonProperty(value = "channel_type")
    private final String channelType;

    @BsonProperty(value = "algo")
    private final String algo;

    @BsonProperty(value = "public_key")
    private final byte[] publicKey;

    @lombok.Builder(builderClassName = "Builder", builderMethodName = "basicBuilder")
    @BsonCreator
    public ConnectInfo(
            @BsonProperty(value = "channel_id") String channelId,
            @BsonProperty(value = "channel_type") String channelType,
            @BsonProperty(value = "algo") String algo,
            @BsonProperty(value = "public_key") byte[] publicKey
    ) {
        this.channelId = channelId;
        this.channelType = channelType;
        this.algo = algo;
        this.publicKey = publicKey;
    }
}
