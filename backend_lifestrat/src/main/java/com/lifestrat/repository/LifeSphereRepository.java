package com.lifestrat.repository;

import com.lifestrat.entity.LifeSphere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LifeSphereRepository extends JpaRepository<LifeSphere, Long> {

    List<LifeSphere> findAllByUserId(Long userId);
}