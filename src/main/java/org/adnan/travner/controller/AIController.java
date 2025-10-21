package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.service.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Controller for travel recommendations and chatbot functionality
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AIController {

    private final AIService aiService;

    /**
     * Get travel recommendations based on user preferences
     */
    @PostMapping("/recommendations")
    public ResponseEntity<ApiResponse<String>> getTravelRecommendations(
            @RequestBody AIService.TravelPreferences preferences) {
        
        try {
            String recommendations = aiService.getTravelRecommendations(preferences);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(recommendations)
                    .message("Travel recommendations generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating travel recommendations", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate travel recommendations: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Generic chat endpoint following requested OpenRouter pattern
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<String>> chat(@RequestBody Map<String, Object> body) {
        String message = body != null ? String.valueOf(body.getOrDefault("message", "")) : "";
        String model = body != null ? (String) body.get("model") : null;

        if (message == null || message.isBlank() || "null".equals(message)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("message is required")
                    .build()
            );
        }

        try {
            String answer = aiService.chat(message, model);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(answer)
                    .message("AI response generated")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating AI response", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate AI response: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get itinerary suggestions for a destination
     */
    @GetMapping("/itinerary")
    public ResponseEntity<ApiResponse<String>> getItinerarySuggestions(
            @RequestParam String destination,
            @RequestParam int duration,
            @RequestParam List<String> interests) {
        
        try {
            String itinerary = aiService.getItinerarySuggestions(destination, duration, interests);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(itinerary)
                    .message("Itinerary suggestions generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating itinerary suggestions", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate itinerary suggestions: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get travel buddy matching suggestions
     */
    @PostMapping("/travel-buddy-suggestions")
    public ResponseEntity<ApiResponse<String>> getTravelBuddySuggestions(
            @RequestBody AIService.TravelBuddyProfile userProfile) {
        
        try {
            String suggestions = aiService.getTravelBuddySuggestions(userProfile);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(suggestions)
                    .message("Travel buddy suggestions generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating travel buddy suggestions", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate travel buddy suggestions: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get local food recommendations
     */
    @GetMapping("/food-recommendations")
    public ResponseEntity<ApiResponse<String>> getFoodRecommendations(
            @RequestParam String destination,
            @RequestParam(required = false) List<String> dietaryRestrictions) {
        
        try {
            String recommendations = aiService.getFoodRecommendations(destination, 
                    dietaryRestrictions != null ? dietaryRestrictions : List.of());
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(recommendations)
                    .message("Food recommendations generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating food recommendations", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate food recommendations: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get budget travel tips
     */
    @GetMapping("/budget-tips")
    public ResponseEntity<ApiResponse<String>> getBudgetTravelTips(
            @RequestParam String destination,
            @RequestParam double budget) {
        
        try {
            String tips = aiService.getBudgetTravelTips(destination, budget);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(tips)
                    .message("Budget travel tips generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating budget travel tips", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate budget travel tips: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get cultural insights for a destination
     */
    @GetMapping("/cultural-insights")
    public ResponseEntity<ApiResponse<String>> getCulturalInsights(
            @RequestParam String destination) {
        
        try {
            String insights = aiService.getCulturalInsights(destination);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(insights)
                    .message("Cultural insights generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating cultural insights", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate cultural insights: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get weather and best time to visit information
     */
    @GetMapping("/weather-info")
    public ResponseEntity<ApiResponse<String>> getWeatherAndBestTimeInfo(
            @RequestParam String destination) {
        
        try {
            String info = aiService.getWeatherAndBestTimeInfo(destination);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(info)
                    .message("Weather information generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating weather information", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate weather information: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get safety tips for a destination
     */
    @GetMapping("/safety-tips")
    public ResponseEntity<ApiResponse<String>> getSafetyTips(
            @RequestParam String destination) {
        
        try {
            String tips = aiService.getSafetyTips(destination);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(tips)
                    .message("Safety tips generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating safety tips", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate safety tips: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get transportation options for a destination
     */
    @GetMapping("/transportation")
    public ResponseEntity<ApiResponse<String>> getTransportationInfo(
            @RequestParam String destination) {
        
        try {
            String info = aiService.getTransportationInfo(destination);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(info)
                    .message("Transportation information generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating transportation information", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate transportation information: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get accommodation recommendations
     */
    @GetMapping("/accommodation")
    public ResponseEntity<ApiResponse<String>> getAccommodationRecommendations(
            @RequestParam String destination,
            @RequestParam double budget,
            @RequestParam String accommodationType) {
        
        try {
            String recommendations = aiService.getAccommodationRecommendations(destination, budget, accommodationType);
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .data(recommendations)
                    .message("Accommodation recommendations generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating accommodation recommendations", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate accommodation recommendations: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get comprehensive destination guide
     */
    @GetMapping("/destination-guide")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDestinationGuide(
            @RequestParam String destination,
            @RequestParam(required = false) Double budget) {
        
        try {
            Map<String, String> guide = new HashMap<>();
            
            guide.put("culturalInsights", aiService.getCulturalInsights(destination));
            guide.put("weatherInfo", aiService.getWeatherAndBestTimeInfo(destination));
            guide.put("safetyTips", aiService.getSafetyTips(destination));
            guide.put("transportation", aiService.getTransportationInfo(destination));
            guide.put("foodRecommendations", aiService.getFoodRecommendations(destination, List.of()));
            guide.put("budgetTips", budget != null ? aiService.getBudgetTravelTips(destination, budget) : 
                "Budget information not provided");
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, String>>builder()
                    .success(true)
                    .data(guide)
                    .message("Comprehensive destination guide generated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating destination guide", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, String>>builder()
                    .success(false)
                    .message("Failed to generate destination guide: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Health check for AI service
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(
            ApiResponse.<String>builder()
                .success(true)
                .data("AI service is running")
                .message("AI service health check successful")
                .build()
        );
    }
}



