package com.taobao.arthas.core.command;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.basic1000.*;
import com.taobao.arthas.core.command.hidden.JulyCommand;
import com.taobao.arthas.core.command.hidden.ThanksCommand;
import com.taobao.arthas.core.command.klass100.*;
import com.taobao.arthas.core.command.logger.LoggerCommand;
import com.taobao.arthas.core.command.monitor200.*;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.middleware.cli.annotations.Name;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO automatically discover the built-in commands.
 * @author beiwei30 on 17/11/2016.
 */
public class BuiltinCommandPack implements CommandResolver {
    private static final Logger logger = LoggerFactory.getLogger(BuiltinCommandPack.class);
    private List<Command> commands = new ArrayList<Command>();

    public BuiltinCommandPack(List<String> disabledCommands) {
        initCommands(disabledCommands);
    }

    @Override
    public List<Command> commands() {
        return commands;
    }

    private void initCommands(List<String> disabledCommands) {
        List<Class<? extends AnnotatedCommand>> commandClassList = new ArrayList<Class<? extends AnnotatedCommand>>(33);
        commandClassList.add(HelpCommand.class);
        commandClassList.add(AuthCommand.class);
        commandClassList.add(KeymapCommand.class);
        commandClassList.add(SearchClassCommand.class);
        commandClassList.add(SearchMethodCommand.class);
        commandClassList.add(ClassLoaderCommand.class);
        commandClassList.add(JadCommand.class);
        commandClassList.add(GetStaticCommand.class);
        commandClassList.add(MonitorCommand.class);
        commandClassList.add(StackCommand.class);
        commandClassList.add(ThreadCommand.class);
        commandClassList.add(TraceCommand.class);
        commandClassList.add(WatchCommand.class);
        commandClassList.add(TimeTunnelCommand.class);
        commandClassList.add(JvmCommand.class);
        commandClassList.add(MemoryCommand.class);
        commandClassList.add(PerfCounterCommand.class);
        // commandClassList.add(GroovyScriptCommand.class);
        commandClassList.add(OgnlCommand.class);
        commandClassList.add(MemoryCompilerCommand.class);
        commandClassList.add(RedefineCommand.class);
        commandClassList.add(RetransformCommand.class);
        commandClassList.add(DashboardCommand.class);
        commandClassList.add(DumpClassCommand.class);
        commandClassList.add(HeapDumpCommand.class);
        commandClassList.add(JulyCommand.class);
        commandClassList.add(ThanksCommand.class);
        commandClassList.add(OptionsCommand.class);
        commandClassList.add(ClsCommand.class);
        commandClassList.add(ResetCommand.class);
        commandClassList.add(VersionCommand.class);
        commandClassList.add(SessionCommand.class);
        commandClassList.add(SystemPropertyCommand.class);
        commandClassList.add(SystemEnvCommand.class);
        commandClassList.add(VMOptionCommand.class);
        commandClassList.add(LoggerCommand.class);
        commandClassList.add(HistoryCommand.class);
        commandClassList.add(CatCommand.class);
        commandClassList.add(ExecCommand.class);
        commandClassList.add(LsCommand.class);
        commandClassList.add(SqlCommand.class);
        commandClassList.add(CdCommand.class);
        commandClassList.add(Base64Command.class);
        commandClassList.add(EchoCommand.class);
        commandClassList.add(PwdCommand.class);
        commandClassList.add(MBeanCommand.class);
        commandClassList.add(GrepCommand.class);
        commandClassList.add(TeeCommand.class);
        commandClassList.add(ProfilerCommand.class);
        commandClassList.add(SzCommand.class);
        commandClassList.add(RzCommand.class);
        commandClassList.add(RmCommand.class);
        commandClassList.add(MakedirCommand.class);
        // commandClassList.add(VmToolCommand.class);
        commandClassList.add(StopCommand.class);
        try {
            // if (ClassLoader.getSystemClassLoader().getResource("jdk/jfr/Recording.class") != null) {
            //     commandClassList.add(JFRCommand.class);
            // }
        } catch (Throwable e) {
            logger.error("This jdk version not support jfr command");
        }

        for (Class<? extends AnnotatedCommand> clazz : commandClassList) {
            Name name = clazz.getAnnotation(Name.class);
            if (name != null && name.value() != null) {
                if (disabledCommands.contains(name.value())) {
                    continue;
                }
            }
            commands.add(Command.create(clazz));
        }
    }
}
