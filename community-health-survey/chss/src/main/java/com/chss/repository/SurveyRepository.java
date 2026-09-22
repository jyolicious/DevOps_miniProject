package com.chss.repository;

import com.chss.entity.Survey;
import com.chss.entity.SurveyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

    // FR-2 / US-2: case-insensitive search by name
    List<Survey> findByNameContainingIgnoreCase(String name);

    // FR-4 / US-4: dashboard summary counts
    long countByStatus(SurveyStatus status);
}
