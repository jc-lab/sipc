

# Java Host

* [java/src/test/java/DevelopTest01.java](../java/src/test/java/DevelopTest01.java)

```Java
    public void run(String[] args) throws Exception {
        // Common instances
        TcpChannelHost tcpChannelHost = TcpChannelHost.create();

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

        // Register a request handler. This can be called by the Client.
        eventChannel.onRequest(
                "server_side_hello",
                Sample1Messages.SampleRequest.getDefaultInstance(),
                (CalleeRequestContext<Sample1Messages.SampleRequest> requestContext) -> {
                    System.out.println("SampleRequest : " + requestContext.getRequest());
                    requestContext.progress(Sample1Messages.SampleProgress.newBuilder().setMessage("FIRST").build());
                    requestContext.progress(Sample1Messages.SampleProgress.newBuilder().setMessage("SECOND").build());
                    requestContext.complete(Sample1Messages.SampleResponse.newBuilder().setMessage("OK").build());
                }
        );

        Future<Void> handshakeDone = stdioHost.handshake();
        handshakeDone.get();
        System.out.println("HANDSHAKED");

        // Call the method registered on the client side.
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
                    });
                System.out.println("RESPONSE : " + responseFuture.get());
            } catch (InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
                System.out.println("RESPONSE - FAILED : " + e.getMessage());
            }
        } while(false);

        Future<Void> done = stdioHost.waitForDone();
        done.get();
        System.out.println("ALL DONE");

        tcpChannelHost.close();
    }
```

# CPP Client

* [cpp/sample/sample.cc](../cpp/sample/sample.cc)

```C++
int main() {
  auto logger = jcu::sipc::createDefaultLogger([](const std::string& line) -> void {
    fprintf(stderr, "%s\n", line.c_str());
  });
  auto loop = uvw::Loop::getDefault();
  auto ipc = jcu::sipc::Client::create(loop, logger);
  ipc->registerTransport(jcu::sipc::transport::Stdio::create(loop));
  ipc->registerTransport(jcu::sipc::transport::Tcp::create(loop));
  ipc->connect(
      getenv("SIPC_CONNECT_INFO")
  );

  // Register a request handler. This can be called by the host.
  ipc->onRequest<SampleRequest>("client_side_hello", [ipc](std::shared_ptr<jcu::sipc::CalleeRequestContext<SampleRequest>> ctx) -> void {
    // Run in worker thread

    auto req = ctx->getRequest();
    fprintf(stderr, "client_side_hello : %s\n", req.message().c_str());

    SampleProgress progress;

    progress.set_message("FIRST");
    ctx->progress(&progress);

    progress.set_message("SECOND");
    ctx->progress(&progress);

    SampleResponse response;
    response.set_message("OK");
    ctx->complete(&response);

    // Close after all queued writes
    ipc->close();
  });

  ipc->onHandshake([&ipc](jcu::sipc::Client *client) -> void {
    // Run in Event Process Thread (libuv)
    // DO NOT BLOCKING

    fprintf(stderr, "CONNECTED!!!\n");

    // Just Call Request Sample
    
    ::kr::jclab::sipc::sample::SampleRequest request;
    request.set_message("HELLO I AM CLIENT");

    // Can call the host's method.
    std::future<SampleResponse> future = std::move(ipc->request<SampleRequest, SampleProgress, SampleResponse>(
        "server_side_hello",
        request,
        [](const SampleProgress& progress) -> void {
          // Run in Event Process Thread (libuv)
          // DO NOT BLOCKING
          fprintf(stderr, "progress : %s\n", progress.message().c_str());
        },
        [](const SampleResponse& response) -> void {
          // Run in Event Process Thread (libuv)
          // DO NOT BLOCKING
          fprintf(stderr, "response : %s\n", response.message().c_str());
        }
    ));
  });

  if (!loop->run()) {
    return 1;
  }

  fprintf(stderr, "done\n");

  return 0;
}
```
