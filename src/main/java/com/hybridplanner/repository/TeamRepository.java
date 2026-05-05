package com.hybridplanner.repository;

import com.hybridplanner.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    @Query("SELECT t FROM Team t JOIN FETCH t.pyo JOIN FETCH t.room")
    List<Team> findAllWithDetails();
}
