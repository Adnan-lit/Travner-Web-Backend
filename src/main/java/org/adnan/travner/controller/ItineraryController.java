package org.adnan.travner.controller;

import org.adnan.travner.exception.ExceptionResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.ItineraryDTO;
import org.adnan.travner.dto.ItineraryRequest;
import org.adnan.travner.service.ItineraryService;
import org.adnan.travner.util.RequestValidation;
import org.adnan.travner.web.ValidObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for itinerary operations
 */
@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
@Slf4j
public class ItineraryController {

    private static final Set<String> SORT_FIELDS = Set.of(
            "title", "description", "destination", "startDate", "isPublic",
            "isTemplate", "createdAt", "updatedAt");

    private final ItineraryService itineraryService;

    /**
     * Get all public itineraries with pagination and sorting
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getAllItineraries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = RequestValidation.pageable(page, size, sortBy, direction, SORT_FIELDS, "createdAt");

        try {
            Page<ItineraryDTO> itineraries = itineraryService.getPublicItineraries(pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Get itinerary by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItineraryDTO>> getItineraryById(@ValidObjectId("id") String id) {
        try {
            Optional<ItineraryDTO> itinerary = itineraryService.getItineraryById(id);
            if (itinerary.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(itinerary.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Itinerary not found with ID: " + id));
            }
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Get itineraries by destination
     */
    @GetMapping("/destination/{destination}")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getItinerariesByDestination(
            @PathVariable String destination,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = RequestValidation.pageable(page, size);

        try {
            Page<ItineraryDTO> itineraries = itineraryService.getItinerariesByDestination(destination, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Get itineraries by user
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getItinerariesByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = RequestValidation.pageable(page, size);

        try {
            Page<ItineraryDTO> itineraries = itineraryService.getItinerariesByUser(username, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Search itineraries
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> searchItineraries(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = RequestValidation.pageable(page, size);

        try {
            Page<ItineraryDTO> itineraries = itineraryService.searchItineraries(query, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Get itineraries by tags
     */
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getItinerariesByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = RequestValidation.pageable(page, size);

        try {
            Page<ItineraryDTO> itineraries = itineraryService.getItinerariesByTags(tags, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Get template itineraries
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<ItineraryDTO>>> getTemplateItineraries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = RequestValidation.pageable(page, size);

        try {
            Page<ItineraryDTO> itineraries = itineraryService.getTemplateItineraries(pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(itineraries));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
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
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Update itinerary
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItineraryDTO>> updateItinerary(
            Authentication authentication,
            @ValidObjectId("id") String id,
            @Valid @RequestBody ItineraryRequest request) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            ItineraryDTO updatedItinerary = itineraryService.updateItinerary(id, authentication.getName(), request);
            return ResponseEntity.ok(ApiResponse.success("Itinerary updated successfully", updatedItinerary));
        } catch (RuntimeException e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Delete itinerary
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteItinerary(
            Authentication authentication,
            @ValidObjectId("id") String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            itineraryService.deleteItinerary(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Itinerary deleted successfully", null));
        } catch (RuntimeException e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Like itinerary
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<ItineraryDTO>> likeItinerary(
            Authentication authentication,
            @ValidObjectId("id") String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            ItineraryDTO updatedItinerary = itineraryService.likeItinerary(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Itinerary liked successfully", updatedItinerary));
        } catch (RuntimeException e) {
            return ExceptionResponses.from(e);
        }
    }
}
