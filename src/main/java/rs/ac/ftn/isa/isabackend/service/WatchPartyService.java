package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.ac.ftn.isa.isabackend.dto.WatchPartyDTO;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.WatchParty;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;
import rs.ac.ftn.isa.isabackend.repository.WatchPartyRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class WatchPartyService {

    private final WatchPartyRepository watchPartyRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Autowired
    public WatchPartyService(WatchPartyRepository watchPartyRepository, UserRepository userRepository) {
        this.watchPartyRepository = watchPartyRepository;
        this.userRepository = userRepository;
    }

    public WatchPartyDTO createRoom(String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronadjen"));

        Optional<WatchParty> existing = watchPartyRepository.findByCreatorIdAndActiveTrue(creator.getId());
        if (existing.isPresent()) {
            return new WatchPartyDTO(existing.get());
        }

        WatchParty watchParty = new WatchParty();
        watchParty.setRoomCode(generateRoomCode());
        watchParty.setCreator(creator);
        watchParty.setCreatedAt(LocalDateTime.now());
        watchParty.setActive(true);

        watchParty = watchPartyRepository.save(watchParty);
        return new WatchPartyDTO(watchParty);
    }

    public WatchPartyDTO findByCode(String code) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(code)
                .orElseThrow(() -> new RuntimeException("Soba nije pronadjena"));

        if (!watchParty.isActive()) {
            throw new RuntimeException("Soba je zatvorena");
        }

        return new WatchPartyDTO(watchParty);
    }

    public void closeRoom(String code, String username) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(code)
                .orElseThrow(() -> new RuntimeException("Soba nije pronadjena"));

        if (!watchParty.getCreator().getUsername().equals(username)) {
            throw new RuntimeException("Samo kreator moze zatvoriti sobu");
        }

        watchParty.setActive(false);
        watchPartyRepository.save(watchParty);
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        if (watchPartyRepository.findByRoomCode(code.toString()).isPresent()) {
            return generateRoomCode();
        }

        return code.toString();
    }
}
