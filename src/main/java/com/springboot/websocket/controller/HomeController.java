package com.springboot.websocket.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpSession session) {
        return session.getAttribute("username") != null ? "redirect:/chat" : "home";
    }

    @GetMapping("/chat")
    public String chat(HttpSession session) {
        return session.getAttribute("username") != null ? "chat" : "redirect:/login";
    }
}
