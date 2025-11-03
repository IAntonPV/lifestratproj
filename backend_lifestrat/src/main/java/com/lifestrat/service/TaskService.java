package com.lifestrat.service;

import com.lifestrat.entity.Task;
import com.lifestrat.entity.TaskType;
import com.lifestrat.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // Получить все задачи пользователя
    public List<Task> findAllByUserId(Long userId) {
        log.debug("Getting all tasks for user ID: {}", userId);
        return taskRepository.findAllByUserId(userId);
    }

    // Получить задачи пользователя по типу
    public List<Task> findAllByUserIdAndType(Long userId, TaskType type) {
        log.debug("Getting tasks for user ID: {} and type: {}", userId, type);
        return taskRepository.findAllByUserIdAndType(userId, type);
    }

    // Найти задачу по ID, убедившись, что она принадлежит пользователю
    public Optional<Task> findByIdAndUserId(Long taskId, Long userId) {
        log.debug("Finding task by ID: {} for user ID: {}", taskId, userId);
        return findAllByUserId(userId).stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst();
    }

    // Сохранить новую задачу
    public Task create(Task task, Long userId) {
        log.debug("Creating new task for user ID: {}", userId);

        // Убеждаемся, что задача ассоциирована с правильным userId
        if (task.getUser() == null || !task.getUser().getId().equals(userId)) {
            log.warn("Task user association mismatch. Expected user ID: {}, but got: {}",
                    userId, task.getUser() != null ? task.getUser().getId() : "null");
            throw new IllegalArgumentException("Task must be associated with the correct user");
        }

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with ID: {} for user ID: {}", savedTask.getId(), userId);
        return savedTask;
    }

    // Обновить данные задачи
    public Task update(Task taskFromDb, Task taskFromRequest) {
        log.debug("Updating task ID: {}", taskFromDb.getId());

        // Копируем все поля из taskFromRequest в taskFromDb
        taskFromDb.setTitle(taskFromRequest.getTitle());
        taskFromDb.setDescription(taskFromRequest.getDescription());
        taskFromDb.setDeadline(taskFromRequest.getDeadline());
        taskFromDb.setPriority(taskFromRequest.getPriority());
        taskFromDb.setEstimatedTimeMinutes(taskFromRequest.getEstimatedTimeMinutes());
        taskFromDb.setEnergyCost(taskFromRequest.getEnergyCost());
        taskFromDb.setCompleted(taskFromRequest.isCompleted());

        Task updatedTask = taskRepository.save(taskFromDb);
        log.info("Task updated successfully with ID: {}", updatedTask.getId());
        return updatedTask;
    }

    // Удалить задачу
    public void delete(Task task) {
        log.debug("Deleting task ID: {}", task.getId());
        taskRepository.delete(task);
        log.info("Task deleted successfully with ID: {}", task.getId());
    }

    // Отметить задачу как выполненную
    public Task markAsCompleted(Long taskId, Long userId) {
        log.debug("Marking task as completed - Task ID: {}, User ID: {}", taskId, userId);

        Task task = findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> {
                    log.error("Task not found or access denied - Task ID: {}, User ID: {}", taskId, userId);
                    return new RuntimeException("Task not found or access denied");
                });

        task.setCompleted(true);
        Task completedTask = taskRepository.save(task);
        log.info("Task marked as completed - Task ID: {}, User ID: {}", taskId, userId);
        return completedTask;
    }
}