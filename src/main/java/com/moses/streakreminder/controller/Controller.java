package com.moses.streakreminder.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moses.streakreminder.model.FirestoreSnapshotResponse;
import com.moses.streakreminder.service.FirestoreDebugService;

@RestController
public class Controller {
   
    private final FirestoreDebugService firestoreDebugService;

    public Controller(FirestoreDebugService firestoreDebugService) {
        this.firestoreDebugService = firestoreDebugService;
    }
    @GetMapping("/healthCheck")
    public ResponseEntity<String> healthCheck(){
        return new ResponseEntity<>("Streak Reminder Service is up and running", HttpStatusCode.valueOf(200));
    }
    
    @GetMapping("/firestore")
    public FirestoreSnapshotResponse firestore() {
        return firestoreDebugService.getFirestoreSnapshot();
    }

    @PostMapping("/notification/test/{userId}")
    public ResponseEntity<String> sendTestNotification(
        @PathVariable String userId) {

    return ResponseEntity.ok(
            firestoreDebugService.sendTestNotification(userId)
    );
}



}
