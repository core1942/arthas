package com.taobao.arthas.core.command.basic1000;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Name("rm")
@Summary("delete file")
public class RmCommand extends AnnotatedCommand {

    private static final Logger logger = LoggerFactory.getLogger(RmCommand.class);

    private String file;

    @Argument(argName = "file", index = 0)
    @Description("file")
    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public void process(CommandProcess process) {
        String currentPath = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);
        File f = new File(currentPath + "/" + file);
        if (f.delete()) {
            process.end(0, "删除成功\n");
        } else {
            process.end(1, "删除失败\n");
        }
    }




    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }


}
