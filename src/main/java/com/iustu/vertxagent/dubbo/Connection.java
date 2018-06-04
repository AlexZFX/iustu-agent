package com.iustu.vertxagent.dubbo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Author : Alex
 * Date : 2018/6/4 13:42
 * Description :
 */
public class Connection {

    private final NioEventLoopGroup nioEventLoopGroup;

    private final OnConnectionListener connectionListener;

    private ChannelFuture connectChannelFuture;

    private Channel channel; // assign after connection connected successfully

    public Connection(
            NioEventLoopGroup nioEventLoopGroup,
            OnConnectionListener connectionListener) {
        this.nioEventLoopGroup = nioEventLoopGroup;
        this.connectionListener = connectionListener;
    }

    public ChannelFuture connectChannel() {
        if (connectChannelFuture == null) {
            int port = Integer.valueOf(System.getProperty("dubbo.protocol.port"));
            connectChannelFuture = new Bootstrap()
                    .group(nioEventLoopGroup)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer())
                    .connect("127.0.0.1", port).addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            channel = future.channel();
                            connectionListener.onConnectionConnected(this);
                            future.channel().closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                                connectionListener.onConnectionClosed(this);
                            });
                        } else {
                            connectionListener.onConnectionConnectFailed(this);
                        }
                    });
        }

        return connectChannelFuture;
    }

    public interface OnConnectionListener {

        void onConnectionConnected(Connection conn);

        void onConnectionConnectFailed(Connection conn);

        void onConnectionClosed(Connection conn);
    }

}
