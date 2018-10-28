package com.iustu.agent;

import com.iustu.agent.dubbo.RpcClient;
import com.iustu.agent.dubbo.model.AgentRequestProto;
import com.iustu.agent.register.EtcdRegistry;
import com.iustu.agent.register.IRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author : Alex
 * Date : 2018/6/7 19:05
 * Description :
 */
public class ProviderAgent {

    private static Logger logger = LoggerFactory.getLogger(ConsumerAgent.class);

    private final int serverPort = Integer.valueOf(System.getProperty("server.port"));

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private RpcClient rpcClient;

    public void start() throws InterruptedException {
        // TODO: 2018/6/6 配置线程数
//        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
//        EventLoopGroup workerGroup = new NioEventLoopGroup(16);
//        EventLoopGroup providerWorkerGroup = new NioEventLoopGroup(8);
//        ((NioEventLoopGroup) workerGroup).setIoRatio(70);
        EventLoopGroup eventLoopGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(16) : new NioEventLoopGroup(16);
//        EventLoopGroup providerWorkerGroup = new EpollEventLoopGroup(8);
//        ((EpollEventLoopGroup) workerGroup).setIoRatio(70);
        rpcClient = new RpcClient(workerGroup);
//        EventExecutorGroup executors = new DefaultEventExecutorGroup(8);
        ProviderInBoundHandler providerInBoundHandler = new ProviderInBoundHandler(registry, rpcClient);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(eventLoopGroup, workerGroup)
//                    .channel(NioServerSocketChannel.class)
                    .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
//                                    .addLast(new ChannelDuplexHandler() {
//                                        @Override
//                                        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
//                                            super.close(ctx, promise);
//                                            SocketAddress remoteAddr = ctx.channel().remoteAddress();
//                                            logger.warn("----- channel close " + remoteAddr);
//                                        }
//
//                                        @Override
//                                        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
//                                            super.write(ctx, msg, promise);
//
//                                            if (msg instanceof byte[]) {
//                                                logger.warn("----- channel write " + new String((byte[]) msg));
//                                            }
//                                        }
//                                    })
                                    .addLast(new ProtobufVarint32FrameDecoder())
                                    .addLast(new ProtobufDecoder(AgentRequestProto.AgentRequest.getDefaultInstance()))
                                    .addLast(new ProtobufVarint32LengthFieldPrepender())
                                    .addLast(new ProtobufEncoder())
                                    .addLast(providerInBoundHandler);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
//                    .option(EpollChannelOption.TCP_CORK, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
            ;
            logger.info("server start:" + serverPort);
            //绑定端口，开始接受进来的连接
            ChannelFuture channelFuture = bootstrap.bind(serverPort).sync();
            //等待服务器socket关闭
            //在本例子中不会发生,这时可以关闭服务器了
            channelFuture.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            eventLoopGroup.shutdownGracefully();
            logger.info("server stop");
        }

    }
}
