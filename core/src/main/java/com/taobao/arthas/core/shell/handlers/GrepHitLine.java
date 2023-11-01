package com.taobao.arthas.core.shell.handlers;

import java.util.ArrayList;
import java.util.List;

/**
 * create by WG on 2022/5/20 16:17
 *
 * @author WangGang
 */
public class GrepHitLine {
    private int before, after;
    private CircularFifoQueue<String> beforeLine;
    private volatile String hitLine ;
    private List<String> afterLine;
    private boolean flag;
    private boolean begin;

    public GrepHitLine(int before, int after) {
        this(false, before, after);
    }
    public GrepHitLine(boolean begin, int before, int after) {
        this.begin = begin;
        this.before = before;
        this.after = after;
        if (before > 0) {
            beforeLine = new CircularFifoQueue<>(before);
        }
        if (after > 0) {
            afterLine = new ArrayList<>(after);
        }
    }

    public GrepHitLine(String hitLine, int after) {
        this.hitLine = hitLine;
        if (after > 0) {
            this.after = after;
            afterLine = new ArrayList<>(after);
        }
    }

    public boolean add(String line) {
        if (hitLine == null ) {
            if (beforeLine != null) {
                if (beforeLine.isFull()) {
                    flag = true;
                }
                beforeLine.add(line);
            } else {
                if (afterLine != null) {
                    flag = true;
                }
            }
        } else {
            if (afterLine != null) {
                afterLine.add(line);
                return afterLine.size() == after;
            }
            return true;
        }
        return false;
    }

    public boolean setHit(String hitLine) {
        if (this.hitLine == null) {
            this.hitLine = hitLine;
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (flag && !begin) {
            stringBuilder.append("--------------------------------------------------------\n");
        }
        if (before > 0) {
            for (String s : beforeLine) {
                stringBuilder.append(s);
            }
        }
        if (hitLine!=null) {
            stringBuilder.append(hitLine);
        }
        if (after > 0) {
            for (String s : afterLine) {
                stringBuilder.append(s);
            }
        }
        if (stringBuilder.length()==0) {
            return null;
        }
        return stringBuilder.toString();
    }

    public boolean hasHitLine() {
        return hitLine != null;
    }
}
