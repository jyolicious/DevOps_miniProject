package com.communityhealth.survey.controller;

import com.communityhealth.survey.entity.Survey;
import com.communityhealth.survey.service.SurveyService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    // HOME
    @GetMapping("/")
    public String home() {
        return "home";
    }

    // LIST / SEARCH
    @GetMapping("/surveys")
    public String getAllSurveys(
            @RequestParam(required = false) String query,
            Model model) {

        model.addAttribute(
                "surveys",
                surveyService.searchSurveys(query)
        );

        model.addAttribute("query", query);

        return "surveys/list";
    }

    // VIEW ONE
    @GetMapping("/surveys/{id}")
    public String getSurvey(
            @PathVariable Long id,
            Model model) {

        Survey survey = surveyService.getSurveyById(id);

        model.addAttribute("survey", survey);

        return "surveys/view";
    }

    // SHOW CREATE FORM
    @GetMapping("/surveys/create")
    public String showCreateForm(Model model) {

        model.addAttribute("survey", new Survey());

        return "surveys/create";
    }

    // CREATE
    @PostMapping("/surveys")
    public String createSurvey(
            @Valid @ModelAttribute("survey") Survey survey,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "surveys/create";
        }

        surveyService.createSurvey(survey);

        return "redirect:/surveys";
    }

    // SHOW UPDATE FORM
    @GetMapping("/surveys/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        Survey survey = surveyService.getSurveyById(id);

        model.addAttribute("survey", survey);

        return "surveys/edit";
    }

    // UPDATE
    @PostMapping("/surveys/{id}/update")
    public String updateSurvey(
            @PathVariable Long id,
            @Valid @ModelAttribute("survey") Survey survey,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "surveys/edit";
        }

        surveyService.updateSurvey(id, survey);

        return "redirect:/surveys/" + id;
    }
}