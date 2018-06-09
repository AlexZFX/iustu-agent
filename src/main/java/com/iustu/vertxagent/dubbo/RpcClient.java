package com.iustu.vertxagent.dubbo;

import com.iustu.vertxagent.conn.ConnectionManager;
import com.iustu.vertxagent.dubbo.model.*;
import com.iustu.vertxagent.register.IRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    public static final int port = Integer.parseInt(System.getProperty("dubbo.protocol.port"));

    private static final String host = "127.0.0.1";

    private static final String type = System.getProperty("type");

    private final ConnectionManager connectManager = new ConnectionManager(host, port, type);

    public RpcClient(IRegistry registry) {

    }

    public CommonFuture invoke(String interfaceName, String method, String parameterTypesString, String parameter, CommonFuture rpcFuture) {
        final ChannelFuture channelFuture = connectManager.getChannelFuture();
        if (channelFuture.isSuccess()) {
            Channel channel = channelFuture.channel();
            sendRequest(rpcFuture, channel, interfaceName, method, parameterTypesString, parameter);
            return rpcFuture;
        }

        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isCancelled()) {
                rpcFuture.cancel(false);
            } else if (future.isSuccess()) {
                Channel channel = future.channel();
                sendRequest(rpcFuture, channel, interfaceName, method, parameterTypesString, parameter);
            } else {
                rpcFuture.tryFailure(future.cause());
            }
        });

        return rpcFuture;
    }

    private void sendRequest(CommonFuture rpcFuture, Channel channel, String interfaceName, String method, String parameterTypesString, String parameter) {
        try {
            final Request request = createRequest(interfaceName, method, parameterTypesString, parameter);
            channel.writeAndFlush(request).addListener((ChannelFutureListener) writeFuture -> {
                if (writeFuture.isCancelled()) {
                    rpcFuture.cancel(false);
                } else if (writeFuture.isSuccess()) {
                    CommonHolder.registerFuture(writeFuture.channel(), request.getId(), rpcFuture);
                } else {
                    rpcFuture.tryFailure(writeFuture.cause());
                }
            });
        } catch (IOException e) {
            rpcFuture.tryFailure(e);
        }
    }

    private Request createRequest(String interfaceName, String method, String parameterTypesString, String parameter) throws IOException {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(method);
        invocation.setAttachment("path", interfaceName);
        invocation.setParameterTypes(parameterTypesString);    // Dubbo内部用"Ljava/lang/String"来表示参数类型是String

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject(parameter, writer);
        invocation.setArguments(out.toByteArray());

        Request request = new Request();
        request.setVersion("2.0.0");
        request.setTwoWay(true);
        request.setData(invocation);
//        logger.info("requestId=" + request.getId());
        return request;
    }
}
