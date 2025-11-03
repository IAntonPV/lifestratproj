package com.lifestrat.service;

import com.lifestrat.dto.ProjectProgressDto;
import com.lifestrat.entity.Task;
import com.lifestrat.entity.TaskType;
import com.lifestrat.entity.Project;
import com.lifestrat.entity.LifeSphere;
import com.lifestrat.repository.TaskRepository;
import com.lifestrat.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public AnalyticsService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    // Получить данные для радар-диаграммы баланса сфер жизни
    public Map<String, Double> getLifeSphereBalance(Long userId) {
        log.debug("Getting life sphere balance for user ID: {}", userId);

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        // Получаем все завершенные задачи пользователя за последние 30 дней
        List<Task> completedTasks = taskRepository.findAllByUserId(userId).stream()
                .filter(Task::isCompleted)
                .filter(task -> task.getDeadline().isAfter(thirtyDaysAgo) ||
                        task.getDeadline().isEqual(thirtyDaysAgo))
                .toList();

        // Группируем по сферам жизни и суммируем время
        Map<String, Double> sphereBalance = completedTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getLifeSphere().getName(),
                        Collectors.summingDouble(Task::getEstimatedTimeMinutes)
                ));

        log.info("Life sphere balance calculated for user ID: {}. Spheres: {}", userId, sphereBalance.keySet());
        return sphereBalance;
    }

    // Получить прогресс по основным проектам
    public List<ProjectProgressDto> getMainProjectsProgress(Long userId) {
        log.debug("Getting projects progress for user ID: {}", userId);

        List<Project> userProjects = projectRepository.findAllByUserId(userId);
        List<ProjectProgressDto> progressList = new ArrayList<>();

        for (Project project : userProjects) {
            // Получаем все шаги (tasks типа STEP) для этого проекта
            List<Task> projectSteps = taskRepository.findAllByProjectId(project.getId()).stream()
                    .filter(task -> task.getType() == TaskType.STEP)
                    .toList();

            int totalSteps = projectSteps.size();
            int completedSteps = (int) projectSteps.stream()
                    .filter(Task::isCompleted)
                    .count();

            ProjectProgressDto progress = new ProjectProgressDto(
                    project.getId(),
                    project.getTitle(),
                    totalSteps,
                    completedSteps,
                    totalSteps > 0 ? (double) completedSteps / totalSteps * 100 : 0.0
            );

            progressList.add(progress);
        }

        log.info("Projects progress calculated for user ID: {}. Projects count: {}", userId, progressList.size());
        return progressList;
    }

    // Получить статистику продуктивности
    public Map<String, Object> getProductivityStats(Long userId) {
        log.debug("Getting productivity stats for user ID: {}", userId);

        // Получаем все завершенные задачи пользователя
        List<Task> completedTasks = taskRepository.findAllByUserId(userId).stream()
                .filter(Task::isCompleted)
                .toList();

        // Извлекаем уникальные даты выполнения задач
        Set<LocalDate> completedDates = completedTasks.stream()
                .map(task -> task.getDeadline()) // Используем deadline как дату выполнения
                .collect(Collectors.toSet());

        // Сортируем даты в хронологическом порядке
        List<LocalDate> sortedDates = completedDates.stream()
                .sorted()
                .toList();

        // Вычисляем текущую и максимальную серии
        long currentStreak = calculateCurrentStreak(sortedDates);
        long maxStreak = calculateMaxStreak(sortedDates);

        Map<String, Object> stats = new HashMap<>();
        stats.put("currentStreak", currentStreak);
        stats.put("maxStreak", maxStreak);
        stats.put("totalCompletedTasks", completedTasks.size());
        stats.put("analysisDate", LocalDate.now());

        log.info("Productivity stats calculated for user ID: {}. Current streak: {}, Max streak: {}",
                userId, currentStreak, maxStreak);

        return stats;
    }

    // Вычислить текущую серию дней подряд
    private long calculateCurrentStreak(List<LocalDate> sortedDates) {
        if (sortedDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Если сегодня или вчера не было выполненных задач, серия прервана
        if (!sortedDates.contains(today) && !sortedDates.contains(yesterday)) {
            return 0;
        }

        long streak = 0;
        LocalDate currentDate = today;

        // Проверяем последовательность дней в обратном порядке от сегодня
        while (sortedDates.contains(currentDate)) {
            streak++;
            currentDate = currentDate.minusDays(1);
        }

        return streak;
    }

    // Вычислить максимальную серию дней подряд
    private long calculateMaxStreak(List<LocalDate> sortedDates) {
        if (sortedDates.isEmpty()) {
            return 0;
        }

        long maxStreak = 1;
        long currentStreak = 1;

        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate previousDate = sortedDates.get(i - 1);
            LocalDate currentDate = sortedDates.get(i);

            // Проверяем, идут ли даты подряд
            if (previousDate.plusDays(1).equals(currentDate)) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }

    // Получить общую статистику по времени
    public Map<String, Object> getTimeStatistics(Long userId) {
        log.debug("Getting time statistics for user ID: {}", userId);

        List<Task> userTasks = taskRepository.findAllByUserId(userId);

        int totalTimePlanned = userTasks.stream()
                .mapToInt(Task::getEstimatedTimeMinutes)
                .sum();

        int totalTimeCompleted = userTasks.stream()
                .filter(Task::isCompleted)
                .mapToInt(Task::getEstimatedTimeMinutes)
                .sum();

        double completionRate = totalTimePlanned > 0 ?
                (double) totalTimeCompleted / totalTimePlanned * 100 : 0.0;

        Map<String, Object> timeStats = new HashMap<>();
        timeStats.put("totalTimePlanned", totalTimePlanned);
        timeStats.put("totalTimeCompleted", totalTimeCompleted);
        timeStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
        timeStats.put("tasksCount", userTasks.size());
        timeStats.put("completedTasksCount", userTasks.stream().filter(Task::isCompleted).count());

        log.info("Time statistics calculated for user ID: {}", userId);
        return timeStats;
    }

    // Получить распределение задач по приоритетам
    public Map<String, Long> getPriorityDistribution(Long userId) {
        log.debug("Getting priority distribution for user ID: {}", userId);

        List<Task> userTasks = taskRepository.findAllByUserId(userId);

        Map<String, Long> priorityDistribution = userTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getPriority().name(),
                        Collectors.counting()
                ));

        log.info("Priority distribution calculated for user ID: {}", userId);
        return priorityDistribution;
    }
}