package rs.ac.ftn.isa.isabackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watch_parties")
public class WatchParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", unique = true, nullable = false, length = 6)
    private String roomCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public WatchParty() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
