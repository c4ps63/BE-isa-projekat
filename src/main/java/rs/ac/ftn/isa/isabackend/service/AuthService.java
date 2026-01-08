package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.ac.ftn.isa.isabackend.dto.UserRegistrationDTO;
import rs.ac.ftn.isa.isabackend.model.Role;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.repository.RoleRepository;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;

import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public void registerUser(UserRegistrationDTO dto) {
        if (!dto.getPassword().equals(dto.getRepeatedPassword())) {
            throw new IllegalArgumentException("Lozinke se ne poklapaju");
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Korisnik sa ovim emailom vec postoji");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setAddress(dto.getAddress());
        user.setEnabled(false);

        List<Role> roles = roleRepository.findByName("ROLE_USER");
        if (roles.isEmpty()) {
            throw new RuntimeException("GRESKA: Rola ROLE_USER ne postoji u bazi!");
        }
        user.setRoles(roles);

        userRepository.save(user);

        emailService.sendActivationEmail(user.getEmail());
    }

    public void activateUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
    }
}