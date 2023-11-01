package com.alibaba.arthas.tunnel.client;

import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;

/**
 * create by WG on 2022/4/13 14:37
 *
 * @author WangGang
 */
public class MyLocalServerChannel extends LocalServerChannel {
    @Override
    protected LocalChannel newLocalChannel(LocalChannel peer) {
        return new MyLocalChannel(this, peer);
    }
}
