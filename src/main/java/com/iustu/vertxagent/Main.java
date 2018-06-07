package com.iustu.vertxagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author : Alex
 * Date : 2018/6/7 19:03
 * Description :
 */
public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static final String type = System.getProperty("type");

    public static void main(String[] args) {
        if ("provider".equals(type)) {
            ProviderAgent providerAgent = new ProviderAgent();
            try {
                providerAgent.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if ("consumer".equals(type)) {
            ConsumerAgent consumerAgent = new ConsumerAgent();
            try {
                consumerAgent.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            logger.error("unknown type");
        }

    }
}
