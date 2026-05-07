package com.mailmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/accounts",
            "/accounts/{id}",
            "/import-jobs",
            "/emails",
            "/clients",
            "/rules",
            "/webhooks",
            "/audit-logs"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
