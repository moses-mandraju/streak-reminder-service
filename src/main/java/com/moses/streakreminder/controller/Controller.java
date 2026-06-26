package com.moses.streakreminder.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
   
    @GetMapping("/healthCheck")
    public ResponseEntity<String> healthCheck(){
        return new ResponseEntity<>("Streak Reminder Service is up and running", HttpStatusCode.valueOf(200));
    }



}
