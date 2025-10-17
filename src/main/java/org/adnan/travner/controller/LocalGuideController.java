package org.adnan.travner.controller;

import org.adnan.travner.dto.LocalGuideDTO;
import org.adnan.travner.dto.LocalGuideRequest;
import org.adnan.travner.entry.LocalGuideEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.LocalGuideRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.service.LocalGuideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Controller for local guide operations
 */
@RestController
@RequestMapping("/api/local-guides")
@CrossOrigin(origins = "*")
public class LocalGuideController {

    @Autowired
    private LocalGuideService localGuideService;

    @Autowired
    private LocalGuideRepository localGuideRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all local guides with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<?> getAllLocalGuides(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<String> languages,
            @RequestParam(required = false) List<String> specialties,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean isAvailable
    ) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<LocalGuideEntry> guides;
            
            if (query != null && !query.trim().isEmpty()) {
                guides = localGuideRepository.searchGuides(query, pageable);
            } else if (location != null && !location.trim().isEmpty()) {
                guides = localGuideRepository.findByLocationContainingIgnoreCaseAndIsAvailableTrue(location, pageable);
            } else if (languages != null && !languages.isEmpty()) {
                guides = localGuideRepository.findByLanguagesInAndIsAvailableTrue(languages, pageable);
            } else if (specialties != null && !specialties.isEmpty()) {
                guides = localGuideRepository.findBySpecialtiesInAndIsAvailableTrue(specialties, pageable);
            } else if (minRating != null && maxRating != null) {
                guides = localGuideRepository.findByRatingRange(minRating, maxRating, pageable);
            } else if (minPrice != null && maxPrice != null) {
                guides = localGuideRepository.findByPriceRange(minPrice, maxPrice, pageable);
            } else if (isAvailable != null) {
                guides = localGuideRepository.findByIsAvailableTrueOrderByRatingDesc(pageable);
            } else {
                guides = localGuideRepository.findByIsAvailableTrueOrderByRatingDesc(pageable);
            }
            
            return ResponseEntity.ok(guides.map(localGuideService::convertToDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving local guides: " + e.getMessage());
        }
    }

    /**
     * Get local guide by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getLocalGuideById(@PathVariable String id) {
        try {
            return localGuideService.getLocalGuideById(id)
                    .map(guide -> ResponseEntity.ok(guide))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving local guide: " + e.getMessage());
        }
    }

    /**
     * Create a new local guide profile
     */
    @PostMapping
    public ResponseEntity<?> createLocalGuide(
            @Valid @RequestBody LocalGuideRequest request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            Optional<UserEntry> userOpt = Optional.ofNullable(userRepository.findByuserName(username));
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            
            // Check if user already has a guide profile
            if (localGuideRepository.findByUser_Id(userOpt.get().getId()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("User already has a local guide profile");
            }
            
            LocalGuideDTO guide = localGuideService.createLocalGuide(request, userOpt.get().getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(guide);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating local guide: " + e.getMessage());
        }
    }

    /**
     * Update local guide profile
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLocalGuide(
            @PathVariable String id,
            @Valid @RequestBody LocalGuideRequest request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            Optional<UserEntry> userOpt = Optional.ofNullable(userRepository.findByuserName(username));
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            
            return localGuideService.updateLocalGuide(id, request, userOpt.get().getId())
                    .map(guide -> ResponseEntity.ok(guide))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating local guide: " + e.getMessage());
        }
    }

    /**
     * Delete local guide profile
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLocalGuide(
            @PathVariable String id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            Optional<UserEntry> userOpt = Optional.ofNullable(userRepository.findByuserName(username));
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            
            boolean deleted = localGuideService.deleteLocalGuide(id, userOpt.get().getId());
            if (deleted) {
                return ResponseEntity.ok().body("Local guide profile deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting local guide: " + e.getMessage());
        }
    }

    /**
     * Get local guides by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getLocalGuidesByUser(@PathVariable String userId) {
        try {
            List<LocalGuideDTO> guides = localGuideService.getLocalGuidesByUser(userId);
            return ResponseEntity.ok(guides);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user's local guides: " + e.getMessage());
        }
    }
}
