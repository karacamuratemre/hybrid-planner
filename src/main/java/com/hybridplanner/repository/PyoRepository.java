package com.hybridplanner.repository;

import com.hybridplanner.model.Pyo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PyoRepository extends JpaRepository<Pyo, Long> {
}
