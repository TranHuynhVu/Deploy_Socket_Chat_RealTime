package com.springboot.websocket.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        return session.getAttribute("username") != null ? "redirect:/chat" : "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                       HttpSession session,
                       Model model) {
        if (username != null && !username.trim().isEmpty()) {
            session.setAttribute("username", username.trim());
            return "redirect:/chat";
        }
        model.addAttribute("error", "Username cannot be empty");
        return "auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}