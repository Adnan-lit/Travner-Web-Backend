package org.adnan.travner.controller;

import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("public")
public class PublicController {

    @Autowired
    private UserService userService;

    @PostMapping("create-user")
    public ResponseEntity<Void> createUser(@RequestBody UserEntry user) {
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            userService.saveNewUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}