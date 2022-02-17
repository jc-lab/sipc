package kr.jclab.javautils.sipc.handler;

@FunctionalInterface
public interface ChannelHandler {
    void accept(String instanceId);
}
