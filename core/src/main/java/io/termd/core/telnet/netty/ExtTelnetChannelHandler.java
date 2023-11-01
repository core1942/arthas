package io.termd.core.telnet.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.termd.core.function.Supplier;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.util.Logging;

/**
 * create by WG on 2022/4/1 15:22
 *
 * @author WangGang
 */
public class ExtTelnetChannelHandler extends ChannelInboundHandlerAdapter {

    private final Supplier<TelnetHandler> factory;
    private ExtNettyTelnetConnection conn;

    public ExtTelnetChannelHandler(Supplier<TelnetHandler> factory) {
        this.factory = factory;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        try {
            int size = buf.readableBytes();
            byte[] data = new byte[size];
            buf.getBytes(0, data);
            conn.receive(data);
        } finally {
            buf.release();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.conn = new ExtNettyTelnetConnection(factory.get(), ctx);
        conn.onInit();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        conn.onClose();
        this.conn = null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Logging.logReportedIoError(cause);
        ctx.close();
    }
}
