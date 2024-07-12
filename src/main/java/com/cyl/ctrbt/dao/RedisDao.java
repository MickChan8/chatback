package com.cyl.ctrbt.dao;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.entity.MemoryAction;
import com.cyl.ctrbt.entity.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.List;

@Component
public class RedisDao {

    @Value("${redis.server}")
    private String redis;

    public void setString(String key, String value){

        try(Jedis jedis = new Jedis(redis, 6379)){
            if (!"PONG".equals(jedis.ping())) {
                System.out.println("Failed to connect to Redis");
            }
            jedis.set(key, value);
        }
    }

    public String getString(String key){

        String value;
        try(Jedis jedis = new Jedis(redis, 6379)){
            if (!"PONG".equals(jedis.ping())) {
                System.out.println("Failed to connect to Redis");
            }
            value = jedis.get(key);
        }
        return value;
    }

    public void setList(String key, List value){

        String strValue = JSONUtil.toJsonStr(value);
        this.setString(key, strValue);
    }

    public List getList(String key, Class listClass){

        List value;
        String strValue = this.getString(key);
        value = JSONUtil.toList(strValue, listClass);
        return value;
    }

    public void del(String key){

        try(Jedis jedis = new Jedis(redis, 6379)){
            if (!"PONG".equals(jedis.ping())) {
                System.out.println("Failed to connect to Redis");
            }
            jedis.del(key);
        }
    }
}
