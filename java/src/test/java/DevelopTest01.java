import kr.jclab.javautils.sipc.ProcessSipcHost;
import kr.jclab.javautils.sipc.ProcessStdioSipcHost;
import kr.jclab.javautils.sipc.channel.tcp.TcpChannelHost;
import kr.jclab.javautils.sipc.event.CalleeRequestContext;
import kr.jclab.javautils.sipc.event.EventChannel;
import kr.jclab.sipc.sample.proto.Sample1Messages;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DevelopTest01 {
    final ExecutorService executor = Executors.newCachedThreadPool();
//    final SampleServiceGrpc.SampleServiceImplBase serviceStub = new SampleServiceGrpc.SampleServiceImplBase() {
//        @Override
//        public void sampleCall(Sample1Messages.SampleRequest request, StreamObserver<Sample1Messages.SampleResponse> responseObserver) {
//            System.out.println("SERVER: request: " + request);
//            responseObserver.onNext(Sample1Messages.SampleResponse.newBuilder()
//                    .setMessage("I AM SERVER, REQUESTED_USER=" + request.getUserId())
//                    .build());
//            responseObserver.onCompleted();
//        }
//
//        @Override
//        public void download(Sample1Messages.DownloadRequest request, StreamObserver<Sample1Messages.FilePart> responseObserver) {
//            assert "CLIENT".equals(request.getUserId());
//            responseObserver.onNext(
//                    Sample1Messages.FilePart.newBuilder()
//                            .setMessage("FIRST")
//                            .build()
//            );
//            responseObserver.onNext(
//                    Sample1Messages.FilePart.newBuilder()
//                            .setMessage("SECOND")
//                            .build()
//            );
//            responseObserver.onNext(
//                    Sample1Messages.FilePart.newBuilder()
//                            .setMessage(request.getUserId())
//                            .build()
//            );
//            responseObserver.onCompleted();
//        }
//
//        @Override
//        public StreamObserver<Sample1Messages.FilePart> upload(StreamObserver<Sample1Messages.UploadResponse> responseObserver) {
//            return new StreamObserver<Sample1Messages.FilePart>() {
//                int index = 0;
//                volatile StringBuilder message = new StringBuilder();
//
//                @Override
//                public void onNext(Sample1Messages.FilePart value) {
//                    System.out.println("UPLOAD: onNext : " + value.getMessage());
//
//                    int currentIndex = index++;
//                    message.append(value.getMessage());
//                    if (currentIndex == 0) {
//                        assert "FIRST".equals(value.getMessage());
//                    } else if (currentIndex == 1) {
//                        assert "SECOND".equals(value.getMessage());
//                    }
//                }
//
//                @Override
//                public void onError(Throwable t) {
//
//                }
//
//                @Override
//                public void onCompleted() {
//                    System.out.println("UPLOAD: onCompleted");
//
//                    responseObserver.onNext(Sample1Messages.UploadResponse.newBuilder()
//                            .setUserId("SERVER")
//                            .setMessage(message.toString())
//                            .build());
//                    responseObserver.onCompleted();
//                }
//            };
//        }
//    };
//
//    public static void main(String[] args) throws Exception {
//        new DevelopTest01().run(args);
//    }
//
//    public void run(String[] args) throws Exception {
//        // Common instances
//        TcpChannelHost tcpChannelHost = TcpChannelHost.create();
//
//        // STDIO
//        ProcessSipcHost stdioHost = ProcessSipcHost.builder(tcpChannelHost)
//                .executor(executor)
//                .build();
//        stdioHost.bindService(serviceStub);
//
//        System.out.println("stdioHost.getConnectInfo() = " + stdioHost.getConnectInfo());
//
//        stdioHost.attachProcess(
//                Runtime.getRuntime().exec(
//                        new String[] {
//                                "D:\\temp\\sipc-test\\t1.exe",
//                                "--connect-info=" + stdioHost.getConnectInfo()
//                        }
//                )
//        );
//
//        Future<Void> handshakeDone = stdioHost.handshake();
//        handshakeDone.get();
//        System.out.println("HANDSHAKED");
//
//        SampleServiceGrpc.SampleServiceBlockingStub clientSideService = SampleServiceGrpc.newBlockingStub(stdioHost.getGrpcChannel());
//        Sample1Messages.SampleResponse response = clientSideService.sampleCall(
//                Sample1Messages.SampleRequest.newBuilder()
//                        .setUserId("USER_ID")
//                        .setMessage("MESSAGE")
//                        .build()
//        );
//        System.out.println("response = " + response);
//
//        Future<Void> done = stdioHost.waitForDone();
//        done.get();
//        System.out.println("ALL DONE");
//
//    }

    public static void main(String[] args) throws Throwable {
        try {
            new DevelopTest01().run(args);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void run(String[] args) throws Exception {
        // Common instances
        TcpChannelHost tcpChannelHost = TcpChannelHost.create();

        // STDIO
//        ProcessStdioSipcHost stdioHost = ProcessStdioSipcHost.builder()
//                .executor(executor)
//                .build();

        // TCP
        ProcessSipcHost stdioHost = ProcessSipcHost.builder(tcpChannelHost)
                .executor(executor)
                .build();

        System.out.println("stdioHost.getConnectInfo() = " + stdioHost.getConnectInfo());

        stdioHost.attachProcess(
                Runtime.getRuntime().exec(
                        new String[] {
                                "D:\\jcworkspace\\sipc\\cpp\\cmake-build-debug\\sample\\jcu_sipc_test_sample-app.exe",
                        },
                        new String[] {
                                "SIPC_CONNECT_INFO=" + stdioHost.getConnectInfo()
                        }
                )
        );

        EventChannel eventChannel = stdioHost.getEventChannel();

        eventChannel.onRequest(
                "server_side_hello",
                Sample1Messages.SampleRequest.getDefaultInstance(),
                (CalleeRequestContext<Sample1Messages.SampleRequest> requestContext) -> {
                    System.out.println("SampleRequest : " + requestContext.getRequest());
                    requestContext.progress(Sample1Messages.SampleProgress.newBuilder().setMessage("FIRST").build());
                    requestContext.progress(Sample1Messages.SampleProgress.newBuilder().setMessage("SECOND").build());
                    requestContext.complete(Sample1Messages.SampleResponse.newBuilder().setMessage("OK").build());

                    do {
                        Sample1Messages.SampleRequest request = Sample1Messages.SampleRequest.newBuilder()
                                .setMessage("Hi, Client! I am a Server")
                                .build();
                        try {
                            Future<Sample1Messages.SampleResponse> responseFuture = eventChannel.request(
                                    "client_side_hello",
                                    request,
                                    Sample1Messages.SampleResponse.getDefaultInstance(),
                                    Sample1Messages.SampleProgress.getDefaultInstance(),
                                    (progress) -> {
                                        System.out.println("PROGRESS : " + progress.getMessage());
                                    }
                            );
                            System.out.println("RESPONSE : " + responseFuture.get());
                        } catch (InterruptedException | ExecutionException | IOException e) {
                            e.printStackTrace();
                            System.out.println("RESPONSE - FAILED : " + e.getMessage());
                        }
                    } while(false);
                }
        );

        Future<Void> handshakeDone = stdioHost.handshake();
        handshakeDone.get();
        System.out.println("HANDSHAKED");

//        SampleServiceGrpc.SampleServiceBlockingStub clientSideService = SampleServiceGrpc.newBlockingStub(stdioHost.getGrpcChannel());
//        Sample1Messages.SampleResponse response = clientSideService.sampleCall(
//                Sample1Messages.SampleRequest.newBuilder()
//                        .setUserId("USER_ID")
//                        .setMessage("MESSAGE")
//                        .build()
//        );
//        System.out.println("response = " + response);

        Future<Void> done = stdioHost.waitForDone();
        done.get();
        System.out.println("ALL DONE");

    }
}
