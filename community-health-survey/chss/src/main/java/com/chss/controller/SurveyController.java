package com.chss.controller;

import com.chss.entity.Role;
import com.chss.entity.Survey;
import com.chss.entity.SurveyStatus;
import com.chss.entity.User;
import com.chss.service.SurveyService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/surveys")
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    // FR-2 / US-2: search + full list
    @GetMapping
    public String list(@RequestParam(required = false) String query, Model model) {
        model.addAttribute("surveys", surveyService.search(query));
        model.addAttribute("query", query);
        return "survey-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("survey", new Survey());
        return "survey-form";
    }

    // FR-1 / US-1: create, validated, always starts at DRAFT
    @PostMapping
    public String create(@Valid @ModelAttribute Survey survey, BindingResult result,
                          HttpSession session, Model model) {
        if (result.hasErrors()) {
            return "survey-form";
        }
        User creator = (User) session.getAttribute("currentUser");
        surveyService.create(survey, creator);
        return "redirect:/surveys";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("survey", surveyService.get(id));
        model.addAttribute("statuses", SurveyStatus.values());
        return "survey-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("survey", surveyService.get(id));
        return "survey-form";
    }

    // FR-6 / PB-17
    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute Survey survey,
                          BindingResult result) {
        if (result.hasErrors()) {
            return "survey-form";
        }
        surveyService.update(id, survey);
        return "redirect:/surveys/" + id;
    }

    // FR-3 / US-3: Administrator only; invalid transitions rejected with an error message
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam SurveyStatus target,
                                HttpSession session, Model model) {
        User current = (User) session.getAttribute("currentUser");
        if (current == null || current.getRole() != Role.ADMINISTRATOR) {
            model.addAttribute("error", "Only an Administrator can change survey status.");
            model.addAttribute("survey", surveyService.get(id));
            model.addAttribute("statuses", SurveyStatus.values());
            return "survey-detail";
        }
        try {
            surveyService.changeStatus(id, target);
        } catch (IllegalStateException invalidTransition) {
            model.addAttribute("error", invalidTransition.getMessage());
        }
        model.addAttribute("survey", surveyService.get(id));
        model.addAttribute("statuses", SurveyStatus.values());
        return "survey-detail";
    }
}
