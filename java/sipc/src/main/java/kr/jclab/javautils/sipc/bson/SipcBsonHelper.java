package kr.jclab.javautils.sipc.bson;

import kr.jclab.javautils.sipc.bson.entity.ConnectInfo;
import kr.jclab.javautils.sipc.bson.entity.TcpConnectInfo;
import org.bson.*;
import org.bson.codecs.*;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.io.BasicOutputBuffer;

import java.nio.ByteBuffer;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

public class SipcBsonHelper {
    private final BsonDocumentCodec codec;

    public static class LazyHolder {
        public static SipcBsonHelper INSTANCE = new SipcBsonHelper();
    }

    public static SipcBsonHelper getInstance() {
        return LazyHolder.INSTANCE;
    }

    public SipcBsonHelper() {
        this.codec = new BsonDocumentCodec(
                fromProviders(
                        new BsonValueCodecProvider(),
                        new ValueCodecProvider(),
                        PojoCodecProvider.builder()
                                .conventions(Conventions.DEFAULT_CONVENTIONS)
                                .register(ConnectInfo.class)
                                .register(TcpConnectInfo.class)
                                .build()
                )
        );
    }

    public byte[] encode(Object entity) {
        BsonDocument document = BsonDocumentWrapper.asBsonDocument(entity, this.codec.getCodecRegistry());
        BasicOutputBuffer output = new BasicOutputBuffer();
        this.codec.encode(new BsonBinaryWriter(output), document, EncoderContext.builder().build());
        return output.toByteArray();
    }

    public ConnectInfo decodeConnectInfo(byte[] encoded) {
        BsonBinaryReader reader = new BsonBinaryReader(
                ByteBuffer.wrap(encoded)
        );

        BsonDocument document = this.codec.decode(reader, DecoderContext.builder().build());
        BsonString channelType = document.getString("channel_type");

        if (TcpConnectInfo.CHANNEL_TYPES.contains(channelType.getValue())) {
            return this.codec.getCodecRegistry().get(TcpConnectInfo.class).decode(document.asBsonReader(), DecoderContext.builder().build());
        }

        return this.codec.getCodecRegistry().get(ConnectInfo.class).decode(document.asBsonReader(), DecoderContext.builder().build());
    }
}
