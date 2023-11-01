package io.termd.core.telnet;

import io.netty.channel.ChannelFuture;

/**
 * create by WG on 2022/4/1 15:07
 *
 * @author WangGang
 */
public abstract class ExtTelnetConnection extends TelnetConnection{

    public ExtTelnetConnection(TelnetHandler handler) {
        super(handler);
    }

    public final ChannelFuture writeAndFlush(byte[] data) {
        ChannelFuture channelFuture = null;
        if (sendBinary) {
            // actually the logic here never get executed.
            int prev = 0;
            for (int i = 0;i < data.length;i++) {
                if (data[i] == -1) {
                    rawWriteAndFlush(data, prev, i - prev);
                    sendAndFlush(new byte[]{-1, -1});
                    prev = i + 1;
                }
            }
            return rawWriteAndFlush(data, prev, data.length - prev);
        } else {
            // Not fully understand the logic below, but
            // Chinese characters will be truncated by the following logic.
            // So currently these logic is commented out.
            // see middleware-container/arthas/issues/246 for more details
            //
//      for (int i = 0;i < data.length;i++) {
//        data[i] = (byte)(data[i] & 0x7F);
//      }
            return sendAndFlush(data);
        }
    }

    private ChannelFuture rawWriteAndFlush(byte[] data, int offset, int length) {
        if (length > 0) {
            if (offset == 0 && length == data.length) {
                return sendAndFlush(data);
            } else {
                byte[] chunk = new byte[length];
                System.arraycopy(data, offset, chunk, 0, chunk.length);
                return sendAndFlush(chunk);
            }
        }
        return null;
    }

    protected abstract ChannelFuture sendAndFlush(byte[] data);
}
