package kr.jclab.sipc.server.internal;

import io.netty.channel.unix.PeerCredentials;
import io.netty.util.AttributeKey;

public class ServerConstants {
    public static final AttributeKey<PeerCredentials> ATTR_PEER_CREDENTIALS = AttributeKey.newInstance("peerCredentials");
}
