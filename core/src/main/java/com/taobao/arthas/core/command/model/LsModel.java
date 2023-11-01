package com.taobao.arthas.core.command.model;

/**
 * Model of `heapdump` command
 * @author gongdewei 2020/4/24
 */
public class LsModel extends ResultModel {

    private String[] list;

    public String[] getList() {
        return list;
    }

    public void setList(String[] list) {
        this.list = list;
    }

    public LsModel() {
    }

    public LsModel(String[] list) {
        this.list = list;
    }

    @Override
    public String getType() {
        return "ls";
    }

}
