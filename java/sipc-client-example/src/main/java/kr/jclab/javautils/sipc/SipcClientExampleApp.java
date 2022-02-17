package kr.jclab.javautils.sipc;

import com.google.protobuf.Any;
import kr.jclab.javautils.sipc.crypto.CryptoException;
import kr.jclab.javautils.sipc.event.CalleeRequestContext;

import java.io.IOException;

public class SipcClientExampleApp {
    public static void main(String[] args) throws IOException, CryptoException {
        SipcClient sipcClient = SipcClient.connect(System.getenv("SIPC_CONNECT_INFO"));
        sipcClient.onHandshaked(() -> {
            System.out.println("sipc client: onHandshaked");
        });
        sipcClient.onRequest(
                "test01",
                Any.getDefaultInstance(),
                (CalleeRequestContext<Any> requestContext) -> {
                    System.err.println("sipc client: test01: recv = " + requestContext.getRequest());
                    requestContext.complete(Any.newBuilder()
                                    .setTypeUrl("HELLO WORLD")
                            .build());
                });
        sipcClient.onRequest(
                "shutdown",
                Any.getDefaultInstance(),
                (CalleeRequestContext<Any> requestContext) -> {
                    System.err.println("sipc client: shutdown" + requestContext.getRequest());
                    requestContext.complete(Any.newBuilder()
                            .setTypeUrl("HELLO WORLD")
                            .build());
                    sipcClient.close();
                });
        System.exit(sipcClient.run());
    }
}
