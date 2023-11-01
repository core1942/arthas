package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.model.CatModel;
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
import java.io.IOException;

@Name("cd")
@Summary("cd command")
public class CdCommand extends AnnotatedCommand {

    private static final String HOME_PATH = System.getProperty("user.dir");


    private static final String HOME = "/";
    private static final String HOME2 = "-";
    private static final String ROOT_PATH = ":\\";
    private static final String ROOT_PATH2 = ":/";

    private String path;

    @Argument(argName = "path", index = 0)
    @Description("path")
    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static void main(String[] args) throws IOException {
        String path = "C:\\Users\\Art\\Desktop\\s\\..\\arthas";
        File dir= new File(path);
        System.out.println(dir.isDirectory());
        System.out.println(dir.getPath());
        System.out.println(dir.getAbsolutePath());
        System.out.println(dir.getCanonicalPath());
    }

    @Override
    public void process(CommandProcess process) {
        String nowPath = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);

        if (HOME2.equals(path) || HOME.equals(path)) {
            System.setProperty(ArthasBootstrap.ARTHAS_USER_DIR, HOME_PATH);
            process.appendResult(new CatModel(null, HOME_PATH + "\n"));
            process.end();
            return;
        }
        File dir;
        if (path.contains(ROOT_PATH) || path.contains(ROOT_PATH2)) {
            dir = new File(path);
        } else {
            dir = new File(nowPath + HOME + path);
        }
        if (!dir.isDirectory()) {
            process.end(-1, path + ": No such directory");
        } else {
            try {
                String canonicalPath = dir.getCanonicalPath();
                System.setProperty(ArthasBootstrap.ARTHAS_USER_DIR, canonicalPath);
                process.appendResult(new CatModel(null, canonicalPath + "\n"));
                process.end();
            } catch (IOException e) {
                process.end(-1, e.getMessage());
            }
        }

    }


    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

}
