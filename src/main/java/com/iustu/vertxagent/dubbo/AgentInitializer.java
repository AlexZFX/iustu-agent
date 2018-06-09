package com.iustu.vertxagent.dubbo;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

/**
 * Author : Alex
 * Date : 2018/6/8 21:03
 * Description :
 */
public class AgentInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpClientCodec())
                .addLast(new HttpObjectAggregator(4096))
                .addLast(new AgentHandler());
    }
}
