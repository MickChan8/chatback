package com.cyl.ctrbt.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.dao.RedisDao;
import com.cyl.ctrbt.entity.Memory;
import com.cyl.ctrbt.entity.Message;

import com.cyl.ctrbt.repository.MemoryRepository;

import com.cyl.ctrbt.service.QwenService;
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

import org.apache.http.util.EntityUtils;




import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


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
  private RedisDao redisDao;

  @Autowired
  private MemoryRepository memoryRepository;

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


  private static void fetchPageContent(CloseableHttpClient httpClient, String url) {
    HttpGet pageRequest = new HttpGet(url);
    pageRequest.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

    try (CloseableHttpResponse pageResponse = httpClient.execute(pageRequest)) {
      String pageHtml = EntityUtils.toString(pageResponse.getEntity());
      Document pageDoc = Jsoup.parse(pageHtml, "UTF-8");
      // 根据需要，选择适当的元素进行解析
      Elements articleContent = pageDoc.select("div.article"); // 仅为示例，根据实际页面结构调整
      System.out.println("Content: " + articleContent.text());
    } catch (Exception e) {
      System.out.println("Failed to fetch page content for URL: " + url);
      e.printStackTrace();
    }
  }

  @RequestMapping(value = "/response", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
  public String receiveMessage() throws IOException {

    //百度
    String query = "Java 爬虫";
    String url = "https://www.baidu.com/s?wd=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

    // Initialize HttpClient
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet request = new HttpGet(url);
    request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

    // Execute the request
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        // Parse the response HTML using Jsoup
        Document doc = Jsoup.parse(entity.getContent(), "UTF-8", "");

        // Select the search result links
        Elements links = doc.select("h3.t a");
        for (Element link : links) {
          String linkText = link.text();
          String absoluteUrl = link.attr("abs:href");

          System.out.println("URL: " + absoluteUrl);
          System.out.println("Text: " + linkText);
          System.out.println();
          fetchPageContent(httpClient, absoluteUrl);
        }
      }


    }
    String msg = "你好";


    return msg;
  }
}