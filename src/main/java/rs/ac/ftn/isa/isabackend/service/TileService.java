package rs.ac.ftn.isa.isabackend.service;

import org.springframework.stereotype.Service;

@Service
public class TileService {

    public double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    public double tile2lat(int y, int z) {
        double n = Math.PI - 2.0 * Math.PI * y / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));
    }

    public static class BoundingBox {
        public double minLat;
        public double maxLat;
        public double minLng;
        public double maxLng;

        public BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLng = minLng;
            this.maxLng = maxLng;
        }
    }

    public BoundingBox getBoundingBox(int x, int y, int z) {
        double minLng = tile2lon(x, z);
        double maxLng = tile2lon(x + 1, z);
        double maxLat = tile2lat(y, z);
        double minLat = tile2lat(y + 1, z);

        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }
}