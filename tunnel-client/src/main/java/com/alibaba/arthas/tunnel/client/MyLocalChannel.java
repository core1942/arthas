package com.alibaba.arthas.tunnel.client;

import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;

import java.lang.reflect.Field;
import java.util.Queue;

/**
 * create by WG on 2022/4/13 14:19
 *
 * @author WangGang
 */
public class MyLocalChannel extends LocalChannel {

    private Field superPeerFiled;
    private Field inboundBufferFiled;

    public MyLocalChannel() {
        super();
        init();
    }


    public MyLocalChannel(LocalServerChannel parent, LocalChannel peer) {
        super(parent, peer);
        init();
    }

    private void init() {
        try {
            superPeerFiled = getClass().getSuperclass().getDeclaredField("peer");
            inboundBufferFiled = getClass().getSuperclass().getDeclaredField("inboundBuffer");
            superPeerFiled.setAccessible(true);
            inboundBufferFiled.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        LocalChannel peer = (LocalChannel) superPeerFiled.get(this);
        Queue<Object> inboundBuffer = (Queue<Object>) inboundBufferFiled.get(peer);
        if (inboundBuffer.size() < 512) {
            super.doWrite(in);
        } else {
            peer.eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    flush();
                }
            });
        }
    }
}
