package io.termd.core.tty;

import io.netty.channel.ChannelFuture;
import io.termd.core.function.Function;

/**
 * create by WG on 2022/4/1 13:58
 *
 * @author WangGang
 */
public class ExtTtyOutputMode implements Function<int[], ChannelFuture> {

    private final Function<int[], ChannelFuture> readHandler;

    public ExtTtyOutputMode(Function<int[], ChannelFuture> readHandler) {
        this.readHandler = readHandler;
    }

    @Override
    public ChannelFuture apply(int[] data) {
        ChannelFuture lastChannelFuture = null;
        if (readHandler != null && data.length > 0) {
            int prev = 0;
            int ptr = 0;
            while (ptr < data.length) {
                // Simple implementation that works only on system that uses /n as line terminator
                // equivalent to 'stty onlcr'
                int cp = data[ptr];
                if (cp == '\n') {
                    if (ptr > prev) {
                        sendChunk(data, prev, ptr);
                    }
                    lastChannelFuture = readHandler.apply(new int[]{'\r','\n'});
                    prev = ++ptr;
                } else {
                    ptr++;
                }
            }
            if (ptr > prev) {
                lastChannelFuture = sendChunk(data, prev, ptr);
            }
        }
        return lastChannelFuture;
    }

    private ChannelFuture sendChunk(int[] data, int prev, int ptr) {
        int len = ptr - prev;
        int[] buf = new int[len];
        System.arraycopy(data, prev, buf, 0, len);
        return readHandler.apply(buf);
    }
}
