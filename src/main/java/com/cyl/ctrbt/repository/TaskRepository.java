package com.cyl.ctrbt.repository;


import com.cyl.ctrbt.entity.Task;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TaskRepository extends CrudRepository<Task, Integer> {

    List<Task> findByReportFlg(Boolean reportFlg);

    Task findFirstById(Integer id);

}
