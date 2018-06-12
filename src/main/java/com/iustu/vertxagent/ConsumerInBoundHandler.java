package com.iustu.vertxagent;

import com.iustu.vertxagent.conn.ConnectionManager;
import com.iustu.vertxagent.dubbo.AgentClient;
import com.iustu.vertxagent.dubbo.model.CommonFuture;
import com.iustu.vertxagent.register.Endpoint;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

/**
 * Author : Alex
 * Date : 2018/6/6 10:13
 * Description :
 */
public class ConsumerInBoundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {


    private static Logger logger = LoggerFactory.getLogger(ConsumerInBoundHandler.class);

    private List<Endpoint> endpoints = null;

    private int endpointSize = 0;

    private EventLoopGroup eventLoopGroup = null;

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    private static final String type = System.getProperty("type");


    private Map<String, AgentClient> agentClientMap;

    public ConsumerInBoundHandler(List<Endpoint> endpoints, EventLoopGroup eventLoopGroup) {
        super();
        this.endpoints = endpoints;
        this.endpointSize = endpoints.size();
        this.eventLoopGroup = eventLoopGroup;
        this.agentClientMap = new HashMap<>();
    }

    //读入consumer的请求
    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws IOException {
//        if (msg instanceof HttpRequest) {
//            FullHttpRequest httpRequest = (FullHttpRequest) msg;
        Map<String, String> paramMap = getParamMap(msg);
        String interfaceName = paramMap.get("interface");
        String method = paramMap.get("method");
        String parameterTypesString = paramMap.get("parameterTypesString");
        String parameter = paramMap.get("parameter");
        try {
            consumer(ctx.channel(), interfaceName, method, parameterTypesString, parameter);
        } catch (Exception e) {
            ctx.channel().close();
            e.printStackTrace();
        }
//            ctx.channel().writeAndFlush(httpResponse)
//                    .addListener((ChannelFutureListener) future1 -> {
//                        if (future1.isSuccess()) {
//                            logger.info("provider write done");
//                        } else {
//                            logger.info("provider write error", future1.cause());
//                        }
//                    });

//        } else {
//            logger.info(msg.toString());
//            ctx.channel().close();
//            logger.error("unknown requset");
//        }
    }

    public void consumer(Channel channel, String interfaceName, String method, String parameterTypesString, String parameter) {

        // 简单的负载均衡，随机取一个
        // TODO: 2018/5/31
//        Endpoint endpoint = endpoints.get(random.nextInt(endpoints.size()));
        Endpoint endpoint = endpoints.get(atomicInteger.getAndIncrement() % endpointSize);

        String agentKey = endpoint.getHost() + endpoint.getPort();
        AgentClient agentClient = agentClientMap.get(agentKey);
        if (agentClient == null) {
            // TODO: 2018/6/9 consumer 线程和连接池大小
            int count = Collections.frequency(endpoints, endpoint);
//            ConnectionManager connectionManager;
//            if (count == 1) {
//                connectionManager = new ConnectionManager(endpoint.getHost(), endpoint.getPort(), type, eventLoopGroup, 4);
//            } else if (count == 2) {
//                connectionManager = new ConnectionManager(endpoint.getHost(), endpoint.getPort(), type, eventLoopGroup, 8);
//            } else {
//                connectionManager = new ConnectionManager(endpoint.getHost(), endpoint.getPort(), type, eventLoopGroup, 8);
//            }
            ConnectionManager connectionManager = new ConnectionManager(endpoint.getHost(), endpoint.getPort(), type, eventLoopGroup, 3 * count);
            agentClient = new AgentClient(connectionManager);
            agentClientMap.put(agentKey, agentClient);
        }
        CommonFuture agentFuture = new CommonFuture(channel.eventLoop());
        agentClient.invoke(interfaceName, method, parameterTypesString, parameter, agentFuture);
        agentFuture.addListener((GenericFutureListener<CommonFuture>) future -> {
            if (future.isSuccess()) {
                final byte[] bytes = future.getNow();
                ByteBuf buffer = channel.alloc().buffer(bytes.length).writeBytes(bytes);
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                HttpHeaders headers = response.headers();
                headers.set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                headers.set(CONTENT_LENGTH, String.valueOf(buffer.readableBytes()));
                channel.writeAndFlush(response)
                ;
            } else if (future.isCancelled()) {
                logger.error("agentFuture canceled");
            } else {
                logger.error("agentFuture failed", future.cause());
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
