package com.cyl.ctrbt.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.dao.RedisDao;
import com.cyl.ctrbt.entity.Memory;
import com.cyl.ctrbt.entity.Message;
import com.cyl.ctrbt.service.QwenService;
import com.cyl.ctrbt.service.SearchService;
import com.cyl.ctrbt.service.WeatherService;
import com.cyl.ctrbt.util.MemoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@RequestMapping("/test")
@RestController
@CrossOrigin(origins = "*")
public class TestController {

  private final static String rulesMemoryKey = "gp1.rules.memories";

  @Value("${redis.server}")
  private String redis;

  @Autowired
  private QwenService qwenService;

  @Autowired
  private WeatherService weatherService;

  @Autowired
  private SearchService searchService;

  @Autowired
  private RedisDao redisDao;


  @RequestMapping(value = "/weather", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
  public String insertTest() throws IOException {



    return weatherService.getWeather();
  }



  @RequestMapping(value = "/analysememory", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
  public String analyseMemory(@RequestBody(required = false) JSONObject json) throws IOException {

    System.out.println("------Conversation------:");
    System.out.println(JSONUtil.toJsonStr(json));
    System.out.println("\n");

    //获取会话
    JSONArray jsonMessages = json.getJSONArray("messages");
    List<Message> messages = JSONUtil.toList(jsonMessages, Message.class);

    qwenService.analyseMemory(messages);
    return "OK";
  }

  // 插入规则记忆
  @RequestMapping(value = "/insert/rules", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
  public String insertRules() throws IOException {

    ArrayList<Memory> memories = new ArrayList<Memory>();
    MemoryUtil.createMemory(memories, "聊天中显示的名字都是微信号，群内成员互相以外号相称。", false);
    MemoryUtil.createMemory(memories, "群内有如下成员，括号内为外号：Kertin（徐书记）、何茂贤（贤鸡）、无言（顾总）（驴总）、3073（浩总）、chrion（磊总）、王成（成哥）、P.（大炮）、Kay（凯鸡）（凯总）、Focalos（蛋仔）（坤蛋怪）、yy（传松）、小宇Mick（庭总）", false);
    redisDao.setList(rulesMemoryKey, memories);

    return "OK";
  }


  // 测记忆保存率
  @RequestMapping(value = "/retention", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
  public String calculateRetention() throws IOException {

    LocalDateTime dateTime = LocalDateTime.of(2024, 7, 16, 20, 15, 30);
    Instant dateInstant = dateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant();

    Instant now = Instant.now();


    Memory memory = new Memory("test", 1.3, dateInstant);
    Double d = MemoryUtil.calculateRetention(memory);

    return d.toString();
  }





  @RequestMapping(value = "/response", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
  public String receiveMessage() throws IOException {

//  searchService.searchBingNews("特朗普 暗杀");

    String msg = searchService.getPageText("https://www.donga.com/cn/article/all/20240715/5062063/1");





    return msg;
  }






















}