package com.communityhealth.survey.service;

import com.communityhealth.survey.entity.Survey;
import com.communityhealth.survey.enums.SurveyStatus;
import com.communityhealth.survey.repository.SurveyRepository;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;

    public SurveyService(SurveyRepository surveyRepository) {
        this.surveyRepository = surveyRepository;
    }

    // CREATE
    public Survey createSurvey(Survey survey) {

        survey.setStatus(SurveyStatus.DRAFT);

        return surveyRepository.save(survey);
    }

    // READ ALL
    public List<Survey> getAllSurveys() {

        return surveyRepository.findAll();
    }

    // READ ONE
    public Survey getSurveyById(Long id) {

        return surveyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Survey not found with id: " + id));
    }

    // UPDATE
    public Survey updateSurvey(Long id, Survey updatedSurvey) {

        Survey existingSurvey = getSurveyById(id);

        existingSurvey.setName(updatedSurvey.getName());
        existingSurvey.setAge(updatedSurvey.getAge());
        existingSurvey.setGender(updatedSurvey.getGender());
        existingSurvey.setLocation(updatedSurvey.getLocation());
        existingSurvey.setHealthCondition(
                updatedSurvey.getHealthCondition());
        existingSurvey.setSurveyDate(
                updatedSurvey.getSurveyDate());

        // Status is intentionally NOT changed here.
        // Status changes will be handled separately in Week 6.

        return surveyRepository.save(existingSurvey);
    }

    // SEARCH
    public List<Survey> searchSurveys(String query) {

        if (query == null || query.trim().isEmpty()) {
            return surveyRepository.findAll();
        }

        query = query.trim();

        // If query is a number, search by survey ID
        try {

            Long id = Long.parseLong(query);

            return surveyRepository.findById(id)
                    .map(List::of)
                    .orElse(Collections.emptyList());

        } catch (NumberFormatException ignored) {

            // Not a number, so search text fields
            return surveyRepository.searchSurveys(query);
        }
    }

    public Survey updateStatus(Long id, SurveyStatus newStatus, String changedBy) {

    Survey survey = surveyRepository.findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Survey not found with id: " + id));

    SurveyStatus currentStatus = survey.getStatus();

    boolean validTransition =
            (currentStatus == SurveyStatus.DRAFT && newStatus == SurveyStatus.SUBMITTED)
            || (currentStatus == SurveyStatus.SUBMITTED && newStatus == SurveyStatus.VERIFIED)
            || (currentStatus == SurveyStatus.VERIFIED && newStatus == SurveyStatus.CLOSED);

    if (!validTransition) {
        throw new IllegalStateException(
                "Invalid status transition from "
                        + currentStatus + " to " + newStatus
        );
    }

    survey.setStatus(newStatus);
    survey.setStatusChangedBy(changedBy);

    return surveyRepository.save(survey);
}

public long getTotalSurveyCount() {
    return surveyRepository.count();
}

public long getDraftCount() {
    return surveyRepository.countByStatus(SurveyStatus.DRAFT);
}

public long getSubmittedCount() {
    return surveyRepository.countByStatus(SurveyStatus.SUBMITTED);
}

public long getVerifiedCount() {
    return surveyRepository.countByStatus(SurveyStatus.VERIFIED);
}

public long getClosedCount() {
    return surveyRepository.countByStatus(SurveyStatus.CLOSED);
}
}