package com.iustu.vertxagent;

import com.google.protobuf.ByteString;
import com.iustu.vertxagent.dubbo.RpcClient;
import com.iustu.vertxagent.dubbo.model.AgentRequestProto;
import com.iustu.vertxagent.dubbo.model.AgentResponseProto;
import com.iustu.vertxagent.dubbo.model.CommonFuture;
import com.iustu.vertxagent.register.IRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
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

/**
 * Author : Alex
 * Date : 2018/6/7 19:08
 * Description :
 */
public class ProviderInBoundHandler extends SimpleChannelInboundHandler<AgentRequestProto.AgentRequest> {

    private static Logger logger = LoggerFactory.getLogger(ConsumerInBoundHandler.class);


    private IRegistry registry;

    private RpcClient rpcClient;

    public ProviderInBoundHandler(IRegistry registry, RpcClient rpcClient) {
        super();
        this.registry = registry;
        this.rpcClient = rpcClient;

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AgentRequestProto.AgentRequest msg) throws Exception {
//        Map<String, String> paramMap = getParamMap(msg);
//        String interfaceName = paramMap.get("interface");
//        String method = paramMap.get("method");
//        String parameterTypesString = paramMap.get("parameterTypesString");
//        String parameter = paramMap.get("parameter");
        String interfaceName = msg.getInterfaceName();
        String method = msg.getMethod();
        String parameterTypesString = msg.getParameterTypesString();
        String parameter = msg.getParameter();
        provider(ctx.channel(), interfaceName, method, parameterTypesString, parameter);
    }


    public void provider(Channel channel, String interfaceName, String method, String parameterTypesString, String parameter) {
        CommonFuture rpcFuture = new CommonFuture(channel.eventLoop());
        rpcClient.invoke(interfaceName, method, parameterTypesString, parameter, rpcFuture);
        rpcFuture.addListener((GenericFutureListener<CommonFuture>) future -> {
            if (future.isCancelled()) {
                // TODO: 2018/6/4 cancelled
                logger.warn("rpcFuture cancelled");
            } else if (future.isSuccess()) {
                final byte[] bytes = future.getNow();
                logger.info("receive provider response: " + new String(bytes));
                AgentResponseProto.AgentResponse response = AgentResponseProto.AgentResponse
                        .newBuilder()
                        .setId(31231L)
                        .setData(ByteString.copyFrom(bytes))
                        .build();

//                if (channel.isActive()) {
//                ByteBuf buffer = channel.alloc().buffer(bytes.length).writeBytes(bytes);
//                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
//                HttpHeaders headers = response.headers();
//                headers.set(CONTENT_TYPE, "text/plain; charset=UTF-8");
//                headers.set(CONTENT_LENGTH, String.valueOf(bytes.length));
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
