package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.dto.UserDTO;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.service.UserService;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);

        if (user.isPresent()) {
            return ResponseEntity.ok(new UserDTO(user.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);

        if (user.isPresent()) {
            return ResponseEntity.ok(new UserDTO(user.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // TODO: POST, PUT, DELETE - za autentifikovane korisnike
}