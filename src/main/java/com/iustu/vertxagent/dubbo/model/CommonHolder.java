package com.iustu.vertxagent.dubbo.model;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;

public class CommonHolder {

    // key: requestId     value: CommonFuture
    public static final AttributeKey<Map<Long, CommonFuture>> rpcFutureMapKey = AttributeKey.valueOf("CommonFutureMap");

    public static void registerRpcFuture(Channel channel, long requestId, CommonFuture commonFuture) {
        Attribute<Map<Long, CommonFuture>> attr = channel.attr(rpcFutureMapKey);
        Map<Long, CommonFuture> commonFutureMap = attr.get();
        if (commonFutureMap == null) {
            commonFutureMap = new HashMap<>();
            attr.setIfAbsent(commonFutureMap);
        }
        CommonFuture oldRpcFuture = commonFutureMap.put(requestId, commonFuture);
        if (oldRpcFuture != null) {
            throw new IllegalStateException("requestId " + requestId + " exists");
        }
    }

    public static CommonFuture getAndRemoveRpcFuture(Channel channel, long requestId) {
        Attribute<Map<Long, CommonFuture>> attr = channel.attr(rpcFutureMapKey);
        Map<Long, CommonFuture> rpcFutureMap = attr.get();
        if (rpcFutureMap == null) {
            rpcFutureMap = new HashMap<>();
            attr.setIfAbsent(rpcFutureMap);
        }
        return rpcFutureMap.remove(requestId);
    }
}
