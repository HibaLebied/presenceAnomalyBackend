package com.company.attendance.repository.dashboard;

import com.company.attendance.entity.dashboard.AnomalyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnomalyTypeRepository extends JpaRepository<AnomalyType, Integer> {

    Optional<AnomalyType> findByCode(String code);

    boolean existsByCode(String code);
}
