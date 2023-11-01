package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.command.basic1000.GrepCommand;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.handlers.GrepHitLine;
import com.taobao.arthas.core.shell.handlers.MyLineParser;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author beiwei30 on 12/12/2016.
 */
public class GrepHandler extends StdoutHandler implements StatisticsFunction {
    public static final String NAME = "grep";

    private String keyword;
    private boolean ignoreCase;
    /**
     * select non-matching lines
     */
    private final boolean invertMatch;

    private final Pattern pattern;

    /**
     * print line number with output lines
     */
    private final boolean showLineNumber;

    private boolean trimEnd;

    /**
     * print NUM lines of leading context
     */
    private final Integer beforeLines;
    /**
     * print NUM lines of trailing context
     */
    private final Integer afterLines;

    /**
     * stop after NUM selected lines
     */
    private final Integer maxCount;

    private static CLI cli = null;

    /**
     * 行缓存
     */
    private GrepHitLine grepHitLine;

    private MyLineParser myLineParser;

    public static final String LINE_SPLIT="\n";

    public static StdoutHandler inject(List<CliToken> tokens) {
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);

        GrepCommand grepCommand = new GrepCommand();
        if (cli == null) {
            cli = CLIConfigurator.define(GrepCommand.class);
        }
        CommandLine commandLine = cli.parse(args, true);

        try {
            CLIConfigurator.inject(commandLine, grepCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        int beforeLines = grepCommand.getBeforeLines();
        int afterLines = grepCommand.getAfterLines();

        return new GrepHandler(grepCommand.getPattern(), grepCommand.isIgnoreCase(), grepCommand.isInvertMatch(),
                        grepCommand.isRegEx(), grepCommand.isShowLineNumber(), grepCommand.isTrimEnd(), beforeLines,
                        afterLines, grepCommand.getMaxCount());
    }

    public GrepHandler(String keyword, boolean ignoreCase, boolean invertMatch, boolean regexpMode,
                    boolean showLineNumber, boolean trimEnd, int beforeLines, int afterLines, int maxCount) {
        this.ignoreCase = ignoreCase;
        this.invertMatch = invertMatch;
        this.showLineNumber = showLineNumber;
        this.trimEnd = trimEnd;
        this.beforeLines = Math.max(beforeLines, 0);
        this.afterLines = Math.max(afterLines, 0);
        this.maxCount = Math.max(maxCount, 0);
        if (regexpMode) {
            final int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
            this.pattern = Pattern.compile(keyword, flags);
        } else {
            this.pattern = null;
        }
        this.keyword = ignoreCase ? keyword.toLowerCase() : keyword;
        this.grepHitLine = new GrepHitLine(true, this.beforeLines, this.afterLines);
        this.myLineParser = new MyLineParser();
    }


    public String apply(String input) {
        return doApply(input,false);
    }

    private String doApply(String input, boolean end) {
        StringBuilder output = new StringBuilder();
        List<String> lines;
        if (end) {
            String line = this.myLineParser.end();
            if (StringUtils.isEmpty(line)) {
                return null;
            }
            lines = new ArrayList<>();
            lines.add(line);
        } else {
            lines = this.myLineParser.handle(input);
        }
        for (String line : lines) {
            String match = match(line);
            if (match != null) {
                output.append(match);
            }
        }
        if (output.length()==0) {
            return null;
        }
        return output.toString();
    }


    private String match(String line) {
        if (LINE_SPLIT.equals(line)) {
            return null;
        }
        String re = null;
        boolean match;
        if (pattern == null) {
            match = (ignoreCase ? line.toLowerCase() : line).contains(keyword);
            if (match && !invertMatch) {
                String replaceStr = "\033[31m" + keyword + "\033[0m";
                line = line.replace(keyword, replaceStr);
            }
        } else {
            match = pattern.matcher(line).find();
            if (match && !invertMatch) {
                String replaceStr = "\033[31m$0\033[0m";
                line = line.replaceAll(pattern.pattern(), replaceStr);
            }
        }
        if (invertMatch != match) {
            boolean b = grepHitLine.setHit(line);
            if (!b) {
                String s = grepHitLine.toString();
                if (s!=null) {
                    re = grepHitLine.toString();
                }
                grepHitLine = new GrepHitLine(line, this.afterLines);
            }
        } else {
            boolean add = grepHitLine.add(line);
            if (add) {
                String s = grepHitLine.toString();
                if (s!=null) {
                    re = grepHitLine.toString();
                }
                grepHitLine = new GrepHitLine(this.beforeLines, this.afterLines);
            }
        }
        return re;
    }

    @Override
    public String result() {
        String result = "";
        String apply = doApply(null, true);
        if (apply != null) {
            result = apply;
        }
        boolean hasHitLine = grepHitLine.hasHitLine();
        if (hasHitLine) {
            return result + grepHitLine.toString() + "\n\033[33m(--grep end--)\033[0m\n";
        }
        return "\n\033[33m(--grep end--)\033[0m\n";
    }
}
