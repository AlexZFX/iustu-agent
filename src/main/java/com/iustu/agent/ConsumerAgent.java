package com.iustu.agent;

import com.iustu.agent.conn.ConnectionManager;
import com.iustu.agent.dubbo.AgentClient;
import com.iustu.agent.register.Endpoint;
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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author : Alex
 * Date : 2018/6/6 9:58
 * Description :
 */
public class ConsumerAgent {
    private static Logger logger = LoggerFactory.getLogger(ConsumerAgent.class);

    private final int serverPort = Integer.valueOf(System.getProperty("server.port"));

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private static final String type = System.getProperty("type");

    private List<Endpoint> endpoints = null;

//    private Map<String, AgentClient> agentClientMap = new HashMap<>();

    private List<AgentClient> agentClientList = new ArrayList<>();

    private final Object lock = new Object();

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    public void start() throws Exception {
        // TODO: 2018/6/6 配置线程数
        EventLoopGroup eventLoopGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(16) : new NioEventLoopGroup(16);
//        EventLoopGroup consumerWorkerGroup = new EpollEventLoopGroup(8);
//        ((EpollEventLoopGroup) workerGroup).setIoRatio(70);
//        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
//        EventLoopGroup workerGroup = new NioEventLoopGroup(16);
//        ((NioEventLoopGroup) workerGroup).setIoRatio(70);
//
        if (null == endpoints) {
            synchronized (lock) {
                if (null == endpoints) {
                    endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                }
                int count;
                Set<Endpoint> endpointSet = new HashSet<>(endpoints);
                for (Endpoint endpoint : endpointSet) {
                    count = Collections.frequency(endpoints, endpoint);
                    ConnectionManager connectionManager = new ConnectionManager(endpoint.getHost(), endpoint.getPort(), type, workerGroup, 4 * count);
                    AgentClient client = new AgentClient(connectionManager);
                    agentClientList.add(client);
//                    agentClientMap.put(endpoint.getHost() + endpoint.getPort(), client);
                }
            }
        }
        int size = agentClientList.size();
//       EventExecutorGroup executors = new DefaultEventExecutorGroup(8);
//        ConsumerInBoundHandler consumerInBoundHandler = new ConsumerInBoundHandler(endpoints, agentClientMap, workerGroup);
//        ConsumerInBoundHandler consumerInBoundHandler = new ConsumerInBoundHandler(agentClientList.get(atomicInteger.getAndIncrement() % size));
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(eventLoopGroup, workerGroup)
//                    .channel(NioServerSocketChannel.class)
                    .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
//                            logger.info("server accept channel: " + ch.id());

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
//                                    }
                                    .addLast("encoder", new HttpResponseEncoder())
                                    .addLast("decoder", new HttpRequestDecoder(1024, 1024, 1024, false))
                                    .addLast(new HttpObjectAggregator(2048))
                                    .addLast("handler", new ConsumerInBoundHandler(agentClientList.get(atomicInteger.getAndIncrement() % size)));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
//                    .option(EpollChannelOption.TCP_CORK, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT) //使用对象池，加上后感觉跑分降低了
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
//                    .childOption(Epoll.isAvailable() ? EpollChannelOption.TCP_CORK : ChannelOption.TCP_NODELAY, true)
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
