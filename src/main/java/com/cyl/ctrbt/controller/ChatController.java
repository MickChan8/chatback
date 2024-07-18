package com.cyl.ctrbt.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.entity.Message;
import com.cyl.ctrbt.service.QwenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RequestMapping("/chat")
@RestController
@CrossOrigin(origins = "*")
public class ChatController {

  @Autowired
  public QwenService qwenService;

  @RequestMapping(value = "/jiegua", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
  public String receiveMessage(@RequestBody(required = false) String guawen) throws IOException {

    System.out.println("------guawen------:");
    System.out.println(guawen);
    System.out.println("\n");


    return qwenService.jiegua(guawen);
  }

  @RequestMapping(value = "/response", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
  public String receiveMessage(@RequestBody(required = false) JSONObject json) throws IOException {

    System.out.println("------Conversation------:");
    System.out.println(JSONUtil.toJsonStr(json));
    System.out.println("\n");

    //获取会话
    JSONArray jsonMessages = json.getJSONArray("messages");
    List<Message> messages = JSONUtil.toList(jsonMessages, Message.class);

    return qwenService.chat(messages);
  }
}

