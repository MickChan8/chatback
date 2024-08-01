package com.cyl.ctrbt.agent;

import cn.hutool.core.io.resource.ResourceUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Agent {

    protected String agentName;

    protected String model;

    protected Float temperature;

    protected String promptAction;

    protected String promptLastAction;

    protected String promptAnalyse;

    protected List<Message> messages;

    protected void InitAgent(String agentName, String sk, String model, Float temperature){
        com.alibaba.dashscope.utils.Constants.apiKey = sk;
        this.agentName = agentName;
        this.model = model;
        this.temperature = temperature;
        promptAction = ResourceUtil.readUtf8Str("classpath:prompt\\Agent\\DoAction.txt");
        promptLastAction = ResourceUtil.readUtf8Str("classpath:prompt\\Agent\\DoLastAction.txt");
        promptAnalyse = ResourceUtil.readUtf8Str("classpath:prompt\\Agent\\DoAnalyse.txt");
        messages = createMessages();
    }

    protected void loadStartPrompt(String prompt){
        messages.add(createMessage(Role.USER, prompt));
    }

    protected void loadActionPrompt(){
        messages.add(createMessage(Role.USER, promptAction));
    }

    protected void loadLastActionPrompt(){
        messages.add(createMessage(Role.USER, promptLastAction));
    }

    protected void loadAnalysePrompt(String report){
        String prompt = StringUtils.replace(promptAnalyse, "#report#", report);
        messages.add(createMessage(Role.USER, prompt));
    }

    protected static String doSimpleRequest(String agentName, String model, String prompt, Float temperature){
        List<Message> simpleMessages = createMessages();
        simpleMessages.add(createMessage(Role.USER, prompt));
        try {
            GenerationResult result = new Generation().call(createGenerationParam(model,simpleMessages, temperature));
            log.info("----------------"+agentName+"----------------：\n"
                    +result.getOutput().getChoices().get(0).getMessage().getContent());
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected String doRequest(){
        try {
            GenerationResult result = new Generation().call(createGenerationParam(model, messages, temperature));
            log.info("----------------"+agentName+"----------------：\n"
                    +result.getOutput().getChoices().get(0).getMessage().getContent());
            messages.add(result.getOutput().getChoices().get(0).getMessage());
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static List<Message> createMessages() {
        List<Message> newMessages = new ArrayList<>();
        newMessages.add(createMessage(Role.SYSTEM, "You are a helpful assistant."));
        return newMessages;
    }

    protected static Message createMessage(Role role, String content) {
        return Message.builder().role(role.getValue()).content(content).build();
    }

    private static GenerationParam createGenerationParam(String model, List<Message> messages, Float temperature) {
        return GenerationParam.builder()
                .model(model)
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .temperature(temperature)
                .build();
    }

}
