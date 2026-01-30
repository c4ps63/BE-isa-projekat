package rs.ac.ftn.isa.isabackend.service;

import org.springframework.stereotype.Service;

@Service
public class TileService {

    // Zoom nivoi za razlicite prikaze
    // HIGH_ZOOM: Prikazuju se svi pojedinacni video snimci
    public static final int HIGH_ZOOM_THRESHOLD = 12;
    // MEDIUM_ZOOM: Video snimci se grupisu u klastere srednje velicine
    public static final int MEDIUM_ZOOM_THRESHOLD = 8;
    // LOW_ZOOM: Prikazuje se samo reprezentativni video po velikoj sekciji
    // Zoom < MEDIUM_ZOOM_THRESHOLD

    // Efektivni zoom nivoi za klasterizaciju - vece sekcije na nizim zoom nivoima
    public static final int EFFECTIVE_ZOOM_LOW = 4;      // Za zoom 0-7: veliki tile-ovi
    public static final int EFFECTIVE_ZOOM_MEDIUM = 8;   // Za zoom 8-11: srednji tile-ovi

    public double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    public double tile2lat(int y, int z) {
        double n = Math.PI - 2.0 * Math.PI * y / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));
    }

    public int getTileX(double lon, int z) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << z));
    }

    public int getTileY(double lat, int z) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * (1 << z));
    }

    /**
     * Odredjuje tip zoom nivoa na osnovu trenutnog zoom-a.
     * @return "HIGH", "MEDIUM", ili "LOW"
     */
    public String getZoomLevel(int zoom) {
        if (zoom >= HIGH_ZOOM_THRESHOLD) {
            return "HIGH";
        } else if (zoom >= MEDIUM_ZOOM_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Vraca efektivni zoom nivo za klasterizaciju.
     * Na nizim zoom nivoima koristi vece tile-ove (manji efektivni zoom).
     */
    public int getEffectiveZoom(int actualZoom) {
        if (actualZoom >= HIGH_ZOOM_THRESHOLD) {
            return actualZoom; // Koristi originalni zoom
        } else if (actualZoom >= MEDIUM_ZOOM_THRESHOLD) {
            return EFFECTIVE_ZOOM_MEDIUM; // Srednji tile-ovi
        } else {
            return EFFECTIVE_ZOOM_LOW; // Veliki tile-ovi
        }
    }

    /**
     * Konvertuje tile koordinate sa veceg zoom nivoa na manji (veci tile).
     */
    public int convertTileCoord(int coord, int fromZoom, int toZoom) {
        if (fromZoom <= toZoom) {
            return coord;
        }
        int zoomDiff = fromZoom - toZoom;
        return coord >> zoomDiff; // Deli sa 2^zoomDiff
    }

    /**
     * Racuna centar tile-a u geografskim koordinatama.
     */
    public double[] getTileCenter(int x, int y, int z) {
        double minLng = tile2lon(x, z);
        double maxLng = tile2lon(x + 1, z);
        double maxLat = tile2lat(y, z);
        double minLat = tile2lat(y + 1, z);

        return new double[] {
            (minLat + maxLat) / 2.0,  // centerLat
            (minLng + maxLng) / 2.0   // centerLng
        };
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