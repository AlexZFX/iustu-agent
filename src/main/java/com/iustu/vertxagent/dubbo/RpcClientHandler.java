package com.iustu.vertxagent.dubbo;

import com.iustu.vertxagent.dubbo.model.CommonFuture;
import com.iustu.vertxagent.dubbo.model.CommonHolder;
import com.iustu.vertxagent.dubbo.model.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response) {
        final Channel channel = channelHandlerContext.channel();
        final long requestId = response.getRequestId();
        final CommonFuture rpcFuture = CommonHolder.getAndRemoveRpcFuture(channel, requestId);
        if (rpcFuture == null) {
            throw new IllegalStateException("CommonFuture not found");
        }
        byte[] bytes = response.getBytes();
        rpcFuture.trySuccess(bytes);
    }
}
