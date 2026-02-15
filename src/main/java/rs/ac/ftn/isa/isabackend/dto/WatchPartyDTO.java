package rs.ac.ftn.isa.isabackend.dto;

import rs.ac.ftn.isa.isabackend.model.WatchParty;

import java.time.LocalDateTime;

public class WatchPartyDTO {

    private Long id;
    private String roomCode;
    private UserDTO creator;
    private LocalDateTime createdAt;
    private boolean active;

    public WatchPartyDTO() {}

    public WatchPartyDTO(WatchParty watchParty) {
        this.id = watchParty.getId();
        this.roomCode = watchParty.getRoomCode();
        this.creator = new UserDTO(watchParty.getCreator());
        this.createdAt = watchParty.getCreatedAt();
        this.active = watchParty.isActive();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public UserDTO getCreator() { return creator; }
    public void setCreator(UserDTO creator) { this.creator = creator; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
