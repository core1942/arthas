package com.alibaba.arthas.tunnel.client;

import com.taobao.arthas.common.ArthasConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author hengyunabc 2019-08-28
 */
public class ForwardClient {
    private final static Logger logger = LoggerFactory.getLogger(ForwardClient.class);
    private URI tunnelServerURI;

    public ForwardClient(URI tunnelServerURI) {
        this.tunnelServerURI = tunnelServerURI;
    }

    public void start() throws URISyntaxException, SSLException, InterruptedException {
        String scheme = tunnelServerURI.getScheme() == null ? "ws" : tunnelServerURI.getScheme();
        final String host = tunnelServerURI.getHost() == null ? "127.0.0.1" : tunnelServerURI.getHost();
        final int port;
        if (tunnelServerURI.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = tunnelServerURI.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            logger.error("Only WS(S) is supported, uri: {}", tunnelServerURI);
            return;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        // connect to local server
        WebSocketClientProtocolConfig clientProtocolConfig = WebSocketClientProtocolConfig.newBuilder()
                .webSocketUri(tunnelServerURI)
                .maxFramePayloadLength(ArthasConstants.MAX_HTTP_CONTENT_LENGTH).build();

        final WebSocketClientProtocolHandler websocketClientHandler = new WebSocketClientProtocolHandler(
                clientProtocolConfig);

        final ForwardClientSocketClientHandler forwardClientSocketClientHandler = new ForwardClientSocketClientHandler();

        final EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-ForwardClient", true));
        ChannelFuture closeFuture = null;
        try {
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(512, 512))
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(new HttpClientCodec(), new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH), websocketClientHandler,
                                    forwardClientSocketClientHandler);
                        }
                    });

            closeFuture = b.connect(tunnelServerURI.getHost(), port).sync().channel().closeFuture();
            logger.info("forward client connect to server success, uri: " + tunnelServerURI);
        } finally {
            if (closeFuture != null) {
                closeFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        group.shutdownGracefully();
                    }
                });
            } else {
                group.shutdownGracefully();
            }
        }

    }

}
