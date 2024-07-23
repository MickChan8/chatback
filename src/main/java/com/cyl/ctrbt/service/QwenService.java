package com.cyl.ctrbt.service;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.constants.Constants;
import com.cyl.ctrbt.entity.*;
import com.cyl.ctrbt.repository.MemoryRepository;
import com.cyl.ctrbt.util.MemoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class QwenService {

    @Value("${qwen.secret_key}")
    private String secretKey;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private  SearchService searchService;

    @Autowired
    private MemoryRepository memoryRepository;

    private static boolean memoryFlg = true;

    // 调查
    public String gatherInformation(String topic) throws IOException {

        // 生成搜索关键词
        // 获取prompt
        String fileStrQuery = ResourceUtil.readUtf8Str("classpath:prompt\\InformationGathering\\Query.txt");
        // 处理拼接prompt
        fileStrQuery = StringUtils.replace(fileStrQuery, "#topic#", topic);
        // 发送请求
        String query = doRequest(fileStrQuery, Constants.QWEN15_110B, 0.1f);

        // 调用bing搜索信息
        List<BingNewsResult> newsResults = searchService.searchBingNews(query);
        if(newsResults.isEmpty()){
            return "调查主题“"+topic+"”搜索不到相关信息！";
        }

        // 提取网页内容
        ArrayList<String> pageText = new ArrayList<>();
        for(BingNewsResult result: newsResults){
            pageText.add(searchService.getPageText(result.getUrl()));
        }
        if (pageText.size() > 5) {
            pageText.subList(5, pageText.size()).clear();
        }

        // 生成调查报告
        ArrayList<String> reports = new ArrayList<>();
        for(String text: pageText){
            // 获取prompt
            String fileStrPage = ResourceUtil.readUtf8Str("classpath:prompt\\InformationGathering\\PageSummary.txt");
            // 处理拼接prompt
            fileStrPage = StringUtils.replace(fileStrPage, "#topic#", topic);
            fileStrPage = StringUtils.replace(fileStrPage, "#content#", text);
            // 发送请求
            reports.add(doRequest(fileStrPage, Constants.QWEN2_72B, 0.5f));
        }

        // 生成总结报告
        // 获取prompt
        String fileStrReport1 = ResourceUtil.readUtf8Str("classpath:prompt\\InformationGathering\\ReportSummary_1.txt");
        // 处理拼接prompt
        fileStrReport1 = StringUtils.replace(fileStrReport1, "#topic#", topic);
        String fileStrReport2 = "";
        for(int i = 0; i < reports.size(); i++){
            // 获取prompt
            String fileStrReport2_part = ResourceUtil.readUtf8Str("classpath:prompt\\InformationGathering\\ReportSummary_2.txt");
            // 处理拼接prompt
            fileStrReport2_part = StringUtils.replace(fileStrReport2_part, "#number#", String.valueOf(i+1));
            fileStrReport2_part = StringUtils.replace(fileStrReport2_part, "#report#", reports.get(i));
            fileStrReport2 = fileStrReport2 + fileStrReport2_part;

        }

//        System.out.println(fileStrReport1+fileStrReport2);

        return doRequest(fileStrReport1+fileStrReport2, Constants.QWEN2_72B, 0.5f);
    }

    // 解卦
    public String jiegua(String guawen) throws IOException {

        // 获取prompt
        String fileStr = ResourceUtil.readUtf8Str("classpath:prompt\\Jiegua.txt");

        // 处理拼接prompt
        fileStr = StringUtils.replace(fileStr, "#guawen#", guawen);

        // 发送请求
        String responseStr = doRequest(fileStr, Constants.QWEN15_32B, 0.3f);

        return responseStr;
    }

    // 聊天选择行动
    public Action chooseAction(List<Message> messages) throws IOException {

        // 获取当前日期时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm", Locale.CHINESE);
        String formattedDate = now.format(formatter);

        // 生成聊天记录
        String strConversation = getConversation(messages);

        // 获取prompt
        String fileStr = ResourceUtil.readUtf8Str("classpath:prompt\\Choose.txt");

        // 处理拼接prompt
        fileStr = StringUtils.replace(fileStr, "#time#", formattedDate);
        fileStr = StringUtils.replace(fileStr, "#conversation#", strConversation);
        fileStr = StringUtils.replace(fileStr, "#lastMessage#", messages.get(messages.size()-1).getContent());

        // 发送请求
        String jsonStrBefore = doRequest(fileStr, Constants.QWEN15_110B, 0.1f);
        jsonStrBefore = StringUtils.replace(jsonStrBefore, "\r\n", "\n");
        String jsonStrAfter = StringUtils.replace(StringUtils.replace(jsonStrBefore, "\n```", ""),
                "```json\n", "");

        return JSONUtil.toBean(jsonStrAfter, Action.class);
    }


    // 聊天
    public String chat(List<Message> messages, String chatMode, String shortMemory) throws IOException {

        // 获取长期记忆
        String strLongMemory = getLongMemories();

        // 短期记忆编辑
        String strShortMemory = "";
        // 聊天模式，则获取短期记忆正常聊天
        if(chatMode.equals(Constants.CHAT_MODE_CHAT)){

            // 获取短期记忆
            ArrayList<Memory> shortMemories =  new ArrayList<>();
            memoryRepository.findByMemoryType(3).forEach(shortMemories::add);
            if(shortMemories.size() == 0){
                MemoryUtil.createMemory(shortMemories, "二狗现在心情很不错，虽然依旧毒舌但乐于回答群友的问题", false);
            }
            // 生成短期记忆列表
            StringBuilder sbShortMemory = new StringBuilder();
            for(int i=0; i<shortMemories.size(); i++){
                sbShortMemory.append(i+1).append(". ").append(shortMemories.get(i).getProposition()).append("\n");
            }
            strShortMemory = sbShortMemory.toString();
            System.out.println("-----Short memories------:");
            System.out.println(strShortMemory);
            System.out.println("\n");

        // 搜索模式，则根据结果回答
        }else if(chatMode.equals(Constants.CHAT_MODE_SEARCH)) {

            strShortMemory = "二狗上网查了一下，以下为查询到的网页内容：···"+shortMemory+"···";
            System.out.println("-----Getting informations from the Internet------:");
            System.out.println("\n");
        }

        // 生成聊天记录
        String strConversation = getConversation(messages);

        // 获取prompt
        String fileStr = ResourceUtil.readUtf8Str("classpath:prompt\\Chat.txt");

        // 处理拼接prompt
        fileStr = StringUtils.replace(fileStr, "#longMemories#", strLongMemory);
        fileStr = StringUtils.replace(fileStr, "#conversation#", strConversation);
        fileStr = StringUtils.replace(fileStr, "#shortMemories#", strShortMemory);

        String responseStr = "";
        // 发送请求
        if(chatMode.equals(Constants.CHAT_MODE_SEARCH))
            responseStr = doRequest(fileStr, Constants.QWEN2_72B, -1f);
        else if(chatMode.equals(Constants.CHAT_MODE_CHAT))
            responseStr = doRequest(fileStr, Constants.QWEN15_110B, -1f);

        Message lastMessage = new Message();
        lastMessage.setName("二狗");
        lastMessage.setContent(responseStr);
        messages.add(lastMessage);

        // 后台处理记忆 ##两轮做一次记忆##
        if (memoryFlg) {
            Thread td = new Thread(() -> {
                try {
                    analyseMemory(messages);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            td.start();
        }
        memoryFlg = !memoryFlg;

        return responseStr;
    }

    // 生成记忆
    public synchronized void analyseMemory(List<Message> messages) throws IOException {

        // 获取长期记忆
        String strLongMemory = getLongMemories();

        // 获取短期记忆
        ArrayList<Memory> shortMemories =  new ArrayList<>();
        memoryRepository.findByMemoryType(3).forEach(shortMemories::add);
        if (shortMemories.size() == 0) {
            MemoryUtil.createMemory(shortMemories, "二狗现在心情很不错，虽然依旧毒舌但乐于回答群友的问题", false);
        }
        // 生成记忆列表
        StringBuilder sbShortMemory = new StringBuilder();
        for (int i = 0; i < shortMemories.size(); i++) {
            sbShortMemory.append(i + 1).append(". ").append(shortMemories.get(i).getProposition()).append("\n");
        }
        String strShortMemory = sbShortMemory.toString();
        System.out.println("------Short Memories------:");
        System.out.println(strShortMemory);
        System.out.println("\n");

        // 生成聊天记录
        String strConversation = getConversation(messages);

        // 获取prompt
        String fileStr = ResourceUtil.readUtf8Str("classpath:prompt\\AnalyseMemory.txt");

        // 处理拼接prompt
        fileStr = StringUtils.replace(fileStr, "#longMemories#", strLongMemory);
        fileStr = StringUtils.replace(fileStr, "#conversation#", strConversation);
        fileStr = StringUtils.replace(fileStr, "#shortMemories#", strShortMemory);

        // 发送请求，获取action
        String jsonStrBefore = doRequest(fileStr, Constants.QWEN15_110B, 0.1f);
        String jsonStrAfter = StringUtils.replace(StringUtils.replace(jsonStrBefore, "\n```", ""),
                "```json\n", "");
        List<MemoryAction> actions = JSONUtil.toList(jsonStrAfter, MemoryAction.class);

        // 记忆是否已衰减过
        boolean attenuation_flg = true;
        if(shortMemories.size()>=MemoryUtil.THRESHOLD_COUNT) {
            attenuation_flg = false;
        }
        // 处理action
        for (MemoryAction action : actions) {

            if (action.getMethod().equals("create")) {

                MemoryUtil.createMemory(shortMemories, action.getProposition(), !attenuation_flg);
                attenuation_flg = true;

            } else if (action.getMethod().equals("enhance")) {

                MemoryUtil.enhanceMemory(shortMemories, action.getId());

            } else if (action.getMethod().equals("delete")) {

                MemoryUtil.deleteMemory(shortMemories, action.getId());

            }
        }

        // 筛出入围的长期记忆
        ArrayList<Memory> newLongMemories = (ArrayList)shortMemories.stream()
            .filter( memory -> memory.getStrength() >= MemoryUtil.TURN_STRENGTH)
            .collect(Collectors.toList());
        System.out.println("------Long Memories to be turned------:");
        for (Memory memory : newLongMemories) {
            System.out.println("Content: " + memory.getProposition());
        }

        // 长期记忆转化
        if(!newLongMemories.isEmpty()){

            // 获取规则记忆
            ArrayList<Memory> rulesMemories = new ArrayList<Memory>();
            memoryRepository.findByMemoryType(1).forEach(rulesMemories::add);
            // 生成记忆列表
            StringBuilder sbRulesMemory = new StringBuilder();
            for(int i=0; i<rulesMemories.size(); i++){
                sbRulesMemory.append(i+1).append(". ").append(rulesMemories.get(i).getProposition()).append("\n");
            }
            String strRulesMemory = sbRulesMemory.toString();

            // 获取旧长期记忆
            Memory longMemories = memoryRepository.findOneByMemoryType(2);
            // 生成记忆列表
            String strOldLongMemory = longMemories.getProposition();

            // 获取新长期记忆
            // 生成记忆列表
            StringBuilder sbNewLongMemory = new StringBuilder();
            for(int i=0; i<newLongMemories.size(); i++){
                sbNewLongMemory.append(i+1).append(". ").append(newLongMemories.get(i).getProposition()).append("\n");
            }
            String strNewLongMemory = sbNewLongMemory.toString();

            // 获取prompt1，2
            String fileStr_step1 = ResourceUtil.readUtf8Str("classpath:prompt\\LongMemory_step1.txt");
            String fileStr_step2 = ResourceUtil.readUtf8Str("classpath:prompt\\LongMemory_step2.txt");

            // 处理拼接prompt1
            fileStr_step1 = StringUtils.replace(fileStr_step1, "#rulesMemories#", strRulesMemory);
            fileStr_step1 = StringUtils.replace(fileStr_step1, "#oldLongMemories#", strOldLongMemory);
            fileStr_step1 = StringUtils.replace(fileStr_step1, "#newLongMemories#", strNewLongMemory);
            // 发送请求，进行step1
            strNewLongMemory = doRequest(fileStr_step1, Constants.QWEN15_110B, 0.1f);

            // 处理拼接prompt2
            fileStr_step2 = StringUtils.replace(fileStr_step2, "#rulesMemories#", strRulesMemory);
            fileStr_step2 = StringUtils.replace(fileStr_step2, "#newLongMemories#", strNewLongMemory);
            // 发送请求，进行step2
            strNewLongMemory = doRequest(fileStr_step2, Constants.QWEN15_110B, 0.1f);

            longMemories.setProposition(strNewLongMemory);
            memoryRepository.save(longMemories);

            System.out.println("------Long Memories turned------:");
            System.out.println("Content: " + strNewLongMemory);
        }

        // 筛出新的短期记忆
        shortMemories = (ArrayList)shortMemories.stream()
            .filter( memory -> memory.getStrength() < MemoryUtil.TURN_STRENGTH)
            .collect(Collectors.toList());

        // 清理遗忘以及多余记忆
        shortMemories = MemoryUtil.clearMemory(shortMemories);

        //输出处理后的记忆
        System.out.println("------Updated Short Memories------:");
        for (Memory memory : shortMemories) {
            System.out.println("Time: " + memory.getCreationTime()
                    + "  Strength: " + memory.getStrength()
                    + "  Retention: " + MemoryUtil.calculateRetention(memory)
                    + "  Content: " + memory.getProposition());
        }

        // 处理完的记忆塞回db
        memoryRepository.deleteByMemoryType(3);
        memoryRepository.saveAll(shortMemories);
    }


    // 获取长期记忆
    private String getLongMemories() throws IOException {

        // 获取当前日期时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm", Locale.CHINESE);
        String formattedDate = now.format(formatter);

        // 获取上海当前天气
        String weather = weatherService.getWeather();

        // 获取规则记忆
        ArrayList<Memory> rulesMemories = new ArrayList<Memory>();
        memoryRepository.findByMemoryType(1).forEach(rulesMemories::add);
        MemoryUtil.createMemory(rulesMemories, "当前时间为："+formattedDate, false);
        MemoryUtil.createMemory(rulesMemories, weather, false);

        // 获取长期记忆
        ArrayList<Memory> longMemories = new ArrayList<Memory>();
        memoryRepository.findByMemoryType(2).forEach(longMemories::add);

        // 生成记忆列表
        StringBuilder sbLongMemory = new StringBuilder();
        for(int i=0; i<rulesMemories.size(); i++){
            sbLongMemory.append(i+1).append(". ").append(rulesMemories.get(i).getProposition()).append("\n");
        }
        for(int i=0; i<longMemories.size(); i++){
            sbLongMemory.append(i+1).append(". ").append(longMemories.get(i).getProposition()).append("\n");
        }
        String strLongMemory = sbLongMemory.toString();
        System.out.println("------Long Memories------:");
        System.out.println(strLongMemory);
        System.out.println("\n");
        return strLongMemory;
    }


    // 获取聊天记录
    private String getConversation(List<Message> messages){

        StringBuilder sbConversation = new StringBuilder();
        for(Message m : messages){
            String contentAfter = StringUtils.replace(m.getContent(), "\n", " ");
            sbConversation.append(m.getName()).append(": ").append(contentAfter).append("\n");
        }
        String strConversation = sbConversation.toString();
        System.out.println("------Conversation------:");
        System.out.println(strConversation);
        System.out.println("\n");
        return strConversation;
    }


    // 发送请求
    public String doRequest(String prompt, String modelName, Float temperature) throws IOException {

        // 调用千问
        URL url = new URL("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + secretKey);
        connection.setDoOutput(true);

        // 请求体
        String systemInput = "You are a helpful assistant.";
        String userInput = JSONUtil.escape(prompt);
        userInput = StringUtils.replace(userInput, "\"", "\\\"");
        String temp = "";
        if(temperature != -1f){
            temp = new StringBuilder(", \"temperature\": ").append(temperature).toString();
        }
        String jsonInputString = String.format("{\"model\": \"%s\", \"input\": {\"messages\": [{\"role\": \"system\", \"content\": \"%s\"}, {\"role\": \"user\", \"content\": \"%s\"}]}, \"parameters\": {\"result_format\": \"message\"%s}}",
                modelName,systemInput, userInput, temp);

        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
            wr.flush();
        }

        // 取得返回
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
        finally {
            connection.disconnect();
        }

        System.out.println("------Response------:");
        System.out.println(response.toString());
        System.out.println("\n");

        JSONObject jsonResponse = JSONUtil.parseObj(response);
        Object choice = jsonResponse.getJSONObject("output")
                .getJSONArray("choices").get(0);
        JSONObject choiceJson = JSONUtil.parseObj(choice);
        return choiceJson.getJSONObject("message").get("content").toString();
    }
}
