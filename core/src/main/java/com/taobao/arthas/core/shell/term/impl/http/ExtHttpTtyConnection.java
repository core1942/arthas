package com.taobao.arthas.core.shell.term.impl.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSession;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.termd.core.function.BiConsumer;
import io.termd.core.function.Function;
import io.termd.core.http.HttpTtyConnection;
import io.termd.core.telnet.ExtBinaryEncoder;
import io.termd.core.tty.ExtTtyOutputMode;
import io.termd.core.util.Helper;
import io.termd.core.util.Vector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 从http请求传递过来的 session 信息。解析websocket创建的 term 还需要登陆验证问题
 * 
 * @author hengyunabc 2021-03-04
 *
 */
public class ExtHttpTtyConnection extends HttpTtyConnection {
    private ChannelHandlerContext context;
    private final Function<int[], ChannelFuture> extstdout;
    private BiConsumer<Boolean, ByteBuf> consumer;

    public ExtHttpTtyConnection(ChannelHandlerContext context) {
        this.extstdout = new ExtTtyOutputMode(new ExtBinaryEncoder(StandardCharsets.UTF_8, this::writeAndFlush));
        this.context = context;
    }

    @Override
    protected void write(byte[] buffer) {
        ByteBuf byteBuf = context.alloc().buffer();
        byteBuf.writeBytes(buffer);
        if (context != null) {
            context.writeAndFlush(new TextWebSocketFrame(byteBuf));
        }
    }

    protected ChannelFuture writeAndFlush(byte[] buffer) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
        return context.writeAndFlush(new TextWebSocketFrame(byteBuf));
    }


    public ChannelFuture writeAndFlush(boolean isFinal, boolean isContinue, byte[] data) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(data);
        WebSocketFrame webSocketFrame;
        if (isContinue) {
            webSocketFrame = new ContinuationWebSocketFrame(isFinal, 0, byteBuf);
        } else {
            webSocketFrame = new BinaryWebSocketFrame(isFinal, 0, byteBuf);
        }
        return context.writeAndFlush(webSocketFrame);
    }

    public void readBinary(boolean isFinal, ByteBuf byteBuf) {
        if (consumer != null) {
            consumer.accept(isFinal, byteBuf);
        }
    }

    public ChannelFuture writeAndFlush(String s) {
        int[] codePoints = Helper.toCodePoints(s);
        return extstdout.apply(codePoints);
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        if (context != null) {
            context.executor().schedule(task, delay, unit);
        }
    }

    @Override
    public void execute(Runnable task) {
        if (context != null) {
            context.executor().execute(task);
        }
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
        }
    }

    public Map<String, Object> extSessions() {
        if (context != null) {
            HttpSession httpSession = HttpSessionManager.getHttpSessionFromContext(context);
            if (httpSession != null) {
                Object subject = httpSession.getAttribute(ArthasConstants.SUBJECT_KEY);
                if (subject != null) {
                    Map<String, Object> result = new HashMap<String, Object>();
                    result.put(ArthasConstants.SUBJECT_KEY, subject);
                    return result;
                }
            }
        }
        return Collections.emptyMap();
    }

    public void setConsumer(BiConsumer<Boolean, ByteBuf> consumer) {
        this.consumer = consumer;
    }
}
