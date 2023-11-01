package com.taobao.arthas.core.shell.handlers;

import java.util.LinkedList;
import java.util.List;

/**
 * create by WG on 2022/10/31 15:24
 *
 * @author WangGang
 */
public class MyLineParser {
    private final static String EMPTY = "";

    private String buff = EMPTY;


    public List<String> handle(String buffer) {
        if (buff.length() == 0) {
            buff = buffer;
        } else {
            buff += buffer;
        }
        return handleParsing();
    }




    public String end() {
        if (buff.length() > 0) {
            String re = buff;
            buff = EMPTY;
            return re;
        }
        return null;
    }

    private List<String> handleParsing() {
        int start = 0;
        int i;
        List<String> list = new LinkedList<>();
        while ((i = buff.indexOf('\n', start)) != -1) {
            int end = i + 1;
            String line = buff.substring(start, end);
            list.add(line);
            start = end;
        }
        if (buff.length() > 0) {
            buff = buff.substring(start);
        } else {
            buff = EMPTY;
        }
        return list;
    }

}
