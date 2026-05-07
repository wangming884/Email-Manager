package com.mailmanager.controller;

import jakarta.servlet.http.HttpServletRequest;

public abstract class BaseController {

    protected String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute("traceId");
        return traceId == null ? "n/a" : traceId.toString();
    }
}
