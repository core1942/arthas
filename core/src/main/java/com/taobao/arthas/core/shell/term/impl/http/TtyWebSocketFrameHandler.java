/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taobao.arthas.core.shell.term.impl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.termd.core.function.Consumer;
import io.termd.core.http.HttpTtyConnection;
import io.termd.core.tty.TtyConnection;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyWebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Pattern P = Pattern.compile("\\A\\p{ASCII}*\\z");
    private final ChannelGroup group;
    private final Consumer<TtyConnection> handler;
    private ChannelHandlerContext context;
    private ExtHttpTtyConnection conn;

    public TtyWebSocketFrameHandler(ChannelGroup group, Consumer<TtyConnection> handler) {
        this.group = group;
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        context = ctx;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            ctx.pipeline().remove(HttpRequestHandler.class);
            group.add(ctx.channel());
            conn = new ExtHttpTtyConnection(context);
            handler.accept(conn);
        } else if (evt instanceof IdleStateEvent) {
            ctx.writeAndFlush(new PingWebSocketFrame());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        HttpTtyConnection tmp = conn;
        context = null;
        conn = null;
        if (tmp != null) {
            Consumer<Void> closeHandler = tmp.getCloseHandler();
            if (closeHandler != null) {
                closeHandler.accept(null);
            }
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            conn.writeToDecoder(((TextWebSocketFrame) frame).text());
            // conn.writeToDecoder(escape(((TextWebSocketFrame) frame).text()));
        } else {
            conn.readBinary(frame.isFinalFragment(), frame.content());
        }
    }

    private String escape(String inpute) {
        if (!P.matcher(inpute).find()) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> obj;
            String action;
            try {
                obj = mapper.readValue(inpute, Map.class);
                action = (String) obj.get("action");
            } catch (IOException e) {
                return inpute;
            }
            if ("read".equals(action)) {
                String data = (String) obj.get("data");
                String escaped = StringEscapeUtils.escapeJava(data);
                obj.put("data", escaped);
                try {
                    return mapper.writeValueAsString(obj);
                } catch (IOException e) {
                    return inpute;
                }
            }
        }
        return inpute;
    }

}
