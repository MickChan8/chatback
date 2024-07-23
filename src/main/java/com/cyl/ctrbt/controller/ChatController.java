package com.cyl.ctrbt.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.constants.Constants;
import com.cyl.ctrbt.entity.Action;
import com.cyl.ctrbt.entity.BingNewsResult;
import com.cyl.ctrbt.entity.Message;
import com.cyl.ctrbt.entity.SearchResult;
import com.cyl.ctrbt.service.QwenService;
import com.cyl.ctrbt.service.SearchService;
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

  @Autowired
  public SearchService searchService;

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

    // 获取会话
    JSONArray jsonMessages = json.getJSONArray("messages");
    List<Message> messages = JSONUtil.toList(jsonMessages, Message.class);

    try{
      // 判断行动
      Action action = qwenService.chooseAction(messages);

      // 联网查询
      if(action.getMethod().equals("search")){

        // 先Bing API
        List<BingNewsResult> newsResults = searchService.searchBingNews(action.getQuery());
        if(newsResults.size()>0){
          String strResult = searchService.getPageText(newsResults.get(0).getUrl());
          return qwenService.chat(messages, Constants.CHAT_MODE_SEARCH, strResult);
        }

        // 再Bing
        List<SearchResult> results = searchService.searchBing(action.getQuery());
        if(results.size()>0){
          String strResult = searchService.getPageText(results.get(0).getUrl());
          return qwenService.chat(messages, Constants.CHAT_MODE_SEARCH, strResult);
        }

        // 后Baidu
        results = searchService.searchBaidu(action.getQuery());
        if(results.size()>0){
          String strResult = searchService.getPageText(results.get(0).getUrl());
          return qwenService.chat(messages, Constants.CHAT_MODE_SEARCH, strResult);
        }
      }
    }catch (Exception e){
      System.out.println("------Choosing action failed!!!------");
    }


    return qwenService.chat(messages, Constants.CHAT_MODE_CHAT, "");
  }

  @RequestMapping(value = "/information", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
  public String informationGathering(@RequestParam(required = false) String topic) throws IOException {

    System.out.println("------topic------:");
    System.out.println(topic);
    System.out.println("\n");


    return qwenService.gatherInformation(topic);
  }

}

