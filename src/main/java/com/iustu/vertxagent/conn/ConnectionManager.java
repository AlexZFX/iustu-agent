package com.iustu.vertxagent.conn;

import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionManager implements RpcClientConnection.OnConnectionListener {


    private final NioEventLoopGroup eventLoopGroup;

    private final int nConns;

    private AtomicInteger connIndex = new AtomicInteger(0);

    private final Object lock = new Object();
    private final List<Connection> conns = new ArrayList<>();
    private Connection pConn;

    private final String host;
    private final int port;
    private final String type;

    // TODO: 2018/6/9 线程数，连接池大小设置
    public ConnectionManager(String host, int port, String type) {
        this(host, port, type, 4);
    }

    public ConnectionManager(String host, int port, String type, int eventLoopGroupSize) {
        this(host, port, type, eventLoopGroupSize, 4);
    }

    public ConnectionManager(String host, int port, String type, int eventLoopGroupSize, int connSize) {
        this.host = host;
        this.port = port;
        this.type = type;
        this.eventLoopGroup = new NioEventLoopGroup(eventLoopGroupSize);
        this.nConns = connSize;

    }

    /**
     * 所有方法都不线程安全，全部加了锁
     *
     * @return
     */
    public ChannelFuture getChannelFuture() {
        try {
            if (conns.size() == nConns) {
                return conns.get(connIndex.getAndIncrement() % nConns).connectChannel();
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        synchronized (lock) {
            final int size = conns.size();
            if (size == 0) {
                if (pConn == null) {
                    initPConn();
                }
                return pConn.connectChannel();
            } else {
                if (size < nConns && pConn == null) {
                    initPConn();
                    pConn.connectChannel();
                }

                return conns.get(connIndex.getAndIncrement() % size).connectChannel();
            }
        }
    }

    private void initPConn() {
        if ("consumer".equals(type)) {
            pConn = new AgentConnection(eventLoopGroup, this, host, port);
        } else if ("provider".equals(type)) {
            pConn = new RpcClientConnection(eventLoopGroup, this, host, port);
        } else {
            throw new IllegalArgumentException("type is fault");
        }
    }

    @Override
    public void onConnectionConnected(Connection conn) {
        synchronized (lock) {
            if (pConn == conn) {
                pConn = null;
            }

            conns.add(conn);
        }
    }

    @Override
    public void onConnectionConnectFailed(Connection conn) {
        synchronized (lock) {
            if (pConn == conn) {
                pConn = null;
            }
        }
    }

    @Override
    public void onConnectionClosed(Connection conn) {
        synchronized (lock) {
            conns.remove(conn);
        }
    }
}
