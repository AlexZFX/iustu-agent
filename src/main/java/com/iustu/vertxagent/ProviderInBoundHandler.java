package com.iustu.vertxagent;

import com.iustu.vertxagent.dubbo.RpcClient;
import com.iustu.vertxagent.dubbo.model.RpcFuture;
import com.iustu.vertxagent.register.IRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

/**
 * Author : Alex
 * Date : 2018/6/7 19:08
 * Description :
 */
public class ProviderInBoundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static Logger logger = LoggerFactory.getLogger(ConsumerInBoundHandler.class);


    private IRegistry registry;

    private RpcClient rpcClient;

    public ProviderInBoundHandler(IRegistry registry) {
        super();
        this.registry = registry;
        this.rpcClient = new RpcClient(registry);

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        Map<String, String> paramMap = getParamMap(msg);
        String interfaceName = paramMap.get("interface");
        String method = paramMap.get("method");
        String parameterTypesString = paramMap.get("parameterTypesString");
        String parameter = paramMap.get("parameter");
        provider(ctx.channel(), interfaceName, method, parameterTypesString, parameter);
    }


    public void provider(Channel channel, String interfaceName, String method, String parameterTypesString, String parameter) {
        RpcFuture rpcFuture = new RpcFuture(channel.eventLoop());
        rpcClient.invoke(interfaceName, method, parameterTypesString, parameter, rpcFuture);
        rpcFuture.addListener((GenericFutureListener<RpcFuture>) future -> {
            if (future.isCancelled()) {
                // TODO: 2018/6/4 cancelled
                logger.warn("rpcFuture cancelled");
            } else if (future.isSuccess()) {
                final byte[] bytes = future.getNow();
                logger.info("receive response: " + new String(bytes));
//                if (channel.isActive()) {
                ByteBuf buffer = channel.alloc().buffer(bytes.length).writeBytes(bytes);
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                HttpHeaders headers = response.headers();
                headers.set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                headers.set(CONTENT_LENGTH, String.valueOf(bytes.length));
                channel.writeAndFlush(response)
//                        .addListener((ChannelFutureListener) future1 -> {
//                            if (future1.isSuccess()) {
//                                logger.info("provider write done");
//                            } else {
//                                logger.info("provider write error", future1.cause());
//                            }
//                        })
                        .addListener(ChannelFutureListener.CLOSE)
                ;
//                }
            } else {
                future.cause().printStackTrace();
            }
        });
    }


    private Map<String, String> getParamMap(FullHttpRequest httpRequest) throws IOException {
        Map<String, String> paramMap = new HashMap<>();
        if (httpRequest.method() == HttpMethod.GET) {
            QueryStringDecoder decoder = new QueryStringDecoder(httpRequest.uri());
            decoder.parameters().forEach((key, value) -> paramMap.put(key, value.get(0)));
        } else if (httpRequest.method() == HttpMethod.POST) {
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(httpRequest);
            decoder.offer(httpRequest);
            List<InterfaceHttpData> paramList = decoder.getBodyHttpDatas();
            for (InterfaceHttpData param : paramList) {
                Attribute data = (Attribute) param;
                paramMap.put(data.getName(), data.getValue());
            }
        } else {
            logger.error("not support method", httpRequest.uri());
        }
        return paramMap;
    }


}
