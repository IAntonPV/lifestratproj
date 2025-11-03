package com.lifestrat.service;

import com.lifestrat.entity.LifeSphere;
import com.lifestrat.entity.User;
import com.lifestrat.repository.LifeSphereRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class LifeSphereService {

    private final LifeSphereRepository lifeSphereRepository;

    @Autowired
    public LifeSphereService(LifeSphereRepository lifeSphereRepository) {
        this.lifeSphereRepository = lifeSphereRepository;
    }

    // Получить все сферы жизни пользователя
    public List<LifeSphere> findAllByUserId(Long userId) {
        log.debug("Getting all life spheres for user ID: {}", userId);
        return lifeSphereRepository.findAllByUserId(userId);
    }

    // Найти сферу по ID, убедившись, что она принадлежит пользователю
    public Optional<LifeSphere> findByIdAndUserId(Long sphereId, Long userId) {
        log.debug("Finding life sphere by ID: {} for user ID: {}", sphereId, userId);
        return findAllByUserId(userId).stream()
                .filter(sphere -> sphere.getId().equals(sphereId))
                .findFirst();
    }

    // Создать новую сферу жизни
    public LifeSphere create(LifeSphere lifeSphere, Long userId) {
        log.debug("Creating new life sphere for user ID: {}", userId);

        // Убеждаемся, что сфера ассоциирована с правильным userId
        if (lifeSphere.getUser() == null || !lifeSphere.getUser().getId().equals(userId)) {
            log.warn("Life sphere user association mismatch. Expected user ID: {}, but got: {}",
                    userId, lifeSphere.getUser() != null ? lifeSphere.getUser().getId() : "null");
            throw new IllegalArgumentException("Life sphere must be associated with the correct user");
        }

        // Проверяем уникальность имени сферы для данного пользователя
        boolean nameExists = findAllByUserId(userId).stream()
                .anyMatch(sphere -> sphere.getName().equalsIgnoreCase(lifeSphere.getName()));

        if (nameExists) {
            log.warn("Life sphere with name '{}' already exists for user ID: {}", lifeSphere.getName(), userId);
            throw new IllegalArgumentException("Life sphere with name '" + lifeSphere.getName() + "' already exists");
        }

        LifeSphere savedSphere = lifeSphereRepository.save(lifeSphere);
        log.info("Life sphere created successfully with ID: {} for user ID: {}", savedSphere.getId(), userId);
        return savedSphere;
    }

    // Обновить данные сферы
    public LifeSphere update(LifeSphere lifeSphereFromDb, LifeSphere lifeSphereFromRequest) {
        log.debug("Updating life sphere ID: {}", lifeSphereFromDb.getId());

        // Копируем все поля из lifeSphereFromRequest в lifeSphereFromDb
        lifeSphereFromDb.setName(lifeSphereFromRequest.getName());
        lifeSphereFromDb.setColor(lifeSphereFromRequest.getColor());

        LifeSphere updatedSphere = lifeSphereRepository.save(lifeSphereFromDb);
        log.info("Life sphere updated successfully with ID: {}", updatedSphere.getId());
        return updatedSphere;
    }

    // Удалить сферу жизни
    public void delete(LifeSphere lifeSphere) {
        log.debug("Deleting life sphere ID: {}", lifeSphere.getId());
        lifeSphereRepository.delete(lifeSphere);
        log.info("Life sphere deleted successfully with ID: {}", lifeSphere.getId());
    }

    // Создать стандартные сферы жизни для нового пользователя
    public List<LifeSphere> createDefaultLifeSpheres(User user) {
        log.debug("Creating default life spheres for new user ID: {}", user.getId());

        List<LifeSphere> defaultSpheres = Arrays.asList(
                createLifeSphere("Карьера", "#FF6B6B", user),
                createLifeSphere("Финансы", "#4ECDC4", user),
                createLifeSphere("Здоровье", "#45B7D1", user),
                createLifeSphere("Отношения", "#96CEB4", user),
                createLifeSphere("Саморазвитие", "#FFEAA7", user),
                createLifeSphere("Отдых", "#DDA0DD", user)
        );

        List<LifeSphere> savedSpheres = lifeSphereRepository.saveAll(defaultSpheres);
        log.info("Created {} default life spheres for user ID: {}", savedSpheres.size(), user.getId());
        return savedSpheres;
    }

    // Вспомогательный метод для создания сферы жизни
    private LifeSphere createLifeSphere(String name, String color, User user) {
        LifeSphere sphere = new LifeSphere();
        sphere.setName(name);
        sphere.setColor(color);
        sphere.setUser(user);
        return sphere;
    }

    // Найти сферу по имени для пользователя
    public Optional<LifeSphere> findByNameAndUserId(String name, Long userId) {
        log.debug("Finding life sphere by name: {} for user ID: {}", name, userId);
        return findAllByUserId(userId).stream()
                .filter(sphere -> sphere.getName().equalsIgnoreCase(name))
                .findFirst();
    }
}