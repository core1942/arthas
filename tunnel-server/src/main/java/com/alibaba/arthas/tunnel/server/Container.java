package com.alibaba.arthas.tunnel.server;

/**
 * create by WG on 2022/11/24 14:37
 *
 * @author WangGang
 */
public class Container<T> {
    private T obj;


    public Container(T obj) {
        this.obj = obj;
    }

    public T get() {
        return obj;
    }

    public void set(T obj) {
        this.obj = obj;
    }
}
