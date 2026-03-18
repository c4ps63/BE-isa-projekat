package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.dto.WatchPartyDTO;
import rs.ac.ftn.isa.isabackend.service.WatchPartyService;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/watch-party")
public class WatchPartyController {

    private final WatchPartyService watchPartyService;

    @Autowired
    public WatchPartyController(WatchPartyService watchPartyService) {
        this.watchPartyService = watchPartyService;
    }

    @PostMapping
    public ResponseEntity<?> createRoom(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Korisnik nije ulogovan.");
        }
        try {
            WatchPartyDTO room = watchPartyService.createRoom(principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(room);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getRoomByCode(@PathVariable String code) {
        try {
            WatchPartyDTO room = watchPartyService.findByCode(code);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<?> closeRoom(@PathVariable String code, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Korisnik nije ulogovan.");
        }
        try {
            watchPartyService.closeRoom(code, principal.getName());
            return ResponseEntity.ok("Soba zatvorena");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @MessageMapping("/watch-party/{code}/play")
    @SendTo("/topic/watch-party/{code}")
    public Map<String, Object> playVideo(@DestinationVariable String code, Map<String, Object> message) {
        return message;
    }
}
