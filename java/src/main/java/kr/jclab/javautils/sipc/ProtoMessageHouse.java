package kr.jclab.javautils.sipc;

import com.google.protobuf.GeneratedMessageV3;

public class ProtoMessageHouse {
    public static String getTypeUrl(GeneratedMessageV3 messageV3) {
        return "pb-type.jc-lab.net/sipc/" + messageV3.getDescriptorForType().getFullName();
    }
}
