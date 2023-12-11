package io.termd.core.telnet;

import io.netty.channel.ChannelFuture;
import io.termd.core.function.BiConsumer;
import io.termd.core.function.Consumer;
import io.termd.core.function.Function;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.*;
import io.termd.core.util.Helper;
import io.termd.core.util.Vector;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * create by WG on 2022/4/1 13:50
 *
 * @author WangGang
 */
public class ExtTelnetTtyConnection extends TelnetHandler implements TtyConnection {

    private static final Charset US_ASCII = StandardCharsets.US_ASCII;

    private final boolean inBinary;
    private final boolean outBinary;
    private boolean receivingBinary;
    private boolean sendingBinary;
    private boolean accepted;
    private Vector size;
    private String terminalType;
    private Consumer<Vector> sizeHandler;
    private Consumer<String> termHandler;
    private Consumer<Void> closeHandler;
    protected ExtTelnetConnection conn;
    private final Charset charset;
    private final TtyEventDecoder eventDecoder = new TtyEventDecoder(3, 26, 4);
    private final ReadBuffer readBuffer = new ReadBuffer(ExtTelnetTtyConnection.this::execute);

    private final BinaryDecoder decoder;
    private final ExtBinaryEncoder extencoder;
    private final Function<int[], ChannelFuture>  extstdout;
    private final BinaryEncoder encoder;
    private final Consumer<int[]> stdout;

    private final Consumer<TtyConnection> handler;
    private long lastAccessedTime = System.currentTimeMillis();

    public ExtTelnetTtyConnection(boolean inBinary, boolean outBinary, Charset charset, Consumer<TtyConnection> handler) {
        this.charset = charset;
        this.inBinary = inBinary;
        this.outBinary = outBinary;
        this.handler = handler;
        this.size = new Vector();
        this.decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, readBuffer);
        this.extencoder = new ExtBinaryEncoder(charset, data -> conn.writeAndFlush(data));
        this.extstdout = new ExtTtyOutputMode(extencoder);
        this.encoder = new BinaryEncoder(charset, data -> conn.write(data));
        this.stdout = new TtyOutputMode(encoder);
    }

    @Override
    public long lastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public String terminalType() {
        return terminalType;
    }

    @Override
    public void execute(Runnable task) {
        conn.execute(task);
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        conn.schedule(task, delay, unit);
    }

    @Override
    public Charset inputCharset() {
        return inBinary ? charset : US_ASCII;
    }

    @Override
    public Charset outputCharset() {
        return outBinary ? charset : US_ASCII;
    }

    @Override
    protected void onSendBinary(boolean binary) {
        sendingBinary = binary;
        if (binary) {
            extencoder.setCharset(charset);
            encoder.setCharset(charset);
        }
        checkAccept();
    }

    @Override
    protected void onReceiveBinary(boolean binary) {
        receivingBinary = binary;
        if (binary) {
            decoder.setCharset(charset);
        }
        checkAccept();
    }

    @Override
    protected void onData(byte[] data) {
        lastAccessedTime = System.currentTimeMillis();
        decoder.write(data);
    }

    @Override
    protected void onOpen(TelnetConnection conn) {
        this.conn = (ExtTelnetConnection)conn;

        // Kludge mode
        conn.writeWillOption(Option.ECHO);
        conn.writeWillOption(Option.SGA);

        //
        if (inBinary) {
            conn.writeDoOption(Option.BINARY);
        }
        if (outBinary) {
            conn.writeWillOption(Option.BINARY);
        }

        // Window size
        conn.writeDoOption(Option.NAWS);

        // Get some info about user
        conn.writeDoOption(Option.TERMINAL_TYPE);

        //
        checkAccept();
    }

    private void checkAccept() {
        if (!accepted) {
            if (!outBinary | (outBinary && sendingBinary)) {
                if (!inBinary | (inBinary && receivingBinary)) {
                    accepted = true;
                    readBuffer.setReadHandler(eventDecoder);
                    handler.accept(this);
                }
            }
        }
    }

    @Override
    protected void onTerminalType(String terminalType) {
        this.terminalType = terminalType;
        if (termHandler != null) {
            termHandler.accept(terminalType);
        }
    }

    @Override
    public Vector size() {
        return size;
    }

    @Override
    protected void onSize(int width, int height) {
        this.size = new Vector(width, height);
        if (sizeHandler != null) {
            sizeHandler.accept(size);
        }
    }

    @Override
    public Consumer<Vector> getSizeHandler() {
        return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Vector> handler) {
        this.sizeHandler = handler;
    }

    @Override
    public Consumer<String> getTerminalTypeHandler() {
        return termHandler;
    }

    @Override
    public void setTerminalTypeHandler(Consumer<String> handler) {
        termHandler = handler;
        if (handler != null && terminalType != null) {
            handler.accept(terminalType);
        }
    }

    @Override
    public BiConsumer<TtyEvent, Integer> getEventHandler() {
        return eventDecoder.getEventHandler();
    }

    @Override
    public void setEventHandler(BiConsumer<TtyEvent, Integer> handler) {
        eventDecoder.setEventHandler(handler);
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
        return eventDecoder.getReadHandler();
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        eventDecoder.setReadHandler(handler);
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
        return stdout;
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
        this.closeHandler = closeHandler;
    }

    @Override
    public Consumer<Void> getCloseHandler() {
        return closeHandler;
    }

    @Override
    protected void onClose() {
        if (closeHandler != null) {
            closeHandler.accept(null);
        }
    }

    @Override
    public void close() {
        conn.close();
    }

    @Override
    public void close(int exit) {
        close();
    }

    @Override
    public TtyConnection write(String s) {
        int[] codePoints = Helper.toCodePoints(s);
        stdoutHandler().accept(codePoints);
        return this;
    }

    public ChannelFuture writeAndFlush(String s) {
        int[] codePoints = Helper.toCodePoints(s);
        return extstdout.apply(codePoints);
    }

    public TelnetConnection getTelnetConnection() {
        return this.conn;
    }
}
