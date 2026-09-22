package com.chss.service;

import com.chss.entity.Survey;
import com.chss.entity.SurveyStatus;
import com.chss.entity.User;
import com.chss.repository.SurveyRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;

    public SurveyService(SurveyRepository surveyRepository) {
        this.surveyRepository = surveyRepository;
    }

    /** FR-1 / US-1: new records always start at DRAFT and record the creator. */
    public Survey create(Survey survey, User creator) {
        survey.setStatus(SurveyStatus.DRAFT);
        survey.setCreatedBy(creator);
        return surveyRepository.save(survey);
    }

    /** FR-6 / PB-17: update details prior to closure. */
    public Survey update(Long id, Survey updated) {
        Survey existing = surveyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found: " + id));
        existing.setName(updated.getName());
        existing.setAge(updated.getAge());
        existing.setGender(updated.getGender());
        existing.setLocation(updated.getLocation());
        existing.setHealthCondition(updated.getHealthCondition());
        existing.setSurveyDate(updated.getSurveyDate());
        return surveyRepository.save(existing);
    }

    /** FR-3 / US-3: only single-step forward transitions are allowed. */
    public Survey changeStatus(Long id, SurveyStatus target) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found: " + id));
        if (!survey.getStatus().canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid transition: " + survey.getStatus() + " -> " + target);
        }
        survey.setStatus(target);
        return surveyRepository.save(survey);
    }

    /** FR-2 / US-2: case-insensitive search by name; falls back to id lookup if numeric. */
    public List<Survey> search(String query) {
        if (query == null || query.isBlank()) {
            return surveyRepository.findAll();
        }
        try {
            Long id = Long.parseLong(query.trim());
            return surveyRepository.findById(id).map(List::of).orElseGet(List::of);
        } catch (NumberFormatException notAnId) {
            return surveyRepository.findByNameContainingIgnoreCase(query.trim());
        }
    }

    public Survey get(Long id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found: " + id));
    }

    /** FR-4 / US-4: dashboard summary counts. */
    public Map<String, Long> dashboardSummary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        long total = surveyRepository.count();
        summary.put("TOTAL", total);
        for (SurveyStatus status : SurveyStatus.values()) {
            summary.put(status.name(), surveyRepository.countByStatus(status));
        }
        return summary;
    }
}
