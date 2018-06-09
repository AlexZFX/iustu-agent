package com.iustu.vertxagent.dubbo.model;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;

public class CommonFuture extends DefaultPromise<byte[]> {
    public CommonFuture(EventExecutor executor) {
        super(executor);
    }

    //    @Override
//    public boolean isComplete() {
//        return false;
//    }
//
//    @Override
//    public Future<Object> setHandler(Handler<AsyncResult<Object>> handler) {
//        return null;
//    }
//
//    @Override
//    public void complete(Object result) {
//
//    }
//
//    @Override
//    public void complete() {
//
//    }
//
//    @Override
//    public void fail(Throwable cause) {
//
//    }
//
//    @Override
//    public void fail(String failureMessage) {
//
//    }
//
//    @Override
//    public boolean tryComplete(Object result) {
//        return false;
//    }
//
//    @Override
//    public boolean tryComplete() {
//        return false;
//    }
//
//    @Override
//    public boolean tryFail(Throwable cause) {
//        return false;
//    }
//
//    @Override
//    public boolean tryFail(String failureMessage) {
//        return false;
//    }
//
//    @Override
//    public Object result() {
//        return null;
//    }
//
//    @Override
//    public Throwable cause() {
//        return null;
//    }
//
//    @Override
//    public boolean succeeded() {
//        return false;
//    }
//
//    @Override
//    public boolean failed() {
//        return false;
//    }
//
//    @Override
//    public void handle(AsyncResult<Object> asyncResult) {
//
//    }
////    private CountDownLatch latch = new CountDownLatch(1);
////
////    private RpcResponse response;
////
////    @Override
////    public boolean cancel(boolean mayInterruptIfRunning) {
////        return false;
////    }
////
////    @Override
////    public boolean isCancelled() {
////        return false;
////    }
////
////    @Override
////    public boolean isDone() {
////        return false;
////    }
////
////    @Override
////    public Object get() throws InterruptedException {
////         //boolean b = latch.await(100, TimeUnit.MICROSECONDS);
////        latch.await();
////        try {
////            return response.getBytes();
////        }catch (Exception e){
////            e.printStackTrace();
////        }
////        return "Error";
////    }
////
////    @Override
////    public Object get(long timeout, TimeUnit unit) throws InterruptedException {
////        boolean b = latch.await(timeout,unit);
////        return response.getBytes();
////    }
////
////    public void done(RpcResponse response){
////        this.response = response;
////        latch.countDown();
////    }
}
