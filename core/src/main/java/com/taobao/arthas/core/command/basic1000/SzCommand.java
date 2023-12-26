package com.taobao.arthas.core.command.basic1000;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Name("sz")
@Summary("download file")
public class SzCommand extends AnnotatedCommand {
    private volatile boolean interrupt = false;
    private volatile boolean close = false;
    private static final Logger logger = LoggerFactory.getLogger(SzCommand.class);

    private static final byte[] START_EVENT = { 0x7a, 0x72, 0x65, 0x61, 0x64, 0x6c, 0x69, 0x6e, 0x65, 0x2e, 0x63, 0x00, 0x33, 0x31, 0x36, 0x32 };
    private static final byte[] END_EVENT = { 0x37, 0x37, 0x20, 0x30, 0x20, 0x31, 0x20, 0x33, 0x31, 0x36, 0x32, 0x00, 0x18, 0x6b, 0x64, 0x62};
    private static final byte[] FAIL_EVENT = { 0x58, 0x69, 0x24, 0x79, 0x70, 0x59, 0x57, 0x39, 0x56, 0x78, 0x45, 0x36, 0x37, 0x3f, 0x3e, 0x2a};
    private static final byte[] CANCEL_EVENT = { 0x78, 0x4e, 0x6f, 0x09, 0x0a, 0x5a, 0x4a, 0x6a, 0x5e, 0x6f, 0x78, 0x69, 0x3e, 0x5a, 0x58, 0x6b};
    private static final String SPLIT = "::";

    private String file;

    @Argument(argName = "file", index = 0)
    @Description("file")
    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public void process(CommandProcess process) {
        String currentPath = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);
        process.interruptHandler(event -> interrupt = true);
        process.endHandler(event -> close = true);
        String targetFilePath = currentPath + "/" + this.file;
        File file = Paths.get(targetFilePath).toFile();
        if (file.exists()) {
            process.writeBinary(START_EVENT);
            try (SeekableByteChannel channel = Files.newByteChannel(file.toPath(), StandardOpenOption.READ)) {
                long length = file.length();
                process.writeBinary((length + SPLIT + this.file).getBytes(StandardCharsets.UTF_8));
                ByteBuffer byteBuffer = ByteBuffer.allocate(ArthasConstants.MAX_HTTP_CONTENT_LENGTH - 100);
                int read;
                while (!interrupt && !close && (read = channel.read(byteBuffer)) != -1) {
                    byteBuffer.flip();
                    byte[] send = new byte[read];
                    byteBuffer.get(send);
                    process.writeBinary(send);
                    byteBuffer.clear();
                }
                if (interrupt) {
                    process.writeBinary(CANCEL_EVENT);
                } else {
                    process.writeBinary(END_EVENT);
                }
            } catch (Exception ignored) {
                process.writeBinary(FAIL_EVENT);
            }
        } else {
            process.writeBinary(FAIL_EVENT);
        }
        process.end();
    }


    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

}
