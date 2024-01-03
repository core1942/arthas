package com.taobao.arthas.core.command.basic1000;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.CatModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.middleware.cli.annotations.*;
import org.apache.commons.text.StringEscapeUtils;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Name("cat")
@Summary("Concatenate and print files")
public class CatCommand extends AnnotatedCommand {
    private volatile boolean interrupt = false;
    private volatile boolean close = false;
    private static final Logger logger = LoggerFactory.getLogger(CatCommand.class);
    private List<String> files;
    private String encoding;
    private Integer sizeLimit = 128 * 1024;
    private int maxSizeLimit = 8 * 1024 * 1024;

    /**
     * print line number with output lines
     */
    private boolean showLineNumber = false;

    @Argument(argName = "files", index = 0)
    @Description("files")
    public void setFiles(List<String> files) {
        this.files = files.stream().map(StringEscapeUtils::unescapeJava).collect(Collectors.toList());
    }

    @Option(longName = "encoding")
    @Description("File encoding")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (128 * 1024 by default, the maximum value is 8 * 1024 * 1024)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Option(shortName = "n", longName = "line-number", flag = true)
    @Description("Print line number with output lines")
    public void setShowLineNumber(boolean showLineNumber) {
        this.showLineNumber = showLineNumber;
    }

    @Override
    public void process(CommandProcess process) {

        for (String file : files) {
            file = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR) + "/" + file;
            File f = new File(file);
            if (!f.exists()) {
                process.end(-1, "cat " + file + ": No such file or directory");
                return;
            }
            if (f.isDirectory()) {
                process.end(-1, "cat " + file + ": Is a directory");
                return;
            }
        }

        process.interruptHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                interrupt = true;
            }
        });
        process.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                close = true;
            }
        });

        for (String file : files) {
            file = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR) + "/" + file;
            if (!interrupt && !close) {
                BufferedReader reader =null;
                try {
                    AtomicLong lineNum = new AtomicLong(1);
                    reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(file)), encoding == null ? Charset.defaultCharset() : Charset.forName(encoding)));
                    String str;
                    CharBuffer charBuffer = CharBuffer.allocate(4096);
                    boolean firstLine = true;
                    while (!interrupt && !close && reader.read(charBuffer) != -1) {
                        charBuffer.flip();
                        str = this.showLineNumber ? replacementInfo(charBuffer,lineNum,firstLine) : charBuffer.toString();
                        charBuffer.clear();
                        process.appendResult(new CatModel(file, str));
                        firstLine = false;
                    }
                } catch (IOException e) {
                    logger.error("cat read file error. name: " + file, e);
                    process.end(1, "cat read file error: " + e.getMessage());
                    return;
                }finally {
                    if (reader!=null) {
                        try {
                            reader.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
        logger.info("cat read file end  close:{} interrupt:{}", close, interrupt);
        if (interrupt) {
            process.end(1, "cat read file end with Ctrl+C");
        } else {
            process.end();
        }

    }

    public String replacementInfo(CharBuffer charBuffer, AtomicLong atomicLong,boolean firstLine) {
        StringBuilder stringBuilder = new StringBuilder(charBuffer);
        if (firstLine) {
            stringBuilder.insert(0, "\033[33m" + atomicLong.getAndAdd(1) + ": \033[0m");
        }
        int index = stringBuilder.indexOf("\n");
        while (index != -1) {
            stringBuilder.insert(index + 1, "\033[33m" + atomicLong.getAndAdd(1) + ": \033[0m");
            index = stringBuilder.indexOf("\n", index+4);
        }
        return stringBuilder.toString();
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("D:\\IdeaProjects\\arthas\\test.log")));
        CharBuffer charBuffer = CharBuffer.allocate(1024);
        while ( reader.read(charBuffer) != -1) {
            charBuffer.flip();
            String str = charBuffer.toString();
            charBuffer.clear();
            System.out.println(str);
        }
    }


    private boolean verifyOptions(CommandProcess process) {
        if (sizeLimit > maxSizeLimit) {
            process.end(-1, "sizeLimit cannot be large than: " + maxSizeLimit);
            return false;
        }

        //目前不支持过滤，限制http请求执行的文件大小
        int maxSizeLimitOfNonTty = 128 * 1024;
        if (!process.session().isTty() && sizeLimit > maxSizeLimitOfNonTty) {
            process.end(-1, "When executing in non-tty session, sizeLimit cannot be large than: " + maxSizeLimitOfNonTty);
            return false;
        }
        return true;
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

}
