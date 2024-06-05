package com.taobao.arthas.core;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.arthas.core.config.Configure;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CLIs;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.TypedOption;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Properties;

/**
 * Arthas启动器
 */
public class Arthas {

    private Arthas(String[] args) throws Exception {
        attachAgent(parse(args));
    }

    private Configure parse(String[] args) {
        Option pid = new TypedOption<Long>().setType(Long.class).setShortName("pid").setRequired(true);
        Option core = new TypedOption<String>().setType(String.class).setShortName("core").setRequired(true);
        Option agent = new TypedOption<String>().setType(String.class).setShortName("agent").setRequired(true);
        Option target = new TypedOption<String>().setType(String.class).setShortName("target-ip");
        Option telnetPort = new TypedOption<Integer>().setType(Integer.class)
                .setShortName("telnet-port");
        Option httpPort = new TypedOption<Integer>().setType(Integer.class)
                .setShortName("http-port");
        Option sessionTimeout = new TypedOption<Integer>().setType(Integer.class)
                        .setShortName("session-timeout");

        Option username = new TypedOption<String>().setType(String.class).setShortName("username");
        Option password = new TypedOption<String>().setType(String.class).setShortName("password");

        Option tunnelServer = new TypedOption<String>().setType(String.class).setShortName("tunnel-server");
        Option agentId = new TypedOption<String>().setType(String.class).setShortName("agent-id");
        Option appName = new TypedOption<String>().setType(String.class).setShortName(ArthasConstants.APP_NAME);
        Option sellerId = new TypedOption<String>().setType(String.class).setShortName("seller-id");
        Option sellerName = new TypedOption<String>().setType(String.class).setShortName("seller-name");
        Option storeId = new TypedOption<String>().setType(String.class).setShortName("store-id");
        Option storeName = new TypedOption<String>().setType(String.class).setShortName("store-name");
        Option appType = new TypedOption<String>().setType(String.class).setShortName("app-type");
        Option appVersion = new TypedOption<String>().setType(String.class).setShortName("app-version");
        Option localIp = new TypedOption<String>().setType(String.class).setShortName("local-ip");
        Option macAddr = new TypedOption<String>().setType(String.class).setShortName("mac-addr");

        Option statUrl = new TypedOption<String>().setType(String.class).setShortName("stat-url");
        Option disabledCommands = new TypedOption<String>().setType(String.class).setShortName("disabled-commands");

        CLI cli = CLIs.create("arthas").addOption(pid).addOption(core).addOption(agent).addOption(target)
                .addOption(telnetPort).addOption(httpPort).addOption(sessionTimeout)
                .addOption(username).addOption(password)
                .addOption(sellerId).addOption(sellerName).addOption(storeId).addOption(storeName).addOption(appType).addOption(appVersion).addOption(localIp).addOption(macAddr)
                .addOption(tunnelServer).addOption(agentId).addOption(appName).addOption(statUrl).addOption(disabledCommands);
        CommandLine commandLine = cli.parse(Arrays.asList(args));

        Configure configure = new Configure();
        configure.setJavaPid((Long) commandLine.getOptionValue("pid"));
        configure.setArthasAgent((String) commandLine.getOptionValue("agent"));
        configure.setArthasCore((String) commandLine.getOptionValue("core"));
        if (commandLine.getOptionValue("session-timeout") != null) {
            configure.setSessionTimeout((Integer) commandLine.getOptionValue("session-timeout"));
        }

        if (commandLine.getOptionValue("target-ip") != null) {
            configure.setIp((String) commandLine.getOptionValue("target-ip"));
        }

        if (commandLine.getOptionValue("telnet-port") != null) {
            configure.setTelnetPort((Integer) commandLine.getOptionValue("telnet-port"));
        }
        if (commandLine.getOptionValue("http-port") != null) {
            configure.setHttpPort((Integer) commandLine.getOptionValue("http-port"));
        }

        configure.setUsername((String) commandLine.getOptionValue("username"));
        configure.setPassword((String) commandLine.getOptionValue("password"));

        configure.setTunnelServer((String) commandLine.getOptionValue("tunnel-server"));
        configure.setAgentId((String) commandLine.getOptionValue("agent-id"));
        configure.setStatUrl((String) commandLine.getOptionValue("stat-url"));
        configure.setDisabledCommands((String) commandLine.getOptionValue("disabled-commands"));
        configure.setAppName((String) commandLine.getOptionValue(ArthasConstants.APP_NAME));
        configure.setSellerId((String) commandLine.getOptionValue("seller-id"));
        configure.setSellerName((String) commandLine.getOptionValue("seller-name"));
        configure.setStoreId((String) commandLine.getOptionValue("store-id"));
        configure.setStoreName((String) commandLine.getOptionValue("store-name"));
        configure.setAppType((String) commandLine.getOptionValue("app-type"));
        configure.setAppVersion((String) commandLine.getOptionValue("app-version"));
        configure.setLocalIp((String) commandLine.getOptionValue("local-ip"));
        configure.setMacAddr((String) commandLine.getOptionValue("mac-addr"));
        return configure;
    }

    private void attachAgent(Configure configure) throws Exception {
        VirtualMachineDescriptor virtualMachineDescriptor = null;
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String pid = descriptor.id();
            if (pid.equals(Long.toString(configure.getJavaPid()))) {
                virtualMachineDescriptor = descriptor;
                break;
            }
        }
        VirtualMachine virtualMachine = null;
        try {
            if (null == virtualMachineDescriptor) { // 使用 attach(String pid) 这种方式
                virtualMachine = VirtualMachine.attach("" + configure.getJavaPid());
            } else {
                virtualMachine = VirtualMachine.attach(virtualMachineDescriptor);
            }

            Properties targetSystemProperties = virtualMachine.getSystemProperties();
            String targetJavaVersion = JavaVersionUtils.javaVersionStr(targetSystemProperties);
            String currentJavaVersion = JavaVersionUtils.javaVersionStr();
            if (targetJavaVersion != null && currentJavaVersion != null) {
                if (!targetJavaVersion.equals(currentJavaVersion)) {
                    AnsiLog.warn("Current VM java version: {} do not match target VM java version: {}, attach may fail.",
                                    currentJavaVersion, targetJavaVersion);
                    AnsiLog.warn("Target VM JAVA_HOME is {}, arthas-boot JAVA_HOME is {}, try to set the same JAVA_HOME.",
                                    targetSystemProperties.getProperty("java.home"), System.getProperty("java.home"));
                }
            }

            String arthasAgentPath = configure.getArthasAgent();
            //convert jar path to unicode string
            configure.setArthasAgent(encodeArg(arthasAgentPath));
            configure.setArthasCore(encodeArg(configure.getArthasCore()));
            try {
                virtualMachine.loadAgent(arthasAgentPath,
                        configure.getArthasCore() + ";" + configure.toString());
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("Non-numeric value found")) {
                    AnsiLog.warn(e);
                    AnsiLog.warn("It seems to use the lower version of JDK to attach the higher version of JDK.");
                    AnsiLog.warn(
                            "This error message can be ignored, the attach may have been successful, and it will still try to connect.");
                } else {
                    throw e;
                }
            } catch (com.sun.tools.attach.AgentLoadException ex) {
                if ("0".equals(ex.getMessage())) {
                    // https://stackoverflow.com/a/54454418
                    AnsiLog.warn(ex);
                    AnsiLog.warn("It seems to use the higher version of JDK to attach the lower version of JDK.");
                    AnsiLog.warn(
                            "This error message can be ignored, the attach may have been successful, and it will still try to connect.");
                } else {
                    throw ex;
                }
            }
        } finally {
            if (null != virtualMachine) {
                virtualMachine.detach();
            }
        }
    }

    private static String encodeArg(String arg) {
        try {
            return URLEncoder.encode(arg, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return arg;
        }
    }

    public static void main(String[] args) {
        try {
            new Arthas(args);
        } catch (Throwable t) {
            AnsiLog.error("Start arthas failed, exception stack trace: ");
            t.printStackTrace();
            System.exit(-1);
        }
    }
}
