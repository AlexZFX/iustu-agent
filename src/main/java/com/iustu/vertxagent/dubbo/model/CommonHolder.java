package com.iustu.vertxagent.dubbo.model;

import io.netty.util.concurrent.FastThreadLocal;

import java.util.HashMap;

public class CommonHolder {

    // key: requestId     value: CommonFuture
//    public static final AttributeKey<Map<Long, CommonFuture>> rpcFutureMapKey = AttributeKey.valueOf("CommonFutureMap");

    public static FastThreadLocal<HashMap<Long, CommonFuture>> futureMapHolder = new FastThreadLocal<>();

//    public static final Object lock = new Object();


    public static void registerFuture(Long requestId, CommonFuture commonFuture) {
        if (futureMapHolder.get() == null) {
            futureMapHolder.set(new HashMap<Long, CommonFuture>());
        }
        futureMapHolder.get().put(requestId, commonFuture);
    }

//    public static void registerFuture(Channel channel, long requestId, CommonFuture commonFuture) {
//        HashMap<Long, CommonFuture> futureMap = channelMap.computeIfAbsent(channel.id(), k -> new HashMap<>());
////        futureMap.put(requestId, commonFuture);
//        Attribute<Map<Long, CommonFuture>> attr = channel.attr(rpcFutureMapKey);
//        Map<Long, CommonFuture> commonFutureMap = attr.get();
//        if (commonFutureMap == null) {
//            commonFutureMap = new HashMap<>();
//            attr.setIfAbsent(commonFutureMap);
//        }
//        commonFutureMap.put(requestId, commonFuture);
//        CommonFuture oldFuture = commonFutureMap.put(requestId, commonFuture);
//        if (oldFuture != null) {
//            throw new IllegalStateException("requestId " + requestId + " exists");
//        }
//    }


    public static CommonFuture getAndRemoveFuture(Long requestId) {
        return futureMapHolder.get().remove(requestId);
    }

//    public static CommonFuture getAndRemoveFuture(Channel channel, long requestId) {
//        Attribute<Map<Long, CommonFuture>> attr = channel.attr(rpcFutureMapKey);
//        Map<Long, CommonFuture> futureMap = attr.get();
//        return futureMap.remove(requestId);
//    }

}
