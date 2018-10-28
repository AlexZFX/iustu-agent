package com.iustu.agent.dubbo;

import com.iustu.agent.dubbo.model.AgentResponseProto;
import com.iustu.agent.dubbo.model.CommonFuture;
import com.iustu.agent.dubbo.model.CommonHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author : Alex
 * Date : 2018/6/9 10:06
 * Description :
 */
public class AgentHandler extends SimpleChannelInboundHandler<AgentResponseProto.AgentResponse> {

    private static final Logger logger = LoggerFactory.getLogger(AgentHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AgentResponseProto.AgentResponse msg) {
//        final Channel channel = ctx.channel();
        final long requestId = msg.getId();
//        final CommonFuture agentFuture = CommonHolder.getAndRemoveFuture(channel, requestId);
        final CommonFuture agentFuture = CommonHolder.getAndRemoveFuture(requestId);
        if (agentFuture == null) {
            logger.error("agentFuture not found and RequestId = " + requestId);
            throw new IllegalArgumentException("agentFuture not found");
        }
        byte[] bytes = msg.getData().toByteArray();
        agentFuture.trySuccess(bytes);
    }

}
