package com.taobao.arthas.core.command.basic1000;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.command.model.CatModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.middleware.cli.annotations.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Name("rz")
@Summary("上传文件")
public class RzCommand extends AnnotatedCommand {
    private volatile boolean interrupt = false;
    private volatile boolean close = false;
    private static final Logger logger = LoggerFactory.getLogger(RzCommand.class);

    private static final byte[] CONFIRM_EVENT = { 0x2a, 0x2a, 0x18, 0x42, 0x30, 0x31, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x32, 0x33, 0x62, 0x65 };
    private static final byte[] START_EVENT = { 0x7a, 0x72, 0x65, 0x61, 0x64, 0x6c, 0x69, 0x6e, 0x65, 0x2e, 0x63, 0x00, 0x33, 0x31, 0x36, 0x32 };
    private static final byte[] END_EVENT = { 0x37, 0x37, 0x20, 0x30, 0x20, 0x31, 0x20, 0x33, 0x31, 0x36, 0x32, 0x00, 0x18, 0x6b, 0x64, 0x62};
    private static final String SPLIT = "::";
    @Override
    public void process(CommandProcess process) {
        String currentPath = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);
        process.interruptHandler(event -> interrupt = true);
        process.endHandler(event -> close = true);
        UploadData uploadData = new UploadData();
        process.stdinHandler(event -> {
            String[] split = event.split(SPLIT);
            if (split.length == 2) {
                String fileName = split[0];
                uploadData.setCount(Long.parseLong(split[1]));
                String targetFilePath = currentPath + "/" + fileName;
                File file = Paths.get(targetFilePath).toFile();
                if (!interrupt && !close && !file.exists()) {
                    try (SeekableByteChannel channel = Files.newByteChannel(file.toPath(), StandardOpenOption.READ)) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate(ArthasConstants.MAX_HTTP_CONTENT_LENGTH);
                        int read;
                        boolean isFinal = false;
                        boolean isContinue = false;
                        long length = file.length();
                        long count = 0;
                        while (!interrupt && !close && (read = channel.read(byteBuffer)) != -1) {
                            count += read;
                            if (count != 0) {
                                isContinue = true;
                            }
                            if (count >= length) {
                                isFinal = true;
                            }
                            byteBuffer.flip();
                            byte[] send = new byte[read];
                            byteBuffer.get(send);
                            process.writeBinary(isFinal, isContinue, send);
                            byteBuffer.clear();
                        }
                    } catch (Exception ignored) {

                    }
                    process.binaryHandler(byteBuf -> {

                    });
                }
                if (interrupt) {
                    process.end(1, "下载已取消");
                } else {
                    process.end();
                }
            }
        });
    }


    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }


    static class UploadData{
       private long count;
        private FileOutputStream fileOutputStream;

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public FileOutputStream getFileOutputStream() {
            return fileOutputStream;
        }

        public void setFileOutputStream(FileOutputStream fileOutputStream) {
            this.fileOutputStream = fileOutputStream;
        }
    }

}
