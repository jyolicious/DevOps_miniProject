package com.chss.controller;

import com.chss.entity.User;
import com.chss.service.SurveyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final SurveyService surveyService;

    public DashboardController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User current = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", current);
        model.addAttribute("summary", surveyService.dashboardSummary());
        return "dashboard";
    }
}
