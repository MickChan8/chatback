package com.cyl.ctrbt.entity;

import com.cyl.ctrbt.util.MemoryUtil;
import lombok.Getter;
import lombok.Setter;


import javax.persistence.*;
import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "memory")
public class Memory implements Comparable<Memory>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /* 命题：记忆内容 */
    @Column(name = "proposition", length = 3000)
    private String proposition;

    /* 记忆强度：越大遗忘越慢 */
    @Column(name = "strength")
    private Double strength;

    /* 记忆类型：1：规则；2：长期；3：短期 */
    @Column(name = "memory_type")
    private Integer memoryType;

    /* 记忆创建时间 */
    @Column(name = "creation_time")
    private Instant creationTime;

    public Memory(String proposition, Double strength, Instant creationTime){
        setProposition(proposition);
        setStrength(strength);
        setCreationTime(creationTime);
        setMemoryType(3);
    }

    public Memory() {

    }

    @Override
    public int compareTo(Memory otherMemory) {
        if(this.getStrength() > otherMemory.getStrength()){
            return -1;
        }
        else if(this.getStrength() < otherMemory.getStrength()){
            return 1;
        }
        return 0;
    }

}