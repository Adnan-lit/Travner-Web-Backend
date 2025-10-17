package org.adnan.travner.controller;

import org.adnan.travner.dto.TravelBuddyDTO;
import org.adnan.travner.dto.TravelBuddyRequest;
import org.adnan.travner.entry.TravelBuddyEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.TravelBuddyRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.service.TravelBuddyService;
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
 * Controller for travel buddy operations
 */
@RestController
@RequestMapping("/api/travel-buddies")
@CrossOrigin(origins = "*")
public class TravelBuddyController {

    @Autowired
    private TravelBuddyService travelBuddyService;

    @Autowired
    private TravelBuddyRepository travelBuddyRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all travel buddy posts with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<?> getAllTravelBuddies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<String> interests,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String preferredGender,
            @RequestParam(required = false) String status
    ) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<TravelBuddyEntry> buddies;
            
            if (query != null && !query.trim().isEmpty()) {
                buddies = travelBuddyRepository.searchBuddies(query, pageable);
            } else if (destination != null && !destination.trim().isEmpty()) {
                buddies = travelBuddyRepository.findByDestinationContainingIgnoreCaseAndStatus(destination, TravelBuddyEntry.BuddyStatus.ACTIVE, pageable);
            } else if (interests != null && !interests.isEmpty()) {
                buddies = travelBuddyRepository.findByInterestsInAndStatus(interests, "ACTIVE", pageable);
            } else if (status != null) {
                buddies = travelBuddyRepository.findByStatus(status, pageable);
            } else {
                buddies = travelBuddyRepository.findByStatus("ACTIVE", pageable);
            }
            
            return ResponseEntity.ok(buddies.map(travelBuddyService::convertToDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving travel buddies: " + e.getMessage());
        }
    }

    /**
     * Get travel buddy by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTravelBuddyById(@PathVariable String id) {
        try {
            return travelBuddyService.getTravelBuddyById(id)
                    .map(buddy -> ResponseEntity.ok(buddy))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving travel buddy: " + e.getMessage());
        }
    }

    /**
     * Create a new travel buddy post
     */
    @PostMapping
    public ResponseEntity<?> createTravelBuddy(
            @Valid @RequestBody TravelBuddyRequest request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            Optional<UserEntry> userOpt = Optional.ofNullable(userRepository.findByuserName(username));
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            
            TravelBuddyDTO buddy = travelBuddyService.createTravelBuddy(request, userOpt.get().getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(buddy);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating travel buddy: " + e.getMessage());
        }
    }

    /**
     * Update travel buddy post
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTravelBuddy(
            @PathVariable String id,
            @Valid @RequestBody TravelBuddyRequest request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            Optional<UserEntry> userOpt = Optional.ofNullable(userRepository.findByuserName(username));
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            
            return travelBuddyService.updateTravelBuddy(id, request, userOpt.get().getId())
                    .map(buddy -> ResponseEntity.ok(buddy))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating travel buddy: " + e.getMessage());
        }
    }

    /**
     * Delete travel buddy post
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTravelBuddy(
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
            
            boolean deleted = travelBuddyService.deleteTravelBuddy(id, userOpt.get().getId());
            if (deleted) {
                return ResponseEntity.ok().body("Travel buddy post deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting travel buddy: " + e.getMessage());
        }
    }

    /**
     * Get travel buddies by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTravelBuddiesByUser(@PathVariable String userId) {
        try {
            List<TravelBuddyDTO> buddies = travelBuddyService.getTravelBuddiesByUser(userId);
            return ResponseEntity.ok(buddies);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user's travel buddies: " + e.getMessage());
        }
    }
}
