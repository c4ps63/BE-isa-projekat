package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.ac.ftn.isa.isabackend.dto.LocationDTO;
import rs.ac.ftn.isa.isabackend.model.Location;
import rs.ac.ftn.isa.isabackend.repository.LocationRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    public List<LocationDTO> getLocationsInView(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        List<Location> locations = locationRepository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLng, maxLng);
        List<LocationDTO> dtos = new ArrayList<>();

        for (Location loc : locations) {
            dtos.add(new LocationDTO(loc.getId(), loc.getName(), loc.getLatitude(), loc.getLongitude()));
        }
        return dtos;
    }

    public Location saveLocation(LocationDTO dto) {
        Location location = new Location();
        location.setName(dto.getName());
        location.setLatitude(dto.getLatitude());
        location.setLongitude(dto.getLongitude());

        return locationRepository.save(location);
    }
}