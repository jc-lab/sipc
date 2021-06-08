package kr.jclab.javautils.sipc.grpc;

import kr.jclab.javautils.sipc.channel.IpcChannel;

import java.util.concurrent.Executor;

public class MiddleGrpcChannel extends AbstractMiddleGrpcChannel {
    public MiddleGrpcChannel(Executor executor, IpcChannel transport) {
        super(executor, transport);
    }
}
