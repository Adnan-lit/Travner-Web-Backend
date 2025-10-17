package org.adnan.travner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.ItineraryDTO;
import org.adnan.travner.dto.ItineraryRequest;
import org.adnan.travner.service.ItineraryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for itinerary operations
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
@Slf4j
public class ItineraryController {

    private final ItineraryService itineraryService;

    /**
     * Get all public itineraries with pagination and sorting
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getAllItineraries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

            Page<ItineraryDTO> itineraries = itineraryService.getPublicItineraries(pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            log.error("Error retrieving itineraries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve itineraries: " + e.getMessage()));
        }
    }

    /**
     * Get itinerary by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItineraryDTO>> getItineraryById(@PathVariable String id) {
        try {
            Optional<ItineraryDTO> itinerary = itineraryService.getItineraryById(id);
            if (itinerary.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(itinerary.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Itinerary not found with ID: " + id));
            }
        } catch (Exception e) {
            log.error("Error retrieving itinerary {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve itinerary: " + e.getMessage()));
        }
    }

    /**
     * Get itineraries by destination
     */
    @GetMapping("/destination/{destination}")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getItinerariesByDestination(
            @PathVariable String destination,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ItineraryDTO> itineraries = itineraryService.getItinerariesByDestination(destination, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            log.error("Error retrieving itineraries by destination {}: {}", destination, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get itineraries by destination: " + e.getMessage()));
        }
    }

    /**
     * Get itineraries by user
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getItinerariesByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ItineraryDTO> itineraries = itineraryService.getItinerariesByUser(username, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            log.error("Error retrieving itineraries by user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get itineraries by user: " + e.getMessage()));
        }
    }

    /**
     * Search itineraries
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> searchItineraries(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ItineraryDTO> itineraries = itineraryService.searchItineraries(query, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            log.error("Error searching itineraries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to search itineraries: " + e.getMessage()));
        }
    }

    /**
     * Get itineraries by tags
     */
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getItinerariesByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ItineraryDTO> itineraries = itineraryService.getItinerariesByTags(tags, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            log.error("Error retrieving itineraries by tags {}: {}", tags, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get itineraries by tags: " + e.getMessage()));
        }
    }

    /**
     * Get template itineraries
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getTemplateItineraries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ItineraryDTO> itineraries = itineraryService.getTemplateItineraries(pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            log.error("Error retrieving template itineraries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get template itineraries: " + e.getMessage()));
        }
    }

    /**
     * Create new itinerary
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ItineraryDTO>> createItinerary(
            Authentication authentication,
            @Valid @RequestBody ItineraryRequest request) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            ItineraryDTO createdItinerary = itineraryService.createItinerary(authentication.getName(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Itinerary created successfully", createdItinerary));
        } catch (Exception e) {
            log.error("Error creating itinerary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create itinerary: " + e.getMessage()));
        }
    }

    /**
     * Update itinerary
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItineraryDTO>> updateItinerary(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody ItineraryRequest request) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            ItineraryDTO updatedItinerary = itineraryService.updateItinerary(id, authentication.getName(), request);
            return ResponseEntity.ok(ApiResponse.success("Itinerary updated successfully", updatedItinerary));
        } catch (RuntimeException e) {
            log.error("Error updating itinerary {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to update itinerary: " + e.getMessage()));
        }
    }

    /**
     * Delete itinerary
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteItinerary(
            Authentication authentication,
            @PathVariable String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            itineraryService.deleteItinerary(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Itinerary deleted successfully", null));
        } catch (RuntimeException e) {
            log.error("Error deleting itinerary {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to delete itinerary: " + e.getMessage()));
        }
    }

    /**
     * Like itinerary
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<ItineraryDTO>> likeItinerary(
            Authentication authentication,
            @PathVariable String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            ItineraryDTO updatedItinerary = itineraryService.likeItinerary(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Itinerary liked successfully", updatedItinerary));
        } catch (RuntimeException e) {
            log.error("Error liking itinerary {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to like itinerary: " + e.getMessage()));
        }
    }
}
