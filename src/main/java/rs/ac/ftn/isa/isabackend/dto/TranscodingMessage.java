package rs.ac.ftn.isa.isabackend.dto;

import java.io.Serializable;

public class TranscodingMessage implements Serializable {

    private Long videoId;
    private String inputPath;
    private String outputFormat;
    private String resolution;

    public TranscodingMessage() {
    }

    public TranscodingMessage(Long videoId, String inputPath, String outputFormat, String resolution) {
        this.videoId = videoId;
        this.inputPath = inputPath;
        this.outputFormat = outputFormat;
        this.resolution = resolution;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
}
