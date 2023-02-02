package kr.jclab.sipc.server.internal;

import io.netty.channel.Channel;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.SipcServer;

import java.security.NoSuchAlgorithmException;

public class UnixDomainSocketSipcServer extends SipcServer {
    private final Channel domainChannel;

    public UnixDomainSocketSipcServer(Channel domainChannel, SipcServerContext serverContext) throws NoSuchAlgorithmException {
        super(serverContext);
        this.domainChannel = domainChannel;
    }
}
