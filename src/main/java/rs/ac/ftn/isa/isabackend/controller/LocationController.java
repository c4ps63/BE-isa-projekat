package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.dto.LocationDTO;
import rs.ac.ftn.isa.isabackend.service.LocationService;

import java.util.List;

@RestController
@RequestMapping(value = "/api/locations")
@CrossOrigin(origins = "http://localhost:4200")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @GetMapping("/viewport")
    public ResponseEntity<List<LocationDTO>> getByViewport(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLng) {

        return ResponseEntity.ok(locationService.getLocationsInView(minLat, maxLat, minLng, maxLng));
    }


    @PostMapping
    public ResponseEntity<String> create(@RequestBody LocationDTO dto) {
        locationService.saveLocation(dto);
        return ResponseEntity.ok("Lokacija uspešno kreirana!");
    }
}