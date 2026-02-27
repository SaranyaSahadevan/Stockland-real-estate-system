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
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        if (statusCode == null) {
            statusCode = 500;
        }

        String title;
        String message = switch (statusCode) {
            case 400 -> {
                title = "Bad Request";
                yield "The request could not be understood by the server.";
            }
            case 401 -> {
                title = "Unauthorized";
                yield "You need to log in to access this resource.";
            }
            case 403 -> {
                title = "Access Denied";
                yield "You do not have permission to access this page.";
            }
            case 404 -> {
                title = "Page Not Found";
                yield "Sorry, the page you are looking for does not exist.";
            }
            case 503 -> {
                title = "Service Unavailable";
                yield "The service is temporarily unavailable. Please try again later.";
            }
            default -> {
                title = "Internal Server Error";
                yield "Something went wrong on our end. Please try again later.";
            }
        };

        ModelAndView mv = new ModelAndView("error/error-page");
        mv.addObject("status", statusCode);
        mv.addObject("title", title);
        mv.addObject("message", message);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        mv.addObject("isLoggedIn", isLoggedIn);


        return mv;
    }
}