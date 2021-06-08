package kr.jclab.javautils.sipc.grpc;

import com.google.protobuf.ByteString;
import io.grpc.InternalMetadata;
import io.grpc.Metadata;
import kr.jclab.sipc.common.proto.Frames;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MetadataHelper {
    public static Metadata parse(Frames.GrpcMetadata headersMessage) {
        byte[][] headerData = headersMessage.getDataList().stream()
                .map(ByteString::toByteArray)
                .toArray(byte[][]::new);
        return InternalMetadata.newMetadata(headerData);
    }

    public static void serializeTo(Frames.GrpcMetadata.Builder builder, Metadata header) {
        byte[][] serializedHeaders = InternalMetadata.serialize(header);
        List<ByteString> serializedHeaderList = Arrays.stream(serializedHeaders)
                .map(ByteString::copyFrom)
                .collect(Collectors.toList());
        builder.addAllData(serializedHeaderList);
    }
}
