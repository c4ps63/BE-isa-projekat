package rs.ac.ftn.isa.isabackend.dto;

public class ChatMessageDTO {

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }

    private MessageType type;
    private String content;
    private String senderUsername;
    private String senderAvatarUrl;
    private long timestamp;

    public ChatMessageDTO() {}

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getSenderAvatarUrl() { return senderAvatarUrl; }
    public void setSenderAvatarUrl(String senderAvatarUrl) { this.senderAvatarUrl = senderAvatarUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
