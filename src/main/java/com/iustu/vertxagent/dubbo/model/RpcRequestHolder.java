package com.iustu.vertxagent.dubbo.model;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;

public class RpcRequestHolder {

    // key: requestId     value: RpcFuture
    public static final AttributeKey<Map<Long, RpcFuture>> rpcFutureMapKey = AttributeKey.valueOf("RpcFutureMap");

    public static void registerRpcFuture(Channel channel, long requestId, RpcFuture rpcFuture) {
        Attribute<Map<Long, RpcFuture>> attr = channel.attr(rpcFutureMapKey);
        Map<Long, RpcFuture> rpcFutureMap = attr.get();
        if (rpcFutureMap == null) {
            rpcFutureMap = new HashMap<>();
            attr.setIfAbsent(rpcFutureMap);
        }
        RpcFuture oldRpcFuture = rpcFutureMap.put(requestId, rpcFuture);
        if (oldRpcFuture != null) {
            throw new IllegalStateException("requestId " + requestId + " exists");
        }
    }

    public static RpcFuture getAndRemoveRpcFuture(Channel channel, long requestId) {
        Attribute<Map<Long, RpcFuture>> attr = channel.attr(rpcFutureMapKey);
        Map<Long, RpcFuture> rpcFutureMap = attr.get();
        if (rpcFutureMap == null) {
            rpcFutureMap = new HashMap<>();
            attr.setIfAbsent(rpcFutureMap);
        }
        return rpcFutureMap.remove(requestId);
    }
}
