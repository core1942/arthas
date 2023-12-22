package com.taobao.arthas.core.command.basic1000;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Name("rz")
@Summary("上传文件")
public class RzCommand extends AnnotatedCommand {
    private volatile boolean interrupt = false;
    private volatile boolean close = false;
    private static final Logger logger = LoggerFactory.getLogger(RzCommand.class);

    private static final byte[] START_EVENT = { 0x5a, 0x62, 0x69, 0x37, 0x54, 0x4c, 0x1a, 0x6b, 0x4c, 0x2d, 0x3e, 0x00, 0x3f, 0x3a, 0x1b, 0x30 };
    private static final byte[] CONFIRM_EVENT = { 0x2a, 0x2a, 0x18, 0x42, 0x30, 0x31, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x32, 0x33, 0x62, 0x65 };
    private static final byte[] END_EVENT = { 0x37, 0x37, 0x20, 0x30, 0x20, 0x31, 0x20, 0x33, 0x31, 0x36, 0x32, 0x00, 0x18, 0x6b, 0x64, 0x62};
    private static final byte[] FAIL_EVENT = { 0x58, 0x69, 0x24, 0x79, 0x70, 0x59, 0x57, 0x39, 0x56, 0x78, 0x45, 0x36, 0x37, 0x3f, 0x3e, 0x2a};
    private static final byte[] CANCEL_EVENT = { 0x78, 0x4e, 0x6f, 0x09, 0x0a, 0x5a, 0x4a, 0x6a, 0x5e, 0x6f, 0x78, 0x69, 0x3e, 0x5a, 0x58, 0x6b};
    private static final String SPLIT = "::";
    @Override
    public void process(CommandProcess process) {
        String currentPath = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);
        process.interruptHandler(event -> interrupt = true);
        process.endHandler(event -> close = true);
        UploadData uploadData = new UploadData();
        process.stdinHandler(event -> {
            process.stdinHandler(null);
            String[] split = event.split(SPLIT);
            if (split.length == 2) {
                String fileName = split[0];
                String targetFilePath = currentPath + "/" + fileName;
                File file = Paths.get(targetFilePath).toFile();
                uploadData.setCount(new BigDecimal(Long.parseLong(split[1])));
                uploadData.setFile(file);
                if (file.exists()) {
                    process.writeBinary(CONFIRM_EVENT);
                    process.stdinHandler(confirm -> {
                        process.stdinHandler(null);
                        if ("y".equalsIgnoreCase(confirm)) {
                            downloadFile(process, uploadData);
                        } else {
                            process.writeBinary(CANCEL_EVENT);
                        }
                    });
                }else {
                    downloadFile(process, uploadData);
                }
                if (interrupt) {
                    process.writeBinary(CANCEL_EVENT);
                } else {
                    process.writeBinary(END_EVENT);
                }

            }
        });



    }

    private void downloadFile(CommandProcess process, UploadData uploadData) {
        SeekableByteChannel channel;
        try {
            channel = Files.newByteChannel(uploadData.file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            process.binaryConsumer((isFinal, byteBuf) -> {
                try {
                    if (interrupt || close) {
                        process.binaryConsumer(null);
                        if (interrupt) {
                            process.writeBinary(CANCEL_EVENT);
                        }
                        channel.close();
                    } else {
                        channel.write(byteBuf.nioBuffer());
                        BigDecimal percentage = new BigDecimal(byteBuf.readableBytes()).divide(uploadData.count, 4, RoundingMode.HALF_UP).movePointRight(2);
                        process.write(percentage + "%");
                        if (isFinal) {
                            process.binaryConsumer(null);
                            process.write("100.00%");
                            process.writeBinary(END_EVENT);
                            channel.close();
                        }
                    }
                } catch (Exception e) {
                    try {
                        channel.close();
                    } catch (Exception ignored) {
                    }
                    process.binaryConsumer(null);
                    process.writeBinary(FAIL_EVENT);
                }
            });
        } catch (Exception e) {
            process.writeBinary(FAIL_EVENT);
        }
        process.writeBinary(START_EVENT);

    }


    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }


    static class UploadData{
       private BigDecimal count;
        private File file;

        public BigDecimal getCount() {
            return count;
        }

        public void setCount(BigDecimal count) {
            this.count = count;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }
    }

}
