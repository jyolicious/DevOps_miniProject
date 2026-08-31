package com.communityhealth.survey.controller;

import com.communityhealth.survey.entity.Survey;
import com.communityhealth.survey.service.SurveyService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/surveys")
    public String getAllSurveys(Model model) {

        model.addAttribute(
                "surveys",
                surveyService.getAllSurveys()
        );

        return "surveys/list";
    }

    @GetMapping("/surveys/{id}")
    public String getSurvey(
            @PathVariable Long id,
            Model model) {

        Survey survey = surveyService.getSurveyById(id);

        model.addAttribute("survey", survey);

        return "surveys/view";
    }

    @GetMapping("/surveys/create")
public String showCreateForm(Model model) {

    model.addAttribute("survey", new Survey());

    return "surveys/create";
}

@PostMapping("/surveys")
public String createSurvey(@ModelAttribute Survey survey) {

    surveyService.createSurvey(survey);

    return "redirect:/surveys";
}
}