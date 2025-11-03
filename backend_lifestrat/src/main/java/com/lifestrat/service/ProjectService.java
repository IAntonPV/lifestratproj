package com.lifestrat.service;

import com.lifestrat.entity.Project;
import com.lifestrat.entity.LifeSphere;
import com.lifestrat.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // Получить все проекты пользователя
    public List<Project> findAllByUserId(Long userId) {
        log.debug("Getting all projects for user ID: {}", userId);
        return projectRepository.findAllByUserId(userId);
    }

    // Найти проект по ID, убедившись, что он принадлежит пользователю
    public Optional<Project> findByIdAndUserId(Long projectId, Long userId) {
        log.debug("Finding project by ID: {} for user ID: {}", projectId, userId);
        return findAllByUserId(userId).stream()
                .filter(project -> project.getId().equals(projectId))
                .findFirst();
    }

    // Создать новый проект
    public Project create(Project project, Long userId) {
        log.debug("Creating new project for user ID: {}", userId);

        // Убеждаемся, что проект ассоциирован с правильным userId
        if (project.getUser() == null || !project.getUser().getId().equals(userId)) {
            log.warn("Project user association mismatch. Expected user ID: {}, but got: {}",
                    userId, project.getUser() != null ? project.getUser().getId() : "null");
            throw new IllegalArgumentException("Project must be associated with the correct user");
        }

        // Убеждаемся, что сфера жизни принадлежит пользователю
        LifeSphere lifeSphere = project.getLifeSphere();
        if (lifeSphere == null || !lifeSphere.getUser().getId().equals(userId)) {
            log.warn("Project life sphere association mismatch. Life sphere does not belong to user ID: {}", userId);
            throw new IllegalArgumentException("Project life sphere must belong to the user");
        }

        // Проверяем уникальность названия проекта для данного пользователя
        boolean titleExists = findAllByUserId(userId).stream()
                .anyMatch(p -> p.getTitle().equalsIgnoreCase(project.getTitle()));

        if (titleExists) {
            log.warn("Project with title '{}' already exists for user ID: {}", project.getTitle(), userId);
            throw new IllegalArgumentException("Project with title '" + project.getTitle() + "' already exists");
        }

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with ID: {} for user ID: {}", savedProject.getId(), userId);
        return savedProject;
    }

    // Обновить данные проекта
    public Project update(Project projectFromDb, Project projectFromRequest) {
        log.debug("Updating project ID: {}", projectFromDb.getId());

        // Копируем все поля из projectFromRequest в projectFromDb
        projectFromDb.setTitle(projectFromRequest.getTitle());
        projectFromDb.setDescription(projectFromRequest.getDescription());
        projectFromDb.setDeadline(projectFromRequest.getDeadline());
        projectFromDb.setPriority(projectFromRequest.getPriority());
        projectFromDb.setLifeSphere(projectFromRequest.getLifeSphere());

        Project updatedProject = projectRepository.save(projectFromDb);
        log.info("Project updated successfully with ID: {}", updatedProject.getId());
        return updatedProject;
    }

    // Удалить проект
    public void delete(Project project) {
        log.debug("Deleting project ID: {}", project.getId());
        projectRepository.delete(project);
        log.info("Project deleted successfully with ID: {}", project.getId());
    }

    // Получить проекты пользователя по сфере жизни
    public List<Project> findAllByUserIdAndLifeSphereId(Long userId, Long lifeSphereId) {
        log.debug("Getting projects for user ID: {} and life sphere ID: {}", userId, lifeSphereId);

        // Сначала получаем все проекты пользователя, затем фильтруем по сфере жизни
        return findAllByUserId(userId).stream()
                .filter(project -> project.getLifeSphere().getId().equals(lifeSphereId))
                .toList();
    }

    // Найти проекты по названию (поиск)
    public List<Project> findByTitleContainingAndUserId(String title, Long userId) {
        log.debug("Searching projects by title: '{}' for user ID: {}", title, userId);
        return findAllByUserId(userId).stream()
                .filter(project -> project.getTitle().toLowerCase().contains(title.toLowerCase()))
                .toList();
    }

    // Получить просроченные проекты пользователя
    public List<Project> findOverdueProjectsByUserId(Long userId) {
        log.debug("Getting overdue projects for user ID: {}", userId);
        java.time.LocalDate today = java.time.LocalDate.now();
        return findAllByUserId(userId).stream()
                .filter(project -> project.getDeadline().isBefore(today))
                .toList();
    }

    // Получить проекты с высоким приоритетом
    public List<Project> findHighPriorityProjectsByUserId(Long userId) {
        log.debug("Getting high priority projects for user ID: {}", userId);
        return findAllByUserId(userId).stream()
                .filter(project -> project.getPriority().toString().equals("HIGH") ||
                        project.getPriority().toString().equals("CRITICAL"))
                .toList();
    }
}