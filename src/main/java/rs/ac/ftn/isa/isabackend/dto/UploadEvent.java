package rs.ac.ftn.isa.isabackend.dto;

import java.io.Serializable;

public class UploadEvent implements Serializable {
    private Long videoId;
    private String title;
    private String fileName;
    private long fileSizeBytes;
    private String authorUsername;
    private String uploadTimestamp;
    private String videoFormat;
    private String location;
    private double latitude;
    private double longitude;
    private int durationSeconds;
    private String description;

    public UploadEvent() {}

    public UploadEvent(Long videoId, String title, String fileName, long fileSizeBytes,
                       String authorUsername, String uploadTimestamp, String videoFormat,
                       String location, double latitude, double longitude,
                       int durationSeconds, String description) {
        this.videoId = videoId;
        this.title = title;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.authorUsername = authorUsername;
        this.uploadTimestamp = uploadTimestamp;
        this.videoFormat = videoFormat;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.durationSeconds = durationSeconds;
        this.description = description;
    }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(String uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public String getVideoFormat() { return videoFormat; }
    public void setVideoFormat(String videoFormat) { this.videoFormat = videoFormat; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
