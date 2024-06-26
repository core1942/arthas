package com.taobao.arthas.core.command.basic1000;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.*;

import java.io.File;

@Name("sql")
@Summary("mysql connect")
public class SqlCommand extends AnnotatedCommand {
    private String encoding;
    private Integer port = 3307;

    @Argument(argName = "port", index = 0, required = false)
    @Description("port default：3307")
    public void setPath(Integer port) {
        this.port = port;
    }

    @Option(longName = "encoding")
    @Description("File encoding")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }


    public String getEncoding() {
        return encoding;
    }


    @Override
    public void process(final CommandProcess process) {
        try {
            String mysqlCommand;
            String arthasHome = System.getProperty("user.dir");
            File file = new File(arthasHome + "/../mysql/bin/mysql.exe");
            if (!file.exists()) {
                file = new File(arthasHome + "/../../mysql/bin/mysql.exe");
            }
            String canonicalPath = file.getCanonicalPath();
            if (file.exists()) {
                mysqlCommand = canonicalPath;
            } else {
                String nowPath = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);
                File file2 = new File(nowPath + "/mysql.exe");
                String canonicalPath2 = file2.getCanonicalPath();
                if (!file2.exists()) {
                    throw new RuntimeException(canonicalPath + " or " + canonicalPath2 + " :No such file!");
                }
                mysqlCommand = canonicalPath2;
            }
            String args = " -uroot -pqmai@2021 -P" + port + " -tn";
            String command = mysqlCommand + args;
            ExecCommand.runCmd(process, command, encoding);
        } catch (Exception e) {
            process.end(-1,e.getMessage());
        }
    }


    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

}
