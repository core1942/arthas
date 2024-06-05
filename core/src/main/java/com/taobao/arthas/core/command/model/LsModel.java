package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * Model of `ls` command
 * @author gongdewei 2020/4/24
 */
public class LsModel extends ResultModel {

    private List<FileNode> list;

    public List<FileNode> getList() {
        return list;
    }

    public void setList(List<FileNode> list) {
        this.list = list;
    }

    public LsModel() {
    }

    public LsModel(List<FileNode> list) {
        this.list = list;
    }

    public static class FileNode{
        private boolean isDir;

        private String name;

        public FileNode(boolean isDir, String name) {
            this.isDir = isDir;
            this.name = name;
        }

        public boolean isDir() {
            return isDir;
        }

        public void setDir(boolean dir) {
            isDir = dir;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Override
    public String getType() {
        return "ls";
    }

}
