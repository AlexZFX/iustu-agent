package com.iustu.vertxagent.dubbo;

import com.iustu.vertxagent.dubbo.model.CommonFuture;
import com.iustu.vertxagent.dubbo.model.CommonHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResp> {

    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResp response) {
        final Channel channel = ctx.channel();
        final long requestId = response.getRequestId();
        final CommonFuture rpcFuture = CommonHolder.getAndRemoveFuture(channel, requestId);
        if (rpcFuture == null) {
            logger.error("rpcFuture not found and RequestId = " + requestId);
            throw new IllegalStateException("rpcFuture not found");
        }
//        byte[] bytes = response.getBytes();
        rpcFuture.trySuccess(response.content().retain());
    }
}
