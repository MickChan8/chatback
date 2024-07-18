package com.cyl.ctrbt.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MemoryAction {
    /* 方法: update / enhance / create */
    private String method;

    /* ID */
    private Integer id;

    /* 命题 */
    private String proposition;

}
