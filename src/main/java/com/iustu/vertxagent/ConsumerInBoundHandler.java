package com.iustu.vertxagent;

import com.iustu.vertxagent.dubbo.RpcClient;
import com.iustu.vertxagent.register.Endpoint;
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
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

/**
 * Author : Alex
 * Date : 2018/6/6 10:13
 * Description :
 */
public class ConsumerInBoundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {


    private static Logger logger = LoggerFactory.getLogger(ConsumerInBoundHandler.class);


    private IRegistry registry;

    private RpcClient rpcClient;
    private Random random = new Random();
    private List<Endpoint> endpoints = null;
    private final Object lock = new Object();

    private CloseableHttpAsyncClient httpAsyncClient;
//    private CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClientBuilder.create().setMaxConnTotal(5000).setMaxConnPerRoute(5000).build();

//    private NioEventLoopGroup consumerEvenvLoops = new NioEventLoopGroup(4);

    public ConsumerInBoundHandler(IRegistry registry) throws IOReactorException {
        super();
        this.registry = registry;
        this.rpcClient = new RpcClient(registry);
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
        PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(ioReactor);
        cm.setMaxTotal(100);
        httpAsyncClient = HttpAsyncClients.custom().setConnectionManager(cm).build();
        httpAsyncClient.start();
    }

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
        if (null == endpoints) {
            synchronized (lock) {
                if (null == endpoints) {
                    endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                }
            }
        }

        // 简单的负载均衡，随机取一个
        // TODO: 2018/5/31
        Endpoint endpoint = endpoints.get(random.nextInt(endpoints.size()));

        final String url = "http://" + endpoint.getHost() + ":" + endpoint.getPort();

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

        final HttpPost post = new HttpPost(url);

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("interface", interfaceName));
        params.add(new BasicNameValuePair("method", method));
        params.add(new BasicNameValuePair("parameterTypesString", parameterTypesString));
        params.add(new BasicNameValuePair("parameter", parameter));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params, "utf-8");
        post.setEntity(formEntity);
        httpAsyncClient.execute(post, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                try {
                    byte[] bytes = EntityUtils.toByteArray(httpResponse.getEntity());
                    ByteBuf buffer = channel.alloc().ioBuffer(bytes.length).writeBytes(bytes);
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                    HttpHeaders headers = response.headers();
                    headers.set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                    headers.set(CONTENT_LENGTH, String.valueOf(bytes.length));
                    channel.writeAndFlush(response)
                            .addListener(ChannelFutureListener.CLOSE)
                    ;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Exception e) {
                logger.error("consumer response failed " + e.getLocalizedMessage(), e);
            }

            @Override
            public void cancelled() {
                logger.debug("consumer request cancelled");
            }
        });
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
//        bootstrap.group(consumerEvenvLoops)
//                .channel(NioSocketChannel.class)
//                .handler(new ChannelInitializer<NioSocketChannel>() {
//                    @Override
//                    protected void initChannel(NioSocketChannel ch) throws Exception {
//                        ch.pipeline()
//                                .addLast(new HttpClientCodec())
//                                .addLast(new HttpObjectAggregator(4096))
//                                .addLast(new ChannelInboundHandlerAdapter() {
//                                    @Override
//                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//                                        super.channelActive(ctx);
//
//                                        DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(
//                                                HttpVersion.HTTP_1_1,
//                                                HttpMethod.POST,
//                                                url
//                                        );
//                                        httpRequest.
//                                    }
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
