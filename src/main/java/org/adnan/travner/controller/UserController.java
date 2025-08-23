package org.adnan.travner.controller;


import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getUser() {
        return new ResponseEntity<>(userService.getAll(), HttpStatus.OK);
    }


}
