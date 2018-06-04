package com.iustu.vertxagent.dubbo;

import com.iustu.vertxagent.dubbo.model.RpcFuture;
import com.iustu.vertxagent.dubbo.model.RpcRequestHolder;
import com.iustu.vertxagent.dubbo.model.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response) {
        final Channel channel = channelHandlerContext.channel();
        final long requestId = response.getRequestId();
        final RpcFuture rpcFuture = RpcRequestHolder.getAndRemoveRpcFuture(channel, requestId);
        if (rpcFuture == null) {
            throw new IllegalStateException("RpcFuture not found");
        }
        rpcFuture.trySuccess(response.getBytes());
    }
}
