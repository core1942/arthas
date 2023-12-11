package com.taobao.arthas.core.command.basic1000;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.*;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Name("exec")
@Summary("exec command")
public class ExecCommand extends AnnotatedCommand {
    private String command;

    private String encoding;

    private volatile boolean close = false;
    private static final byte CTRL_C = 0x03;
    private static final String LEFT_KEY = new String(new byte[]{27, 91, 68});


    @Argument(argName = "command", index = 0)
    @Description("command")
    public void setCommand(String command) {
        this.command = command;
    }

    @Option(longName = "encoding")
    @Description("File encoding")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getCommand() {
        return command;
    }

    public String getEncoding() {
        return encoding;
    }

    public static void main2(String[] args) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        CharBuffer charBuffer = CharBuffer.allocate(5);
        while (bufferedReader.read(charBuffer) != -1) {
            charBuffer.flip();
            String str = charBuffer.toString();
            charBuffer.clear();
            System.out.println(str);
        }
    }

    public static void main(String[] args) throws IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "C:/Users/Art/Desktop/s/mysql  -hshard01.mysql.db.zmcms.cn -udinnerdev -pdinnerdev2022 -tn");
        processBuilder.redirectErrorStream(true);
        final Process p = processBuilder.start();

        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream(), "GBK"));
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        new Thread(() -> {
            try {
                BufferedReader bufferedReader1 = new BufferedReader(new InputStreamReader(System.in));
                String line;
                System.out.println("请输入：");
                while ((line = bufferedReader1.readLine()) != null) {
                    bufferedWriter.write(line + "\n");
                    bufferedWriter.flush();
                    System.out.println("请输入：");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(() -> {
            try {
                CharBuffer charBuffer = CharBuffer.allocate(1024);
                while (bufferedReader.read(charBuffer) != -1) {
                    charBuffer.flip();
                    String str = charBuffer.toString();
                    charBuffer.clear();
                    System.out.println(str);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    public void process(final CommandProcess process) {
        if (command.startsWith("./")) {
            String nowPath = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);
            command = nowPath + "/" + command;
        }
        runCmd(process, command, encoding);
    }

    public static void runCmd(CommandProcess process, String command, String encoding) {
        try {
            String[] cmd = {"cmd.exe", "/c", command};
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("TERM", "xterm");
            PtyProcess pt = new PtyProcessBuilder()
                    .setRedirectErrorStream(true)
                    // .setConsole(true)
                    .setCommand(cmd)
                    .setInitialColumns(process.width())
                    .setInitialRows((int) (process.height()*0.9))
                    .setEnvironment(env)
                    .start();
            OutputStream os = pt.getOutputStream();
            InputStream is = pt.getInputStream();
            final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(os, encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding)));
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding)));
            Thread threadStdout = new Thread(() -> {
                try {
                    CharBuffer charBuffer = CharBuffer.allocate(1024);
                    while (bufferedReader.read(charBuffer) != -1) {
                        charBuffer.flip();
                        String str = charBuffer.toString();
                        charBuffer.clear();
                        process.write(str);
                    }
                } catch (Exception e) {
                    process.write(e.getMessage());
                } finally {
                    pt.destroy();
                    process.end();
                }
            });
            threadStdout.start();
            process.resizehandler(event -> pt.setWinSize(new WinSize(process.width(), (int) (process.height() * 0.9))));
            process.interruptHandler(event -> {
                try {
                    bufferedWriter.write(CTRL_C);
                } catch (Exception e) {
                    process.write(e.getMessage());
                }
            });
            process.endHandler(event -> {
                try {
                    pt.destroy();
                } finally {
                    pt.destroy();
                    process.end();
                }
            });
            process.stdinHandler(event -> {
                try {
                    bufferedWriter.write(event + "\n");
                    bufferedWriter.flush();
                } catch (Exception e) {
                    process.write(e.getMessage());
                }
            });
        } catch (Exception e) {
            process.end(-1, e.getMessage());
        }
    }


    // @Override
    // public void process(final CommandProcess process) {
    //
    //     try {
    //         if (command.startsWith("./")) {
    //             String nowPath = System.getProperty(ArthasBootstrap.ARTHAS_USER_DIR);
    //             command = nowPath + "/" + command;
    //         }
    //         // final ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
    //         final ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
    //         processBuilder.redirectErrorStream(true);
    //         final Process p = processBuilder.start();
    //         process.write("...> ");
    //         final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), encoding == null ? Charset.forName("GBK") : Charset.forName(encoding)));
    //         process.interruptHandler(new Handler<Void>() {
    //             @Override
    //             public void handle(Void event) {
    //                 try {
    //                     bufferedWriter.write(CTRL_C);
    //                 } catch (Exception e) {
    //                     process.write(e.getMessage());
    //                 }
    //             }
    //         });
    //         process.endHandler(new Handler<Void>() {
    //             @Override
    //             public void handle(Void event) {
    //                 try {
    //                     close = true;
    //                     p.destroy();
    //                 } finally {
    //                     process.end();
    //                 }
    //             }
    //         });
    //         process.stdinHandler(new Handler<String>() {
    //             List<String> list = new ArrayList<String>();
    //             @Override
    //             public void handle(String event) {
    //                 try {
    //                     list.add(event);
    //                     process.write(event);
    //                     if (event.contains("\n") || event.contains("\r")) {
    //                         String cmd = "";
    //                         for (String s : list) {
    //                             cmd += s;
    //                         }
    //                         bufferedWriter.write(cmd + "\n");
    //                         bufferedWriter.flush();
    //                         list.clear();
    //                         process.write("\n...> ");
    //                     }
    //                 } catch (Exception e) {
    //                     process.write(e.getMessage());
    //                 }
    //             }
    //         });
    //         final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream(), encoding == null ? Charset.forName("GBK") : Charset.forName(encoding)));
    //         new Thread(new Runnable(){
    //
    //             @Override
    //             public void run() {
    //                 try {
    //                     CharBuffer charBuffer = CharBuffer.allocate(1024);
    //                     while (!close && bufferedReader.read(charBuffer) != -1) {
    //                         charBuffer.flip();
    //                         String str = charBuffer.toString();
    //                         charBuffer.clear();
    //                         process.write(str);
    //                     }
    //                 } catch (Exception e) {
    //                     process.write(e.getMessage());
    //                 }finally {
    //                     process.end();
    //                 }
    //             }
    //         }).start();
    //
    //
    //     } catch (Exception e) {
    //         process.end(-1,e.getMessage());
    //     }
    // }


    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

}
