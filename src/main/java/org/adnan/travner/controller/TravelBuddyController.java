package org.adnan.travner.controller;

import org.adnan.travner.dto.TravelBuddyDTO;
import org.adnan.travner.dto.TravelBuddyRequest;
import org.adnan.travner.entry.TravelBuddyEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.ExceptionResponses;
import org.adnan.travner.repository.TravelBuddyRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.service.TravelBuddyService;
import org.adnan.travner.util.IdentityNormalizer;
import org.adnan.travner.util.RequestValidation;
import org.adnan.travner.web.ValidObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for travel buddy operations
 */
@RestController
@RequestMapping("/api/travel-buddies")
public class TravelBuddyController {

    private static final Set<String> SORT_FIELDS = Set.of(
            "status", "travelDate", "destination", "createdAt", "updatedAt");

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
            @RequestParam(defaultValue = "20") int size,
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
        Pageable pageable = RequestValidation.pageable(page, size, sortBy, direction, SORT_FIELDS, "createdAt");

        try {
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
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Get travel buddy by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTravelBuddyById(@ValidObjectId("id") String id) {
        try {
            return travelBuddyService.getTravelBuddyById(id)
                    .map(buddy -> ResponseEntity.ok(buddy))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ExceptionResponses.from(e);
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
            Optional<UserEntry> userOpt = Optional.ofNullable(userRepository.findByuserName(IdentityNormalizer.normalizeUsername(username)));
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            
            TravelBuddyDTO buddy = travelBuddyService.createTravelBuddy(request, userOpt.get().getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(buddy);
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Update travel buddy post
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTravelBuddy(
            @ValidObjectId("id") String id,
            @Valid @RequestBody TravelBuddyRequest request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            Optional<UserEntry> userOpt = Optional.ofNullable(userRepository.findByuserName(IdentityNormalizer.normalizeUsername(username)));
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            
            return travelBuddyService.updateTravelBuddy(id, request, userOpt.get().getId())
                    .map(buddy -> ResponseEntity.ok(buddy))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Delete travel buddy post
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTravelBuddy(
            @ValidObjectId("id") String id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            Optional<UserEntry> userOpt = Optional.ofNullable(userRepository.findByuserName(IdentityNormalizer.normalizeUsername(username)));
            
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
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Get travel buddies by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTravelBuddiesByUser(@ValidObjectId("userId") String userId) {
        try {
            List<TravelBuddyDTO> buddies = travelBuddyService.getTravelBuddiesByUser(userId);
            return ResponseEntity.ok(buddies);
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }
}
