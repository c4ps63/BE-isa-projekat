package rs.ac.ftn.isa.isabackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.dto.LoginDTO;
import rs.ac.ftn.isa.isabackend.dto.UserRegistrationDTO;
import rs.ac.ftn.isa.isabackend.security.TokenUtils;
import rs.ac.ftn.isa.isabackend.service.AuthService;
import rs.ac.ftn.isa.isabackend.service.LoginAttemptService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenUtils tokenUtils;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid UserRegistrationDTO dto) {
        try {
            authService.registerUser(dto);
            return ResponseEntity.ok("Registration successful. Please check your email to activate your account.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activate(@RequestParam String email) {
        authService.activateUser(email);
        return ResponseEntity.ok("Account activated successfully! You can now login.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO loginDto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        if (loginAttemptService.isBlocked(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many login attempts. Please try again in 1 minute.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = tokenUtils.generateToken(userDetails);

            loginAttemptService.loginSucceeded(ip);

            return ResponseEntity.ok(jwt);

        } catch (DisabledException e) {
            System.out.println(">>> Account is not activated: " + loginDto.getEmail());
            loginAttemptService.loginFailed(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Account is not activated. Please check your email.");
        } catch (BadCredentialsException e) {
            System.out.println(">>> Invalid credentials for: " + loginDto.getEmail());
            loginAttemptService.loginFailed(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password.");
        } catch (Exception e) {
            System.out.println(">>> LOGIN ERROR: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            loginAttemptService.loginFailed(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication failed.");
        }
    }
}
