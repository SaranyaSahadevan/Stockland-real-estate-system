package com.stockland.app.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleAllRuntimeErrors(RuntimeException ex) {
        ModelAndView mv = new ModelAndView("error/error-page");
        mv.addObject("status", 500);
        mv.addObject("title", "Application Error");

        mv.addObject("message", ex.getMessage());

        return mv;
    }
}
