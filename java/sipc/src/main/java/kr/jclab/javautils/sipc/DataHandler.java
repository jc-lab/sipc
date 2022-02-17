package kr.jclab.javautils.sipc;

@FunctionalInterface
public interface DataHandler {
    void handle(byte[] data);
}
