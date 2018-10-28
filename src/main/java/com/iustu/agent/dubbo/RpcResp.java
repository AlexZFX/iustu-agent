package com.iustu.agent.dubbo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * Author : Alex
 * Date : 2018/6/13 16:16
 * Description :
 */
public class RpcResp extends DefaultByteBufHolder {

    private final long requestId;

    public RpcResp(long requestId, ByteBuf data) {
        super(data);
        this.requestId = requestId;
    }

    public long getRequestId() {
        return requestId;
    }
}
