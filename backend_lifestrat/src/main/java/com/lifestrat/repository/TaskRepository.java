package com.lifestrat.repository;

import com.lifestrat.entity.Task;
import com.lifestrat.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByUserId(Long userId);

    List<Task> findAllByUserIdAndType(Long userId, TaskType type);

    List<Task> findAllByProjectId(Long projectId);

    List<Task> findAllByLifeSphereId(Long lifeSphereId);
}