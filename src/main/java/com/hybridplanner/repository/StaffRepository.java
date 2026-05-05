package com.hybridplanner.repository;

import com.hybridplanner.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    @Query("SELECT s FROM Staff s JOIN FETCH s.team t JOIN FETCH t.pyo JOIN FETCH t.room")
    List<Staff> findAllWithDetails();

    List<Staff> findByTeamId(Long teamId);
}
