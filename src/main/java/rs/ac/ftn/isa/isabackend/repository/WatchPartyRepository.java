package rs.ac.ftn.isa.isabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.ftn.isa.isabackend.model.WatchParty;

import java.util.Optional;

public interface WatchPartyRepository extends JpaRepository<WatchParty, Long> {

    Optional<WatchParty> findByRoomCode(String roomCode);

    Optional<WatchParty> findByCreatorIdAndActiveTrue(Long creatorId);
}
