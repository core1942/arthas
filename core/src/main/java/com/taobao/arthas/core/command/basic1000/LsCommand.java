package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.model.LsModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Name("ls")
@Summary("ls command")
public class LsCommand extends AnnotatedCommand {

    @Override
    public void process(CommandProcess process) {
        String path = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (files != null) {
            List<LsModel.FileNode> fileNodes = Arrays.stream(files).map(file -> new LsModel.FileNode(file.isDirectory(), file.getName())).collect(Collectors.toList());
            process.appendResult(new LsModel(fileNodes));
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
