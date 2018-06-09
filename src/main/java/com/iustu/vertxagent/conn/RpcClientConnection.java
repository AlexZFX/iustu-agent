package com.iustu.vertxagent.conn;

import com.iustu.vertxagent.dubbo.RpcClientInitializer;
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
public class RpcClientConnection extends Connection {

//    private ChannelFuture channelFuture;

//    private Channel channel; // assign after connection connected successfully


    public RpcClientConnection(
            NioEventLoopGroup nioEventLoopGroup,
            OnConnectionListener connectionListener, String host, int port) {
        super(nioEventLoopGroup, connectionListener, host, port);
    }

    @Override
    public ChannelFuture connectChannel() {
        if (channelFuture == null) {
            channelFuture = new Bootstrap()
                    .group(nioEventLoopGroup)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer())
                    .connect(host, port).addListener((ChannelFutureListener) future -> {
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
        return channelFuture;
    }

    @Override
    public Channel getChannel() {
        if (channel != null) {
            return channel;
        } else {
            channelFuture = connectChannel();
            return channel;
        }
    }


}
