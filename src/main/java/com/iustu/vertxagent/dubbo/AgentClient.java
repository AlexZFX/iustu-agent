package com.iustu.vertxagent.dubbo;

import com.iustu.vertxagent.conn.ConnectionManager;
import com.iustu.vertxagent.dubbo.model.AgentRequestProto;
import com.iustu.vertxagent.dubbo.model.CommonFuture;
import com.iustu.vertxagent.dubbo.model.CommonHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Author : Alex
 * Date : 2018/6/9 12:43
 * Description :
 */
public class AgentClient {

    private static final Logger logger = LoggerFactory.getLogger(AgentClient.class);

    private final ConnectionManager connectionManager;

    private static final AtomicLong atomicLong = new AtomicLong(0);

    public AgentClient(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public CommonFuture invoke(String interfaceName, String method, String parameterTypesString, String parameter, CommonFuture agentFuture) {
        final ChannelFuture channelFuture = connectionManager.getChannelFuture();
        long requestId = atomicLong.getAndIncrement();
        if (requestId < 10) {
            logger.error("atomicLong requestId == " + requestId);
        }
        if (channelFuture.isSuccess()) {
            Channel channel = channelFuture.channel();
            sendAgentRequest(channel, agentFuture, requestId, interfaceName, method, parameterTypesString, parameter);
            return agentFuture;
        }
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isCancelled()) {
                agentFuture.cancel(false);
            } else if (future.isSuccess()) {
                Channel channel = future.channel();
                sendAgentRequest(channel, agentFuture, requestId, interfaceName, method, parameterTypesString, parameter);
            } else {
                agentFuture.tryFailure(future.cause());
            }
        });
        return agentFuture;
    }

    private void sendAgentRequest(Channel channel, CommonFuture agentFuture, long requestId, String interfaceName, String method, String parameterTypesString, String parameter) {
        AgentRequestProto.AgentRequest request = AgentRequestProto.AgentRequest
                .newBuilder()
                .setId(requestId)
                .setInterfaceName(interfaceName)
                .setMethod(method)
                .setParameterTypesString(parameterTypesString)
                .setParameter(parameter)
                .build();
        channel.writeAndFlush(request).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                CommonHolder.registerFuture(future.channel(), requestId, agentFuture);
            } else if (future.isCancelled()) {
                agentFuture.cancel(false);
            } else {
                agentFuture.tryFailure(future.cause());
            }
        });
    }


}
