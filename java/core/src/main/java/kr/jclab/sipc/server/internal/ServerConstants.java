package kr.jclab.sipc.server.internal;

import io.netty.util.AttributeKey;

public class ServerConstants {
    public static final AttributeKey<Integer> ATTR_PEER_PID = AttributeKey.newInstance("peerPid");
}
