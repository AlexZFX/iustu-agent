package com.iustu.vertxagent;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

/**
 * Author : Alex
 * Date : 2018/5/30 15:43
 * Description :
 */
public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        DeploymentOptions options = new DeploymentOptions().setInstances(2);
        vertx.deployVerticle(new HttpVerticle(), options, ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(ar.cause());
            }
        });
    }

}
