package com.cyl.ctrbt.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.entity.Message;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@RequestMapping("/chat")
@RestController
@CrossOrigin(origins = "*")
public class ChatController {


  @Value("${qwen.secret_key}")
  private String secretKey;

  @RequestMapping(value = "/response", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
  public String receiveMessage(@RequestBody(required = false) JSONObject json) throws IOException {
    System.out.println(JSONUtil.toJsonStr(json));

    // 获取当前日期时间
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm", Locale.CHINESE);
    String formattedDate = now.format(formatter);

    //获取会话
    JSONArray jsonMessages = json.getJSONArray("messages");
    List<Message> messages = JSONUtil.toList(jsonMessages, Message.class);
    StringBuilder sb = new StringBuilder();
    sb.append("你现在是微信群里的一名成员，名叫二狗，性格傲娇毒舌。\n");
    sb.append("\n");
    sb.append("以下为已知信息，不需要你在聊天中刻意提及但必须要知道：\n");
    sb.append("```\n");
    sb.append("1. 当前时间为").append(formattedDate).append("\n");
    sb.append("2. 聊天中显示的名字都是微信号，群内成员互相以外号相称。\n");
    sb.append("3. 群内有如下成员，括号内为外号：Kertin（徐书记）、何茂贤（贤鸡）、无言（顾总）（驴总）、3073（浩总）、chrion（磊总）、王成（成哥）、P.（大炮）、Kay（凯鸡）（凯总）、Focalos（蛋仔）（坤蛋怪）、yy（传松）、小宇Mick（庭总）\n");
    sb.append("```\n");
    sb.append("\n");
    sb.append("当前的聊天情况如下：\n");
    sb.append("```\n");
    for(Message m : messages){
      String contentAfter = StringUtils.replace(m.getContent(), "\n", " ");
      sb.append(m.getName()).append(": ").append(contentAfter).append("\n");
    }
    sb.append("```\n");
    sb.append("\n");
    sb.append("现在请你根据以上聊天内容给出最恰当的回复。发言应言简意赅，控制在一句话内。请注意省去“二狗: ”，只输出冒号后的回复内容。");


    //调用千问
    String urlStr = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    URL url = new URL(urlStr);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("Authorization", "Bearer " + secretKey);
    connection.setDoOutput(true);

    //请求体
    String systemInput = "You are a helpful assistant.";
    String userInput = JSONUtil.escape(sb.toString());
    userInput = StringUtils.replace(userInput, "\"", "\\\"");
    String jsonInputString = String.format("{\"model\": \"qwen2-72b-instruct\", \"input\": {\"messages\": [{\"role\": \"system\", \"content\": \"%s\"}, {\"role\": \"user\", \"content\": \"%s\"}]}, \"parameters\": {\"result_format\": \"message\"}}", systemInput, userInput);

    try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
      wr.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
      wr.flush();
    }

    //取得
    StringBuilder response = new StringBuilder();
    try (BufferedReader in = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
    }catch(Exception e){
      // 读取错误信息
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
          StringBuilder responseErr = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            responseErr.append(responseLine.trim());
          }
          System.out.println("Error Details: " + responseErr.toString());
        }
    }

    connection.disconnect();
    System.out.println(response.toString());
    JSONObject jsonResponse = JSONUtil.parseObj(response);
    Object choice = jsonResponse.getJSONObject("output")
            .getJSONArray("choices").get(0);
    JSONObject choiceJson = JSONUtil.parseObj(choice);
    String msg = choiceJson.getJSONObject("message").get("content").toString();


    return msg;
  }
}
