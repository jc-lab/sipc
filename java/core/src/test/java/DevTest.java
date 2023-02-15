import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import kr.jclab.sipc.client.SipcClient;
import kr.jclab.sipc.internal.PidAccessor;
import kr.jclab.sipc.server.SipcChild;
import kr.jclab.sipc.server.SipcServer;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

@Slf4j
public class DevTest {
    public static void main(String[] args) throws Exception {
//        ProcessBuilder processBuilder = new ProcessBuilder()
//                .command("/bin/true");
//        Process process = processBuilder.start();
//
//        long pid = PidAccessor.getPid(process);
//        log.warn("pid : " + pid);

        SipcServer server = SipcServer.builder()
                .handshakeTimeoutMilliseconds(2500)
                .build();

        ChannelDuplexHandler childHandler = new ChannelDuplexHandler() {
            @Override
            public boolean isSharable() {
                return true;
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                log.warn("SERVER: channelRead: " + ctx + " : " + ((ByteBuf)msg).toString(StandardCharsets.UTF_8));
            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                log.warn("SERVER: channelActive: " + ctx);
//                ctx.writeAndFlush(Unpooled.wrappedBuffer("HELLO WORLD".getBytes()));
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                log.warn("SERVER: channelInactive: " + ctx);
            }
        };
        SipcChild child = server.prepareProcess(childHandler);


        if (true) {
            System.out.println("CHILD: " + child.getEncodedConnectInfo());

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("enter pid: ");
            int pid = Integer.parseInt(bufferedReader.readLine().trim());
            child.attachProcess(pid);

            serverWorker(child);
        } else {
            // CONNECT
            long pid = TestUtil.getPid();
            child.attachProcess((int) pid);
//        child.attachProcess(123);
            SipcClient sipcClient = SipcClient.builder()
                    .connectInfoText(child.getEncodedConnectInfo())
                    .handler(new ChannelDuplexHandler() {
                        @Override
                        public boolean isSharable() {
                            return true;
                        }

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            log.warn("CLIENT: channelRead: " + ctx + " : " + ((ByteBuf) msg).toString(StandardCharsets.UTF_8));
                        }

                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            log.warn("CLIENT: channelActive: " + ctx);
//                        ctx.writeAndFlush(Unpooled.wrappedBuffer("HELLO WORLD".getBytes()));
                        }

                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                            log.warn("CLIENT: channelInactive: " + ctx);
                        }
                    })
                    .build();

            // SERVER
            Thread sThread = new Thread(() -> {
                serverWorker(child);
            });

            // CLIENT
            Thread cThread = new Thread(() -> {
                try {
                    log.warn("CLIENT THREAD START");
                    sipcClient.handshakeFuture().get();
                    log.warn("CLIENT THREAD EXIT");
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("CLIENT THREAD EXIT WITH EXCEPTION", e);
                }
            });

            sThread.start();
            cThread.start();
            sThread.join();
            cThread.join();

//
//        for(int i=0; i<5; i++) {
//            try {
//                Thread.sleep(1000);
//            } catch (Exception e) {
//                break;
//            }
//        }

            sipcClient.shutdown();
        }
        server.shutdown();
    }

    public static void serverWorker (SipcChild child) {
        try {
            log.warn("SERVER THREAD START");
            child.handshakeFuture().get();
            for (int i = 0; i < 10; i++) {
                child.writeAndFlush(Unpooled.wrappedBuffer("HELLO WORLD".getBytes()));
                Thread.sleep(1000);
            }
            log.warn("SERVER THREAD EXIT");
        } catch (InterruptedException | ExecutionException e) {
            log.warn("SERVER THREAD EXIT WITH EXCEPTION", e);
        }
    }
}
