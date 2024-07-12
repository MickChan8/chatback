package com.cyl.ctrbt.controller;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.entity.MemoryAction;
import com.cyl.ctrbt.entity.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;

@RequestMapping("/test")
@RestController
@CrossOrigin(origins = "*")
public class TestController {


  @Value("${redis.server}")
  private String redis;

  @RequestMapping(value = "/response", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
  public String receiveMessage() throws IOException {

    Message msg1 = new Message();
    msg1.setContent("Hi");
    msg1.setName("oh");

    String content = ResourceUtil.readUtf8Str("classpath:prompt\\test.txt");
//    System.out.println(content);

    //String转list
    List<MemoryAction> list = JSONUtil.toList(content, MemoryAction.class);
//    System.out.println(list);

    //list转string
    String jstr = JSONUtil.toJsonStr(list);

    String jstr2;
    // 创建Jedis对象，默认连接本地的Redis服务器（默认端口6379）
    try(Jedis jedis = new Jedis(redis, 6379)){
      // 验证连接
      if ("PONG".equals(jedis.ping())) {
        System.out.println("Connected to Redis successfully");
      } else {
        System.out.println("Failed to connect to Redis");
      }

      jedis.set("test", jstr);
      jstr2 = jedis.get("test");
      List<MemoryAction> list2 = JSONUtil.toList(jstr2, MemoryAction.class);
      jedis.del("test");

    }








    String msg ="你好";


    return msg;
  }
}
