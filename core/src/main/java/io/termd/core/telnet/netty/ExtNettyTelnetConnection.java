package io.termd.core.telnet.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.termd.core.telnet.ExtTelnetConnection;
import io.termd.core.telnet.TelnetHandler;

import java.util.concurrent.TimeUnit;

/**
 * create by WG on 2022/4/1 15:06
 *
 * @author WangGang
 */
public class ExtNettyTelnetConnection extends ExtTelnetConnection {

    final ChannelHandlerContext context;

    public ExtNettyTelnetConnection(TelnetHandler handler, ChannelHandlerContext context) {
        super(handler);
        this.context = context;
    }

    @Override
    protected void execute(Runnable task) {
        context.channel().eventLoop().execute(task);
    }

    @Override
    protected void schedule(Runnable task, long delay, TimeUnit unit) {
        context.channel().eventLoop().schedule(task, delay, unit);
    }

    // Not properly synchronized, but ok for now
    @Override
    protected void send(byte[] data) {
        context.writeAndFlush(Unpooled.buffer().writeBytes(data));
    }

    @Override
    protected void onClose() {
        super.onClose();
    }

    @Override
    public void close() {
        context.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public ChannelHandlerContext channelHandlerContext() {
        return context;
    }

    @Override
    protected ChannelFuture sendAndFlush(byte[] data) {
        return  context.writeAndFlush(Unpooled.buffer().writeBytes(data));
    }
}
