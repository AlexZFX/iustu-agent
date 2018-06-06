package com.iustu.vertxagent;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author : Alex
 * Date : 2018/6/6 9:58
 * Description :
 */
public class HttpServer {
    private static Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private final int serverPort = Integer.valueOf(System.getProperty("server.port"));

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
                            ch.pipeline().addLast("encoder", new HttpResponseEncoder())
                                    .addLast("decoder", new HttpRequestDecoder())
                                    .addLast(new HttpObjectAggregator(65535))
                                    .addLast("handler", new HttpServerInBoundHandler());
                        }
                    })
//                    .option(ChannelOption.SO_BACKLOG, 128)
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
