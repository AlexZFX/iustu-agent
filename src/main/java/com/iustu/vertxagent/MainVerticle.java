package com.iustu.vertxagent;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

/**
 * Author : Alex
 * Date : 2018/5/30 15:43
 * Description :
 */
public class MainVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        DeploymentOptions options = new DeploymentOptions().setInstances(2);
        vertx.deployVerticle(new HttpVerticle(), options);
    }

}
