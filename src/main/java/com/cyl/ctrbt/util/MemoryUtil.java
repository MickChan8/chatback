package com.cyl.ctrbt.util;

import com.cyl.ctrbt.entity.Memory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MemoryUtil {

    /* 初始记忆强度，R阈值为0.05则约1天后遗忘 */
    private final static double INIT_STRENGTH = 0.4;

    /* 记忆强度增长因子，记忆10次后约7天后遗忘 */
    private final static double STRENGTH_FACTOR = 1.69;

    /* 遗忘R阈值 */
    private final static double MIN_RETENTION = 0.4;

    /* 遗忘S阈值 */
    private final static double MIN_STRENGTH = 0.1;

    /* 转长期S阈值 */
    public final static double TURN_STRENGTH = 4;

    /* 最大记忆数 */
    private final static int MAX_COUNT = 50;

    /* 记忆竞争衰减因子，值越大，记忆新东西的时候旧记忆遗忘得越快 */
    private final static double ATTENUATION_FACTOR = 0.1;

    /* 衰减阈值，记忆数大于此阈值则开启由竞争造成的衰减 */
    public final static int THRESHOLD_COUNT = (int) (MAX_COUNT*0.8);

    public static void createMemory(ArrayList<Memory> memories, String memoryContent, boolean ifAttenuation){

        // 记忆竞争导致衰减
        if(ifAttenuation){
            for (Memory memory : memories){
                memory.setStrength(memory.getStrength() - (memory.getStrength()-INIT_STRENGTH)*ATTENUATION_FACTOR );
            }
        }
        Memory newMemory = new Memory(memoryContent, INIT_STRENGTH, Instant.now());
        memories.add(newMemory);
    }

    public static void enhanceMemory(ArrayList<Memory> memories, Integer id){

        int index = id - 1;
        Memory memory = memories.get(index);

        memory.setStrength(memory.getStrength()*STRENGTH_FACTOR);
        memory.setCreationTime(Instant.now());
        memories.set(index, memory);
    }

    public static void deleteMemory(ArrayList<Memory> memories, Integer id){

        int index = id - 1;
        Memory memory = memories.get(index);

        // S在0.6以下则直接遗忘
        if(memory.getStrength() <= 0.6)
            memory.setStrength(memory.getStrength()*0.1);

        //  S在0.6~转长期S阈值则削弱
        else if (memory.getStrength() > 0.6 && memory.getStrength() <= TURN_STRENGTH)
            memory.setStrength(memory.getStrength()/STRENGTH_FACTOR);

        memories.set(index, memory);
    }

    public static ArrayList<Memory> clearMemory(ArrayList<Memory> memories){

        return (ArrayList<Memory>)memories.stream()
                .filter(memory -> calculateRetention(memory)>=MIN_RETENTION)
                .filter(memory -> memory.getStrength()>=MIN_STRENGTH)
                .filter(memory -> !memory.getProposition().isEmpty())
                .sorted()
                .limit(MAX_COUNT)
                .collect(Collectors.toList());
    }

    public static Double calculateRetention(Memory memory){
        Instant now = Instant.now();

        // 计算流逝天数
        long days = Duration.between(memory.getCreationTime(), now).get(ChronoUnit.SECONDS) / 86400;

        // 计算记忆保留率
        return Math.exp(-days / memory.getStrength());
    }
}
