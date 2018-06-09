package com.iustu.vertxagent;

import com.iustu.vertxagent.conn.ConnectionManager;
import com.iustu.vertxagent.dubbo.AgentClient;
import com.iustu.vertxagent.dubbo.model.CommonFuture;
import com.iustu.vertxagent.register.Endpoint;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

/**
 * Author : Alex
 * Date : 2018/6/6 10:13
 * Description :
 */
public class ConsumerInBoundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {


    private static Logger logger = LoggerFactory.getLogger(ConsumerInBoundHandler.class);

    private AtomicLong atomicInteger = new AtomicLong(0);

    private Random random = new Random();

    private List<Endpoint> endpoints = null;

    private static final String type = System.getProperty("type");


    private Map<String, AgentClient> agentClientMap = new HashMap<>();

    public ConsumerInBoundHandler(List<Endpoint> endpoints) {
        super();
        this.endpoints = endpoints;
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

    public void consumer(Channel channel, String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {


        // 简单的负载均衡，随机取一个
        // TODO: 2018/5/31
        Endpoint endpoint = endpoints.get(random.nextInt(endpoints.size()));

        String agentKey = endpoint.getHost() + endpoint.getPort();
        AgentClient agentClient = agentClientMap.get(agentKey);
        if (agentClient == null) {
            // TODO: 2018/6/9 consumer 线程和连接池大小 
            ConnectionManager connectionManager = new ConnectionManager(endpoint.getHost(), endpoint.getPort(), type, 4, 1);
            agentClient = new AgentClient(connectionManager);
            agentClientMap.put(agentKey, agentClient);
        }
        CommonFuture agentFuture = new CommonFuture(channel.eventLoop());
        agentClient.invoke(interfaceName, method, parameterTypesString, parameter, agentFuture);
        agentFuture.addListener((GenericFutureListener<CommonFuture>) future -> {
            if (future.isSuccess()) {
                final byte[] bytes = future.getNow();
                ByteBuf buffer = channel.alloc().buffer(bytes.length).writeBytes(bytes);
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer.retain());
                HttpHeaders headers = response.headers();
                headers.set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                headers.set(CONTENT_LENGTH, String.valueOf(buffer.readableBytes()));
                channel.writeAndFlush(response)
                ;
            } else if (future.isCancelled()) {
                logger.error("agentFuture canceled");
            } else {
                logger.error("agentFuture failed", future.cause().getLocalizedMessage());
            }
        });

//        RequestBody requestBody = new FormBody.Builder()
//                .add("interface", interfaceName)
//                .add("method", method)
//                .add("parameterTypesString", parameterTypesString)
//                .add("parameter", parameter)
//                .build();
//
//        Request request = new Request.Builder()
//                .url(url)
//                .post(requestBody)
//                .build();
//        OkHttpClient httpClient = new OkHttpClient.Builder()
//                .readTimeout(30, TimeUnit.SECONDS)
//                .build();
//        Call call = httpClient.newCall(request);
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                logger.info("resp from provider error: ", e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                ResponseBody body = response.body();
//                if (body != null) {
//                    try {
//                        byte[] bytes = body.bytes();
//                        logger.info("resp from provider: " + bytes.length + " | " + bytes);
//
////                        String string = new String(bytes);
//                        ByteBuf buffer = channel.alloc().ioBuffer(bytes.length).writeBytes(bytes);
//                        DefaultFullHttpResponse newResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
//                        HttpHeaders headers = newResponse.headers();
//                        headers.set(CONTENT_TYPE, "text/plain; charset=UTF-8");
//                        headers.set(CONTENT_LENGTH, String.valueOf(bytes.length));
//                        channel.writeAndFlush(newResponse)
//                                .addListener(future -> {
//                                    if(future.isSuccess()){
//                                        logger.error("success");
//                                        logger.error("failed");
//                                    }
//                                })
//                                .addListener(ChannelFutureListener.CLOSE)
//                        ;
////                        logger.info("resp from provider: " + string);
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    logger.info("resp from provider: body == null");
//                }
//            }
//        });

//        final HttpPost post = new HttpPost(url);
//
//        List<BasicNameValuePair> params = new ArrayList<>();
//        params.add(new BasicNameValuePair("interface", interfaceName));
//        params.add(new BasicNameValuePair("method", method));
//        params.add(new BasicNameValuePair("parameterTypesString", parameterTypesString));
//        params.add(new BasicNameValuePair("parameter", parameter));
//        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params, "utf-8");
//        post.setEntity(formEntity);
//        httpAsyncClient.execute(post, new FutureCallback<HttpResponse>() {
//            @Override
//            public void completed(HttpResponse httpResponse) {
//                try {
//                    byte[] bytes = EntityUtils.toByteArray(httpResponse.getEntity());
//                    ByteBuf buffer = channel.alloc().ioBuffer(bytes.length).writeBytes(bytes);
//                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
//                    HttpHeaders headers = response.headers();
//                    headers.set(CONTENT_TYPE, "text/plain; charset=UTF-8");
//                    headers.set(CONTENT_LENGTH, String.valueOf(bytes.length));
//                    channel.writeAndFlush(response)
//                            .addListener(ChannelFutureListener.CLOSE)
//                    ;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void failed(Exception e) {
//                logger.error("consumer response failed " + e.getLocalizedMessage(), e);
//            }
//
//            @Override
//            public void cancelled() {
//                logger.debug("consumer request cancelled");
//            }
//        });
//        try (Response response = call.execute()) {
//            if (response.isSuccessful()) {
//                channel.writeAndFlush(response.body().bytes());
//            }
//        }

//        HttpResponse response = future.get(6000, TimeUnit.MILLISECONDS);
//        channel.writeAndFlush(EntityUtils.toByteArray(response.getEntity()));
//                new FutureCallback<org.apache.http.HttpResponse>() {
//            @Override
//            public void completed(HttpResponse httpResponse) {
//                try {
//                    HttpEntity entity = httpResponse.getEntity();
//                    byte[] bytes = EntityUtils.toByteArray(entity);
//                    channel.writeAndFlush(bytes);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void failed(Exception e) {
//                logger.error("consumer response failed " + e.getLocalizedMessage(), e);
//            }
//
//            @Override
//            public void cancelled() {
//                logger.debug("consumer request cancelled");
//            }
//        });

//        Bootstrap bootstrap = new Bootstrap();
//        bootstrap.group(channel.eventLoop())
//                .channel(NioSocketChannel.class)
//                .handler(new ChannelInitializer<NioSocketChannel>() {
//                    @Override
//                    protected void initChannel(NioSocketChannel ch) throws Exception {
//                        ch.pipeline()
//                                .addLast(new ProtobufVarint32FrameDecoder())
//                                .addLast(new ProtobufDecoder(AgentResponseProto.AgentResponse.getDefaultInstance()))
//                                .addLast(new ProtobufVarint32LengthFieldPrepender())
//                                .addLast(new ProtobufEncoder())
//                                .addLast(new SimpleChannelInboundHandler<AgentResponseProto.AgentResponse>() {
//
//                                    @Override
//                                    public void channelRead0(ChannelHandlerContext ctx, AgentResponseProto.AgentResponse msg) throws Exception {
////                                        ByteBuf buffer = msg.getData();
//                                        byte[] bytes = msg.getData().toByteArray();
//                                        ByteBuf buffer = channel.alloc().buffer(bytes.length).writeBytes(bytes);
//                                        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer.retain());
//                                        HttpHeaders headers = response.headers();
//                                        headers.set(CONTENT_TYPE, "text/plain; charset=UTF-8");
//                                        headers.set(CONTENT_LENGTH, String.valueOf(buffer.readableBytes()));
//                                        channel.writeAndFlush(response)
////                                                .addListener(future -> {
////                                                    if (future.isSuccess()) {
////                                                        logger.error("return consumer success");
////                                                    } else {
////                                                        logger.error("return consumer failed",future.cause());
////                                                    }
////                                                })
//                                        ;
//                                    }
//
//                                    @Override
//                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//                                        super.channelActive(ctx);
//                                        AgentRequestProto.AgentRequest request = AgentRequestProto.AgentRequest
//                                                .newBuilder()
//                                                .setId(atomicInteger.getAndIncrement())
//                                                .setInterfaceName(interfaceName)
//                                                .setMethod(method)
//                                                .setParameterTypesString(parameterTypesString)
//                                                .setParameter(parameter).build();
////                                        HttpRequest httpRequest = new DefaultFullHttpRequest(
////                                                HttpVersion.HTTP_1_1,
////                                                HttpMethod.POST,
////                                                url
////                                        );
////                                        HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
////                                        HttpPostRequestEncoder bodyRequestEncoder = new HttpPostRequestEncoder(factory, httpRequest, false);
////                                        bodyRequestEncoder.addBodyAttribute("interface", interfaceName);
////                                        bodyRequestEncoder.addBodyAttribute("method", method);
////                                        bodyRequestEncoder.addBodyAttribute("parameterTypesString", parameterTypesString);
////                                        bodyRequestEncoder.addBodyAttribute("parameter", parameter);
////                                        httpRequest = bodyRequestEncoder.finalizeRequest();
//                                        ctx.channel().writeAndFlush(request);
//                                    }
//
//                                });
//                    }
//                })
//                .connect(endpoint.getHost(), endpoint.getPort());

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
