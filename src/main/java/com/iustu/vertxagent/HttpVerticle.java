package com.iustu.vertxagent;

import com.iustu.vertxagent.dubbo.RpcClient;
import com.iustu.vertxagent.dubbo.model.RpcFuture;
import com.iustu.vertxagent.register.Endpoint;
import com.iustu.vertxagent.register.EtcdRegistry;
import com.iustu.vertxagent.register.IRegistry;
import io.netty.util.concurrent.GenericFutureListener;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Author : Alex
 * Date : 2018/5/30 15:56
 * Description :
 */
public class HttpVerticle extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(HttpVerticle.class);

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private final int port = Integer.valueOf(System.getProperty("server.port"));
    private RpcClient rpcClient = new RpcClient(registry);
    private Random random = new Random();
    private List<Endpoint> endpoints = null;
    private final Object lock = new Object();

    private CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClientBuilder.create().setMaxConnTotal(5000).setMaxConnPerRoute(5000).build();

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route().handler(this::routingHandler);
        server.requestHandler(router::accept).listen(port, ar -> {
            if (ar.succeeded()) {
                logger.info("HTTP server running on port : " + port);
                startFuture.complete();
            } else {
                logger.error("Could not start a httpServer", ar.cause());
                startFuture.fail(ar.cause());
            }
        });
    }

    private void routingHandler(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();
        String interfaceName = request.getParam("interface");
        String method = request.getParam("method");
        String parameterTypesString = request.getParam("parameterTypesString");
        String parameter = request.getParam("parameter");
        String type = System.getProperty("type");
        HttpServerResponse response = routingContext.response();
        if ("consumer".equals(type)) {
            try {
                consumer(interfaceName, method, parameterTypesString, parameter, ar -> {
                    if (ar.succeeded()) {
                        response.end(ar.result());
                    } else {
                        logger.error("consumer error", ar.cause());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("provider".equals(type)) {
            try {
                provider(interfaceName, method, parameterTypesString, parameter, ar -> {
                    if (ar.succeeded()) {
                        // TODO: 2018/6/4
                        final Buffer buffer = Buffer.buffer(ar.result());
                        response.end(buffer);
                    } else {
                        logger.error("provider error", ar.cause());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            response.end("Environment variable type is needed to set to provider or consumer");
        }
    }

    public void provider(String interfaceName, String method, String parameterTypesString, String parameter, Handler<AsyncResult<byte[]>> resultHandler) throws Exception {
        final RpcFuture rpcFuture = rpcClient.invoke(interfaceName, method, parameterTypesString, parameter);
        rpcFuture.addListener((GenericFutureListener<RpcFuture>) future -> {
            if (future.isCancelled()) {
                // TODO: 2018/6/4 cancelled
                logger.warn("rpcFuture cancelled");
            } else if (future.isSuccess()) {
                final byte[] bytes = future.getNow();
                resultHandler.handle(Future.succeededFuture(bytes));
            } else {
                future.cause().printStackTrace();
            }
        });
    }

    public void consumer(String interfaceName, String method, String parameterTypesString, String parameter, Handler<AsyncResult<Buffer>> resultHandler) throws Exception {

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
        httpAsyncClient.execute(post, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                try {
                    HttpEntity entity = httpResponse.getEntity();
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    resultHandler.handle(Future.succeededFuture(Buffer.buffer(bytes)));
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

//        MultiMap multiMap = MultiMap.caseInsensitiveMultiMap();
//        multiMap.add("interface", interfaceName)
//                .add("method", method)
//                .add("parameterTypesString", parameterTypesString)
//                .add("parameter", parameter);
//
//        webClient.post(url).sendForm(multiMap, ar -> {
//            if (ar.succeeded()) {
//                HttpResponse<Buffer> response = ar.result();
//                resultHandler.handle(Future.succeededFuture(response.body()));
//            } else {
//                logger.error("send form error", ar.cause());
//            }
//        });

    }
}
