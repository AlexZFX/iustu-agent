package com.iustu.vertxagent.dubbo;

import com.iustu.vertxagent.dubbo.model.AgentResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Author : Alex
 * Date : 2018/6/9 10:06
 * Description :
 */
public class AgentHandler extends SimpleChannelInboundHandler<AgentResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AgentResponse msg) throws Exception {

    }
}
