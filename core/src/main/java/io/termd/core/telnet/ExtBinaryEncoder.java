package io.termd.core.telnet;

import io.netty.channel.ChannelFuture;
import io.termd.core.function.Function;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * create by WG on 2022/4/1 13:51
 *
 * @author WangGang
 */
public class ExtBinaryEncoder implements Function<int[], ChannelFuture> {

    private volatile Charset charset;
    final Function<byte[], ChannelFuture> onByte;

    public ExtBinaryEncoder(Charset charset, Function<byte[], ChannelFuture> onByte) {
        this.charset = charset;
        this.onByte = onByte;
    }

    /**
     * Set a new charset on the encoder.
     *
     * @param charset the new charset
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    @Override
    public ChannelFuture apply(int[] codePoints) {
        final char[] tmp = new char[2];
        int capacity = 0;
        for (int codePoint : codePoints) {
            capacity += Character.charCount(codePoint);
        }
        CharBuffer charBuf = CharBuffer.allocate(capacity);
        for (int codePoint : codePoints) {
            int size = Character.toChars(codePoint, tmp, 0);
            charBuf.put(tmp, 0, size);
        }
        charBuf.flip();
        ByteBuffer bytesBuf = charset.encode(charBuf);
        byte[] bytes = bytesBuf.array();
        if (bytesBuf.limit() < bytesBuf.array().length) {
            bytes = Arrays.copyOf(bytes, bytesBuf.limit());
        }
        return onByte.apply(bytes);
    }
}
