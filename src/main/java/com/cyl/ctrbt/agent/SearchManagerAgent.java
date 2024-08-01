package com.cyl.ctrbt.agent;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.common.Role;
import com.cyl.ctrbt.constants.Constants;
import com.cyl.ctrbt.dto.SearchManagerAction;
import com.cyl.ctrbt.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@Scope("prototype")
public class SearchManagerAgent extends Agent{

    private String promptReflection;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    public SearchManagerAgent(Environment env ){

        // 初始化Agent
        InitAgent("调查经理", env.getProperty("qwen.secret_key"), Constants.QWEN15_110B, 0.5f);

        promptReflection = ResourceUtil.readUtf8Str("classpath:prompt\\Agent\\SearchWeb\\SearchManagerReflection.txt");

    }

    private void loadReflectionPrompt(String topic, String context){
        String prompt = StringUtils.replace(promptReflection, "#topic#", topic);
        prompt = StringUtils.replace(prompt, "#context#", context);
        messages.add(createMessage(Role.USER, prompt));
    }

    public String doSearch(String topic, String context) throws IOException {

        log.info("----------------调查经理_接到任务----------------：\n"
                + "调查主题：" + topic);

        // 获取当前日期时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm", Locale.CHINESE);
        String formattedDate = now.format(formatter);

        // 获取初始prompt
        String startPrompt = ResourceUtil.readUtf8Str("classpath:prompt\\Agent\\SearchWeb\\SearchManager.txt");
        // 处理拼接prompt
        startPrompt = StringUtils.replace(startPrompt, "#time#", formattedDate);
        startPrompt = StringUtils.replace(startPrompt, "#topic#", topic);
        startPrompt = StringUtils.replace(startPrompt, "#context#", context);

        loadStartPrompt(startPrompt);
        doRequest();

        int count = 0;
        while(count < 10) {
            loadActionPrompt();
            String jsonStr = JsonUtil.formatJsonStr(doRequest());
            SearchManagerAction action = JSONUtil.toBean(jsonStr, SearchManagerAction.class);

            // 向上级报告
            if (action.getAction().equals("report")) {

                // 报告前反思
                loadReflectionPrompt(topic, context);
                String finalReport = doRequest();

                log.info("----------------调查经理_汇报任务----------------：\n" + finalReport);
                return finalReport;

            }

            // 布置任务，获取search结果
            SearchManAgent agent = applicationContext.getBean(SearchManAgent.class);
            String report = agent.doSearch(action.getKeywords(), action.getContext());

            loadAnalysePrompt("搜索员向你汇报了调查报告：\n"+report);
            doRequest();
            count ++;
        }

        //最终action
        loadLastActionPrompt();
        doRequest();

        // 报告前反思
        loadReflectionPrompt(topic, context);
        String finalReport = doRequest();

        log.info("----------------调查经理_汇报任务----------------：\n" + finalReport);
        return finalReport;
    }
}
