#include <stdio.h>

#include <uvw/loop.h>

#include <jcu-sipc/log.h>
#include <jcu-sipc/client.h>
#include <jcu-sipc/transport/stdio.h>
#include <jcu-sipc/transport/tcp.h>

#include <sample1.pb.h>

using namespace kr::jclab::sipc::sample;

#include <future>

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

    ::kr::jclab::sipc::sample::SampleRequest request;
    request.set_message("HELLO I AM CLIENT");

    std::future<SampleResponse> future = std::move(ipc->request<SampleRequest, SampleProgress, SampleResponse>(
        "server_side_hello",
        request,
        [](const SampleProgress& progress) -> void {
          // DO NOT BLOCKING
          fprintf(stderr, "progress : %s\n", progress.message().c_str());
        },
        [](const SampleResponse& response) -> void {
          // DO NOT BLOCKING
          fprintf(stderr, "response : %s\n", response.message().c_str());
        }
    ));

    // DO NOT BLOCKING!


//    auto grpc_channel = client->getGrpcChannel();
//    auto sample_service = SampleService::NewStub(grpc_channel);
//
//    ::grpc::ClientContext client_context;
//    ::kr::jclab::sipc::sample::SampleRequest request;
//    ::kr::jclab::sipc::sample::SampleResponse response;
//    grpc::Status gstatus = sample_service->sampleCall(&client_context, request, &response);
//    fprintf(stderr, "response : %d\n", response.message().c_str());
//    fprintf(stderr, "res : %d : %s\n", gstatus.error_code(), gstatus.error_message().c_str());
  });

  if (!loop->run()) {
    return 1;
  }

  fprintf(stderr, "done\n");

  return 0;
}
