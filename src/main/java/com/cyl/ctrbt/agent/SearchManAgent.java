package com.cyl.ctrbt.agent;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.constants.Constants;
import com.cyl.ctrbt.dto.SearchManAction;
import com.cyl.ctrbt.entity.BingWebPagesResult;
import com.cyl.ctrbt.service.SearchService;
import com.cyl.ctrbt.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@Scope("prototype")
public class SearchManAgent extends Agent{

    private String promptResearch;

    @Autowired
    private SearchService searchService;

    @Autowired
    public SearchManAgent(Environment env ){

        // 初始化Agent
        InitAgent("搜索员", env.getProperty("qwen.secret_key"), Constants.QWEN2_72B, 0.5f);

        promptResearch = ResourceUtil.readUtf8Str("classpath:prompt\\Agent\\SearchWeb\\SearchManResearch.txt");

    }

    public String doSearch(String keywords, String context) throws IOException {

        log.info("----------------搜索员_接到任务----------------：\n"
                + "调查任务：" + context + "\n"
                + "搜索关键词：" + keywords + "\n");

        // 获取当前日期时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm", Locale.CHINESE);
        String formattedDate = now.format(formatter);

        // 获取搜索结果
        List<BingWebPagesResult> bingPageList = searchService.searchBingWebpages(keywords);
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for(int i = 0; i< bingPageList.size(); i++){
          sb.append(i+1).append(". ").append(bingPageList.get(i).getName()).append("\n");
          sb.append(bingPageList.get(i).getSnippet()).append("\n").append("\n");
          sb2.append(i+1).append(". ").append(bingPageList.get(i).getName()).append("\n");
          sb2.append(bingPageList.get(i).getSnippet()).append("\n").append(bingPageList.get(i).getUrl()).append("\n").append("\n");
        }
        String resultList = sb.toString();
        log.info("----------------搜索员_搜索结果----------------：\n" + sb2.toString());

        // 获取初始prompt
        String startPrompt = ResourceUtil.readUtf8Str("classpath:prompt\\Agent\\SearchWeb\\SearchMan.txt");
        // 处理拼接prompt
        startPrompt = StringUtils.replace(startPrompt, "#time#", formattedDate);
        startPrompt = StringUtils.replace(startPrompt, "#keywords#", keywords);
        startPrompt = StringUtils.replace(startPrompt, "#context#", context);
        startPrompt = StringUtils.replace(startPrompt, "#result#", resultList);

        loadStartPrompt(startPrompt);
        doRequest();

        int count = 0;
        while(count < 10) {
            loadActionPrompt();
            String jsonStr = JsonUtil.formatJsonStr(doRequest());
            SearchManAction action = JSONUtil.toBean(jsonStr, SearchManAction.class);

            // 向上级报告
            if (action.getAction().equals("report")) {

                log.info("----------------搜索员_汇报任务----------------：\n" + action.getContext());
                return action.getContext();

            }

            // 获取research结果
            // 防止下标溢出
            if(action.getId() < 1) action.setId(1);
            else if(action.getId() > bingPageList.size()) action.setId(bingPageList.size());
            // 获取页面内容
            String content = searchService.getPageText(bingPageList.get(action.getId()-1).getUrl());
            // 拼接prompt
            String promptRes = StringUtils.replace(promptResearch, "#time#", formattedDate);
            promptRes = StringUtils.replace(promptRes, "#context#", context);
            promptRes = StringUtils.replace(promptRes, "#content#", content);
            String report = doSimpleRequest("搜索员_页面报告", Constants.QWEN2_72B, promptRes, 0.5f);

            loadAnalysePrompt("你对该页面进行了调查，以下是调查报告：\n"+report);
            doRequest();
            count ++;
        }

        //最终action
        loadLastActionPrompt();
        String jsonStr = JsonUtil.formatJsonStr(doRequest());
        SearchManAction action = JSONUtil.toBean(jsonStr, SearchManAction.class);

        log.info("----------------搜索员_汇报任务----------------：\n" + action.getContext());
        return action.getContext();

    }
}
