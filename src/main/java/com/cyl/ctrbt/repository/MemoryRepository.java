package com.cyl.ctrbt.repository;


import com.cyl.ctrbt.entity.Memory;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MemoryRepository extends CrudRepository<Memory, Integer> {
    List<Memory> findByMemoryType(Integer memoryType);

    Memory findOneByMemoryType(Integer memoryType);

    @Modifying
    @Transactional
    @Query("DELETE FROM Memory m WHERE m.memoryType = :memoryType")
    void deleteByMemoryType(@Param("memoryType") Integer memoryType);
}
