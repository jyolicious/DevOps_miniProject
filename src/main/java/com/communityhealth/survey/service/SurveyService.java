package com.communityhealth.survey.service;

import com.communityhealth.survey.entity.Survey;
import com.communityhealth.survey.enums.SurveyStatus;
import com.communityhealth.survey.repository.SurveyRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;

    public SurveyService(SurveyRepository surveyRepository) {
        this.surveyRepository = surveyRepository;
    }

    public Survey createSurvey(Survey survey) {

        survey.setStatus(SurveyStatus.DRAFT);

        return surveyRepository.save(survey);
    }

    public List<Survey> getAllSurveys() {

        return surveyRepository.findAll();
    }

    public Survey getSurveyById(Long id) {

        return surveyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Survey not found with id: " + id));
    }
}