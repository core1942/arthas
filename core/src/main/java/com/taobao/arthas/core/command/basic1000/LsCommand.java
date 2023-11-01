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

@Name("ls")
@Summary("ls command")
public class LsCommand extends AnnotatedCommand {

    @Override
    public void process(CommandProcess process) {
        String path = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);
        File dir = new File(path);
        String[] list = dir.list();
        process.appendResult(new LsModel(list));
        process.end();
    }


    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

}
