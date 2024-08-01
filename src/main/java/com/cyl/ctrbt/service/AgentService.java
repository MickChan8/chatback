package com.cyl.ctrbt.service;

import com.cyl.ctrbt.agent.SearchManagerAgent;
import com.cyl.ctrbt.entity.Task;
import com.cyl.ctrbt.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class AgentService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TaskRepository taskRepository;

    public void doSearch(String topic) throws IOException {

        SearchManagerAgent agentSearchManager = applicationContext.getBean(SearchManagerAgent.class);
        String result = agentSearchManager.doSearch(topic, "报告应尽量详细");

        Task newTask = new Task(topic, result);
        taskRepository.save(newTask);

    }

    public String getSearchResultSummary(){
        List<Task> tasks = taskRepository.findByReportFlg(false);
        if(tasks.isEmpty()){
            return "当前暂无可查询的报告。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("当前可领取的报告如下：\n");
        for(Task task : tasks){
            sb.append(task.getId()).append(". ").append(task.getTaskName()).append("\n");
        }
        sb.append("请回复'二狗查询XX号报告'领取报告。报告领取后便不显示，请勿领取别人的报告！");
        return sb.toString();
    }

    public String getAllSearchResultSummary(){
        List<Task> tasks = (List<Task>) taskRepository.findAll();
        if(tasks.isEmpty()){
            return "当前暂无可查询的报告。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("所有报告如下：\n");
        for(Task task : tasks){
            sb.append(task.getId()).append(". ").append(task.getTaskName()).append("\n");
        }
        return sb.toString();
    }

    public String getSearchResult(Integer id){
        Task task = taskRepository.findFirstById(id);
        if(task != null && !task.getResult().isEmpty()){
            task.setReportFlg(true);
            taskRepository.save(task);
            return task.getResult();
        }
        else
            return "查无此报告！";
    }
}
