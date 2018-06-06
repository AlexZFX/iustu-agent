package com.iustu.vertxagent;

import com.iustu.vertxagent.register.EtcdRegistry;
import com.iustu.vertxagent.register.IRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * Author : Alex
 * Date : 2018/6/6 9:58
 * Description :
 */
public class HttpServer {
    private static Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private final int serverPort = Integer.valueOf(System.getProperty("server.port"));

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    public void start() throws InterruptedException {
        // TODO: 2018/6/6 配置线程数
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(4);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(eventLoopGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ChannelDuplexHandler() {
                                        @Override
                                        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
                                            super.close(ctx, promise);
                                            SocketAddress remoteAddr = ctx.channel().remoteAddress();
                                            logger.warn("----- channel close " + remoteAddr);
                                        }

                                        @Override
                                        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                            super.write(ctx, msg, promise);

                                            if (msg instanceof byte[]) {
                                                logger.warn("----- channel write " + new String((byte[]) msg));
                                            }
                                        }
                                    })
                                    .addLast("encoder", new HttpResponseEncoder())
                                    .addLast("decoder", new HttpRequestDecoder())
                                    .addLast(new HttpObjectAggregator(65535))
                                    .addLast("handler", new HttpServerInBoundHandler(registry));
                        }
                    })
//                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
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

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        try {
            server.start();
        } catch (InterruptedException e) {
            logger.error("server start error");
            e.printStackTrace();
        }
    }

}
