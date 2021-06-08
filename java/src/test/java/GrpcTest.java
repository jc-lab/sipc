import fake.PeeredIpcChannel;
import io.grpc.stub.StreamObserver;
import kr.jclab.javautils.sipc.grpc.MiddleGrpcChannel;
import kr.jclab.sipc.sample.proto.Sample1Messages;
import kr.jclab.sipc.sample.proto.SampleServiceGrpc;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.*;

public class GrpcTest {
    final ExecutorService executor = Executors.newCachedThreadPool();
    final SampleServiceGrpc.SampleServiceImplBase serviceStub = new SampleServiceGrpc.SampleServiceImplBase() {
        @Override
        public void sampleCall(Sample1Messages.SampleRequest request, StreamObserver<Sample1Messages.SampleResponse> responseObserver) {
            System.out.println("SERVER: request: " + request);
            responseObserver.onNext(Sample1Messages.SampleResponse.newBuilder()
                    .setMessage("I AM SERVER, REQUESTED_USER=" + request.getUserId())
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void download(Sample1Messages.DownloadRequest request, StreamObserver<Sample1Messages.FilePart> responseObserver) {
            assert "CLIENT".equals(request.getUserId());
            responseObserver.onNext(
                    Sample1Messages.FilePart.newBuilder()
                            .setMessage("FIRST")
                            .build()
            );
            responseObserver.onNext(
                    Sample1Messages.FilePart.newBuilder()
                            .setMessage("SECOND")
                            .build()
            );
            responseObserver.onNext(
                    Sample1Messages.FilePart.newBuilder()
                            .setMessage(request.getUserId())
                            .build()
            );
            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<Sample1Messages.FilePart> upload(StreamObserver<Sample1Messages.UploadResponse> responseObserver) {
            return new StreamObserver<Sample1Messages.FilePart>() {
                int index = 0;
                volatile StringBuilder message = new StringBuilder();

                @Override
                public void onNext(Sample1Messages.FilePart value) {
                    System.out.println("UPLOAD: onNext : " + value.getMessage());

                    int currentIndex = index++;
                    message.append(value.getMessage());
                    if (currentIndex == 0) {
                        assert "FIRST".equals(value.getMessage());
                    } else if (currentIndex == 1) {
                        assert "SECOND".equals(value.getMessage());
                    }
                }

                @Override
                public void onError(Throwable t) {

                }

                @Override
                public void onCompleted() {
                    System.out.println("UPLOAD: onCompleted");

                    responseObserver.onNext(Sample1Messages.UploadResponse.newBuilder()
                            .setUserId("SERVER")
                            .setMessage(message.toString())
                            .build());
                    responseObserver.onCompleted();
                }
            };
        }
    };

    public MiddleGrpcChannel createGrpcChannelPair() {
        PeeredIpcChannel p1channel = new PeeredIpcChannel("p1", executor);
        PeeredIpcChannel p2channel = new PeeredIpcChannel("p2", executor);
        p1channel.setRemote(p2channel);
        p2channel.setRemote(p1channel);
        MiddleGrpcChannel serverChannel = new MiddleGrpcChannel(executor, p1channel);
        MiddleGrpcChannel clientChannel = new MiddleGrpcChannel(executor, p2channel);
        serverChannel.bindService(serviceStub);
        return clientChannel;
    }

    @Test
    public void sampleCallTest() throws Exception {
        MiddleGrpcChannel clientChannel = createGrpcChannelPair();
        SampleServiceGrpc.SampleServiceBlockingStub test = SampleServiceGrpc.newBlockingStub(clientChannel);
        Sample1Messages.SampleResponse response = test.sampleCall(
                Sample1Messages.SampleRequest.newBuilder()
                        .setMessage("HELLO_WORLD")
                        .setUserId("CLIENT")
                        .build()
        );
        assert "I AM SERVER, REQUESTED_USER=CLIENT".equals(response.getMessage());
    }

    @Test
    public void downloadCallTest() throws Exception {
        MiddleGrpcChannel clientChannel = createGrpcChannelPair();
        SampleServiceGrpc.SampleServiceBlockingStub test = SampleServiceGrpc.newBlockingStub(clientChannel);
        Iterator<Sample1Messages.FilePart> responseList = test.download(
                Sample1Messages.DownloadRequest.newBuilder()
                        .setUserId("CLIENT")
                        .build()
        );
        assert responseList.hasNext();
        assert "FIRST".equals(responseList.next().getMessage());
        assert responseList.hasNext();
        assert "SECOND".equals(responseList.next().getMessage());
        assert responseList.hasNext();
        assert "CLIENT".equals(responseList.next().getMessage());
    }

    @Test
    public void uploadCallTest() throws Exception {
        MiddleGrpcChannel clientChannel = createGrpcChannelPair();
        SampleServiceGrpc.SampleServiceStub test = SampleServiceGrpc.newStub(clientChannel);

        CompletableFuture<Sample1Messages.UploadResponse> completeFuture = new CompletableFuture<>();
        StreamObserver<Sample1Messages.UploadResponse> responseObserver = new StreamObserver<Sample1Messages.UploadResponse>() {
            @Override
            public void onNext(Sample1Messages.UploadResponse value) {
                completeFuture.complete(value);
            }

            @Override
            public void onError(Throwable t) {
                completeFuture.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                // ok
            }
        };
        StreamObserver<Sample1Messages.FilePart> requestObserver = test.upload(responseObserver);

        requestObserver.onNext(Sample1Messages.FilePart.newBuilder()
                .setMessage("FIRST")
                .build());
        requestObserver.onNext(Sample1Messages.FilePart.newBuilder()
                .setMessage("SECOND")
                .build());
        requestObserver.onCompleted();

        Sample1Messages.UploadResponse response = completeFuture.get();
        assert "SERVER".equals(response.getUserId());
        System.out.println("response.getMessage() : " + response.getMessage());
        assert "FIRSTSECOND".equals(response.getMessage());
    }
}
