package com.iustu.agent.conn;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;

/**
 * Author : Alex
 * Date : 2018/6/8 20:11
 * Description :
 */
public abstract class Connection {

    protected final EventLoopGroup eventLoopGroup;

    protected final RpcClientConnection.OnConnectionListener connectionListener;

    protected ChannelFuture channelFuture;

    protected Channel channel; // assign after connection connected successfully

    protected final String host;

    protected final int port;

    public Connection(
            EventLoopGroup eventLoopGroup,
            OnConnectionListener connectionListener,
            String host,
            int port) {
        this.eventLoopGroup = eventLoopGroup;
        this.connectionListener = connectionListener;
        this.host = host;
        this.port = port;
    }

    public abstract ChannelFuture connectChannel();

    public abstract Channel getChannel();

    public interface OnConnectionListener {

        void onConnectionConnected(Connection conn);

        void onConnectionConnectFailed(Connection conn);

        void onConnectionClosed(Connection conn);

    }
}
