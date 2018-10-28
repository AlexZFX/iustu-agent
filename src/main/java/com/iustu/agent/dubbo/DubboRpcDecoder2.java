package com.iustu.agent.dubbo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class DubboRpcDecoder2 extends ByteToMessageDecoder {
    // header length.
    protected static final int HEADER_LENGTH = 16;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        try {
            do {
                int savedReaderIndex = byteBuf.readerIndex();
                Object msg;
                try {
                    msg = decode2(byteBuf);
                } catch (Exception e) {
                    throw e;
                }
                if (msg == DecodeResult.NEED_MORE_INPUT) {
                    byteBuf.readerIndex(savedReaderIndex);
                    break;
                }

                list.add(msg);
            } while (byteBuf.isReadable());
        } finally {
            if (byteBuf.isReadable()) {
                byteBuf.discardReadBytes();
            }
        }
    }

    enum DecodeResult {
        NEED_MORE_INPUT
    }

    private Object decode2(ByteBuf buff) {
        int readerIndex = buff.readerIndex();
        int readableBytes = buff.readableBytes();
        if (readableBytes < HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        buff.skipBytes(4);
        long requestId = buff.readLong();
        int dataLen = buff.readInt();

        if (readableBytes < HEADER_LENGTH + dataLen) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        buff.readerIndex(readerIndex + HEADER_LENGTH + dataLen);

        int payloadStart = readerIndex + HEADER_LENGTH;
        ByteBuf payload = buff.retainedDuplicate()
                .readerIndex(payloadStart + 2) // leading "\n"
                .writerIndex(payloadStart + dataLen - 1); // tailing "\n"
        return new RpcResp(requestId, payload);
    }
}
