package com.stockland.app.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {

        Integer statusCode =
                (Integer) request.getAttribute("jakarta.servlet.error.status_code");

        if (statusCode == null) statusCode = 500;

        String title;
        String message;

        switch (statusCode) {
            case 400 -> {
                title = "Bad Request";
                message = "Invalid request.";
            }
            case 401 -> {
                title = "Unauthorized";
                message = "Login required.";
            }
            case 403 -> {
                title = "Access Denied";
                message = "No permission.";
            }
            case 404 -> {
                title = "Not Found";
                message = "Page not found.";
            }
            case 503 -> {
                title = "Service Unavailable";
                message = "Try later.";
            }
            default -> {
                title = "Server Error";
                message = "Unexpected error.";
            }
        }

        ModelAndView mv = new ModelAndView("error/error-page");
        mv.addObject("status", statusCode);
        mv.addObject("title", title);
        mv.addObject("message", message);

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        boolean logged =
                auth != null &&
                        auth.isAuthenticated() &&
                        !"anonymousUser".equals(auth.getPrincipal());

        mv.addObject("isLoggedIn", logged);

        return mv;
    }
}
