package rs.ac.ftn.isa.isabackend.dto;

/**
 * DTO za prikaz klasterizovanih video snimaka na mapi.
 * Koristi se na nizim zoom nivoima kako bi se smanjio broj markera
 * i poboljsale performanse pri prikazivanju velikog broja videa.
 */
public class TileClusterDTO {

    private Double centerLatitude;
    private Double centerLongitude;
    private Integer videoCount;
    private VideoDTO representativeVideo;
    private Integer tileX;
    private Integer tileY;
    private Integer tileZ;
    private boolean isCluster;

    public TileClusterDTO() {
    }

    public TileClusterDTO(Double centerLatitude, Double centerLongitude, Integer videoCount,
                          VideoDTO representativeVideo, Integer tileX, Integer tileY, Integer tileZ) {
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.videoCount = videoCount;
        this.representativeVideo = representativeVideo;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileZ = tileZ;
        this.isCluster = videoCount > 1;
    }

    public Double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(Double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public Double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(Double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public Integer getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(Integer videoCount) {
        this.videoCount = videoCount;
        this.isCluster = videoCount > 1;
    }

    public VideoDTO getRepresentativeVideo() {
        return representativeVideo;
    }

    public void setRepresentativeVideo(VideoDTO representativeVideo) {
        this.representativeVideo = representativeVideo;
    }

    public Integer getTileX() {
        return tileX;
    }

    public void setTileX(Integer tileX) {
        this.tileX = tileX;
    }

    public Integer getTileY() {
        return tileY;
    }

    public void setTileY(Integer tileY) {
        this.tileY = tileY;
    }

    public Integer getTileZ() {
        return tileZ;
    }

    public void setTileZ(Integer tileZ) {
        this.tileZ = tileZ;
    }

    public boolean isCluster() {
        return isCluster;
    }

    public void setCluster(boolean cluster) {
        isCluster = cluster;
    }
}
