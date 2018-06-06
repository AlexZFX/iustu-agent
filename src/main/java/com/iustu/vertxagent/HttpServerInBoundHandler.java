package com.iustu.vertxagent;

import com.iustu.vertxagent.dubbo.RpcClient;
import com.iustu.vertxagent.dubbo.model.RpcFuture;
import com.iustu.vertxagent.register.Endpoint;
import com.iustu.vertxagent.register.EtcdRegistry;
import com.iustu.vertxagent.register.IRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Author : Alex
 * Date : 2018/6/6 10:13
 * Description :
 */
public class HttpServerInBoundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static Logger logger = LoggerFactory.getLogger(HttpServerInBoundHandler.class);

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private RpcClient rpcClient = new RpcClient(registry);
    private Random random = new Random();
    private List<Endpoint> endpoints = null;
    private final Object lock = new Object();

    private CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClientBuilder.create().setMaxConnTotal(5000).setMaxConnPerRoute(5000).build();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws IOException {
//        if (msg instanceof HttpRequest) {
//            FullHttpRequest httpRequest = (FullHttpRequest) msg;
        Map<String, String> paramMap = getParamMap(msg);
        String interfaceName = paramMap.get("interface");
        String method = paramMap.get("method");
        String parameterTypesString = paramMap.get("parameterTypesString");
        String parameter = paramMap.get("parameter");
        String type = System.getProperty("type");
        if ("consumer".equals(type)) {
            try {
                consumer(ctx.channel(), interfaceName, method, parameterTypesString, parameter);
            } catch (Exception e) {
                ctx.channel().close();
                e.printStackTrace();
            }
        } else if ("provider".equals(type)) {
            provider(ctx.channel(), interfaceName, method, parameterTypesString, parameter);
        } else {
            logger.error("unknown system type");
        }
//        } else {
//            logger.info(msg.toString());
//            ctx.channel().close();
//            logger.error("unknown requset");
//        }
    }

    public void provider(Channel channel, String interfaceName, String method, String parameterTypesString, String parameter) {
        final RpcFuture rpcFuture = rpcClient.invoke(interfaceName, method, parameterTypesString, parameter);
        rpcFuture.addListener((GenericFutureListener<RpcFuture>) future -> {
            if (future.isCancelled()) {
                // TODO: 2018/6/4 cancelled
                logger.warn("rpcFuture cancelled");
            } else if (future.isSuccess()) {
                final byte[] bytes = future.getNow();
//                if (channel.isActive()) {
                channel.writeAndFlush(bytes);
//                }
            } else {
                future.cause().printStackTrace();
            }
        });
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

        final HttpPost post = new HttpPost(url);

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("interface", interfaceName));
        params.add(new BasicNameValuePair("method", method));
        params.add(new BasicNameValuePair("parameterTypesString", parameterTypesString));
        params.add(new BasicNameValuePair("parameter", parameter));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params, "utf-8");
        post.setEntity(formEntity);
        httpAsyncClient.execute(post, new FutureCallback<org.apache.http.HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                try {
                    HttpEntity entity = httpResponse.getEntity();
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    channel.writeAndFlush(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Exception e) {
                logger.error(e.getLocalizedMessage());
            }

            @Override
            public void cancelled() {
                logger.debug("request cancelled");
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
