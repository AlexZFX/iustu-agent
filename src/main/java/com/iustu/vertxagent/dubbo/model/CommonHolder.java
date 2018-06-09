package com.iustu.vertxagent.dubbo.model;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;

public class CommonHolder {

    // key: requestId     value: CommonFuture
    public static final AttributeKey<Map<Long, CommonFuture>> rpcFutureMapKey = AttributeKey.valueOf("CommonFutureMap");

    public static void registerFuture(Channel channel, long requestId, CommonFuture commonFuture) {
        Attribute<Map<Long, CommonFuture>> attr = channel.attr(rpcFutureMapKey);
        Map<Long, CommonFuture> commonFutureMap = attr.get();
        if (commonFutureMap == null) {
            commonFutureMap = new HashMap<>();
            attr.setIfAbsent(commonFutureMap);
        }
        CommonFuture oldFuture = commonFutureMap.put(requestId, commonFuture);
        if (oldFuture != null) {
            throw new IllegalStateException("requestId " + requestId + " exists");
        }
    }

    public static CommonFuture getAndRemoveFuture(Channel channel, long requestId) {
        Attribute<Map<Long, CommonFuture>> attr = channel.attr(rpcFutureMapKey);
        Map<Long, CommonFuture> futureMap = attr.get();
        if (futureMap == null) {
            futureMap = new HashMap<>();
            attr.setIfAbsent(futureMap);
        }
        return futureMap.remove(requestId);
    }
}
