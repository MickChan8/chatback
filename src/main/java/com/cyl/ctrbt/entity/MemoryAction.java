package com.cyl.ctrbt.entity;

public class MemoryAction {
    /* 方法: update / enhance / create */
    private String method;

    /* ID */
    private Integer id;

    /* 命题 */
    private String proposition;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProposition() {
        return proposition;
    }

    public void setProposition(String proposition) {
        this.proposition = proposition;
    }

}
