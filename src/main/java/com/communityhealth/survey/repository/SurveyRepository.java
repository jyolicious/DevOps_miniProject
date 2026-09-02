package com.communityhealth.survey.repository;

import com.communityhealth.survey.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

    @Query("""
            SELECT s FROM Survey s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(s.location) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(s.healthCondition) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    List<Survey> searchSurveys(@Param("query") String query);
}