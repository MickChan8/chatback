package com.cyl.ctrbt.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /* 任务名称 */
    @Column(name = "task_name", length = 500)
    private String taskName;

    /* 任务结果 */
    @Column(name = "result", length = 10000)
    private String result;

    /* 是否报告完成 */
    @Column(name = "report_flg")
    private Boolean reportFlg = false;

    public Task(String taskName, String result){
        setTaskName(taskName);
        setResult(result);
    }

    public Task() {

    }

}