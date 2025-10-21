package org.adnan.travner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI Service for integrating with OpenRouter API for travel recommendations
 * Uses standard Java HTTP Client to avoid WebFlux/Netty dependency conflicts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Value("${app.ai.openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${app.ai.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;

    // Free models available on OpenRouter
    private static final List<String> FREE_MODELS = List.of(
        "mistralai/mistral-7b-instruct:free",
        "microsoft/phi-3-mini-128k-instruct:free",
        "meta-llama/llama-3.2-3b-instruct:free",
        "google/gemma-2-2b-it:free"
    );

    /**
     * Get travel recommendations based on user preferences
     */
    @Cacheable(value = "travelRecommendations", key = "#preferences.hashCode()")
    public String getTravelRecommendations(TravelPreferences preferences) {
        String prompt = buildTravelRecommendationPrompt(preferences);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(0));
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return "mistralai/mistral-7b-instruct"; // default if not provided
        }
        // Remove trailing ":free" if caller passes free tag; OpenRouter accepts base name too
        if (model.endsWith(":free")) {
            return model.substring(0, model.length() - 5);
        }
        return model;
    }

    /**
     * Get itinerary suggestions for a destination
     */
    @Cacheable(value = "itinerarySuggestions", key = "#destination + '_' + #duration + '_' + #interests.hashCode()")
    public String getItinerarySuggestions(String destination, int duration, List<String> interests) {
        String prompt = buildItineraryPrompt(destination, duration, interests);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(1));
    }

    /**
     * Get travel buddy matching suggestions
     */
    @Cacheable(value = "travelBuddySuggestions", key = "#userProfile.hashCode()")
    public String getTravelBuddySuggestions(TravelBuddyProfile userProfile) {
        String prompt = buildTravelBuddyPrompt(userProfile);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(2));
    }

    /**
     * Get local food recommendations
     */
    @Cacheable(value = "foodRecommendations", key = "#destination + '_' + #dietaryRestrictions.hashCode()")
    public String getFoodRecommendations(String destination, List<String> dietaryRestrictions) {
        String prompt = buildFoodRecommendationPrompt(destination, dietaryRestrictions);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(3));
    }

    /**
     * Get budget travel tips
     */
    @Cacheable(value = "budgetTips", key = "#destination + '_' + #budget")
    public String getBudgetTravelTips(String destination, double budget) {
        String prompt = buildBudgetTipsPrompt(destination, budget);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(0));
    }

    /**
     * Get cultural insights for a destination
     */
    @Cacheable(value = "culturalInsights", key = "#destination")
    public String getCulturalInsights(String destination) {
        String prompt = buildCulturalInsightsPrompt(destination);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(1));
    }

    /**
     * Get weather and best time to visit information
     */
    @Cacheable(value = "weatherInfo", key = "#destination")
    public String getWeatherAndBestTimeInfo(String destination) {
        String prompt = buildWeatherInfoPrompt(destination);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(2));
    }

    /**
     * Get safety tips for a destination
     */
    @Cacheable(value = "safetyTips", key = "#destination")
    public String getSafetyTips(String destination) {
        String prompt = buildSafetyTipsPrompt(destination);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(3));
    }

    /**
     * Get transportation options for a destination
     */
    @Cacheable(value = "transportationInfo", key = "#destination")
    public String getTransportationInfo(String destination) {
        String prompt = buildTransportationPrompt(destination);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(0));
    }

    /**
     * Generic chat endpoint: send a raw user message and optional model
     */
    public String chat(String userMessage, String model) {
        String chosenModel = (model == null || model.isBlank()) ? FREE_MODELS.get(0) : model;
        return callOpenRouterAPI(userMessage, chosenModel);
    }

    /**
     * Get accommodation recommendations
     */
    @Cacheable(value = "accommodationRecommendations", key = "#destination + '_' + #budget + '_' + #accommodationType")
    public String getAccommodationRecommendations(String destination, double budget, String accommodationType) {
        String prompt = buildAccommodationPrompt(destination, budget, accommodationType);
        return callOpenRouterAPI(prompt, FREE_MODELS.get(1));
    }

    /**
     * Call OpenRouter API using standard Java HTTP Client
     */
    private String callOpenRouterAPI(String prompt, String model) {
        if (openRouterApiKey == null || openRouterApiKey.isEmpty()) {
            return "AI service is not configured. Please add OPENROUTER_API_KEY to your environment variables.";
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", normalizeModel(model));
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a Bangladesh travel guide bot providing helpful travel advice."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(openRouterBaseUrl + "/chat/completions"))
                .header("Authorization", "Bearer " + openRouterApiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://travner.com")
                .header("X-Title", "Travner AI Assistant")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

            int maxRetries = 3;
            int retryDelay = 2000; // 2 seconds

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        return parseOpenRouterResponse(response.body());
                    } else if (response.statusCode() >= 500 && attempt < maxRetries) {
                        // Retry on server errors
                        log.warn("OpenRouter API returned {} on attempt {}, retrying...", response.statusCode(), attempt);
                        Thread.sleep(retryDelay * attempt);
                        continue;
                    } else {
                        log.error("OpenRouter API Error: Status {}, Body: {}", response.statusCode(), response.body());
                        return "Sorry, I'm currently experiencing technical difficulties. Please try again later.";
                    }
                } catch (java.io.IOException | InterruptedException e) {
                    if (attempt < maxRetries) {
                        log.warn("Network error on attempt {}, retrying...", attempt, e);
                        Thread.sleep(retryDelay * attempt);
                        continue;
                    }
                    throw e;
                }
            }

            return "Sorry, I'm currently unavailable after multiple retry attempts. Please try again later.";

        } catch (Exception e) {
            log.error("Error calling OpenRouter API", e);
            return "Sorry, I encountered an error processing your request. Please try again.";
        }
    }

    /**
     * Parse OpenRouter API response
     */
    private String parseOpenRouterResponse(String responseBody) {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode choices = response.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
            
            log.error("Unexpected response format from OpenRouter: {}", responseBody);
            return "Sorry, I couldn't process the response. Please try again.";
            
        } catch (Exception e) {
            log.error("Error parsing AI response", e);
            return "Sorry, I encountered an error parsing the response. Please try again.";
        }
    }

    private String buildTravelRecommendationPrompt(TravelPreferences preferences) {
        return String.format("""
            You are a travel expert specializing in Bangladesh and global destinations. 
            Help the user plan their trip based on these preferences:
            - Destination: %s
            - Budget: %s
            - Duration: %s
            - Interests: %s
            - Travel Style: %s
            - Age Group: %s
            
            Provide specific, actionable recommendations including:
            1. Best places to visit
            2. Local experiences to try
            3. Budget-friendly options
            4. Cultural insights
            5. Practical tips for Bangladeshi travelers
            6. Local food recommendations
            7. Transportation options
            8. Safety considerations
            
            Keep your response conversational, helpful, and focused on practical advice for travelers from Bangladesh.
            """, 
            preferences.getDestination() != null ? preferences.getDestination() : "Not specified",
            preferences.getBudget() != null ? "$" + preferences.getBudget() : "Not specified",
            preferences.getDuration() != null ? preferences.getDuration() + " days" : "Not specified",
            preferences.getInterests() != null ? String.join(", ", preferences.getInterests()) : "Not specified",
            preferences.getTravelStyle() != null ? preferences.getTravelStyle() : "Not specified",
            preferences.getAgeGroup() != null ? preferences.getAgeGroup() : "Not specified"
        );
    }

    private String buildItineraryPrompt(String destination, int duration, List<String> interests) {
        return String.format("""
            Create a detailed %d-day itinerary for %s focusing on these interests: %s.
            
            Include:
            1. Day-by-day breakdown with specific timings
            2. Specific attractions and activities
            3. Local food recommendations with restaurant names
            4. Transportation options and costs
            5. Budget estimates in BDT (Bangladeshi Taka)
            6. Cultural tips for Bangladeshi travelers
            7. Best times to visit each location
            8. Alternative activities for different weather conditions
            9. Local customs and etiquette
            10. Emergency contacts and useful phrases
            
            Make it practical, enjoyable, and culturally sensitive for travelers from Bangladesh.
            """, duration, destination, String.join(", ", interests));
    }

    private String buildTravelBuddyPrompt(TravelBuddyProfile userProfile) {
        return String.format("""
            Help find the perfect travel buddy for a %d-year-old who enjoys: %s.
            
            Travel style: %s
            Interested destinations: %s
            Experience level: %s
            
            Provide suggestions for:
            1. Compatible personality types
            2. Shared interest activities
            3. Communication tips
            4. Safety considerations
            5. How to approach potential travel buddies
            6. Red flags to watch out for
            7. Building trust and friendship
            8. Managing expectations
            9. Conflict resolution strategies
            10. Cultural sensitivity tips
            
            Focus on creating meaningful connections within the Bangladeshi travel community.
            """, 
            userProfile.getAge(),
            String.join(", ", userProfile.getInterests()),
            userProfile.getTravelStyle(),
            String.join(", ", userProfile.getDestinations()),
            userProfile.getExperienceLevel()
        );
    }

    private String buildFoodRecommendationPrompt(String destination, List<String> dietaryRestrictions) {
        return String.format("""
            Provide comprehensive food recommendations for %s, considering these dietary restrictions: %s.
            
            Include:
            1. Must-try local dishes
            2. Best restaurants and street food spots
            3. Food safety tips
            4. Cultural dining etiquette
            5. Budget-friendly options
            6. Vegetarian/vegan alternatives
            7. Halal food options
            8. Local markets to visit
            9. Cooking classes or food tours
            10. Food-related cultural insights
            
            Focus on authentic experiences that Bangladeshi travelers would appreciate.
            """, destination, String.join(", ", dietaryRestrictions));
    }

    private String buildBudgetTipsPrompt(String destination, double budget) {
        return String.format("""
            Provide budget travel tips for visiting %s with a budget of $%.2f.
            
            Include:
            1. Budget breakdown (accommodation, food, transport, activities)
            2. Money-saving strategies
            3. Free or low-cost activities
            4. Budget accommodation options
            5. Local transportation tips
            6. Food budget optimization
            7. Shopping and bargaining tips
            8. Currency exchange advice
            9. Hidden costs to avoid
            10. Emergency fund recommendations
            
            Tailor advice specifically for Bangladeshi travelers.
            """, destination, budget);
    }

    private String buildCulturalInsightsPrompt(String destination) {
        return String.format("""
            Provide cultural insights and etiquette guide for %s.
            
            Include:
            1. Cultural norms and customs
            2. Dress codes and appropriate attire
            3. Greeting customs
            4. Religious considerations
            5. Social etiquette
            6. Business etiquette (if applicable)
            7. Photography guidelines
            8. Gift-giving customs
            9. Language tips and useful phrases
            10. Cultural do's and don'ts
            
            Focus on helping Bangladeshi travelers navigate cultural differences respectfully.
            """, destination);
    }

    private String buildWeatherInfoPrompt(String destination) {
        return String.format("""
            Provide weather information and best time to visit %s.
            
            Include:
            1. Climate overview
            2. Best months to visit
            3. Weather by season
            4. What to pack for different seasons
            5. Weather-related activities
            6. Monsoon/rainy season considerations
            7. Extreme weather warnings
            8. Indoor alternatives for bad weather
            9. Weather apps and resources
            10. Local weather patterns and microclimates
            
            Provide practical advice for Bangladeshi travelers.
            """, destination);
    }

    private String buildSafetyTipsPrompt(String destination) {
        return String.format("""
            Provide comprehensive safety tips for traveling to %s.
            
            Include:
            1. General safety guidelines
            2. Areas to avoid
            3. Transportation safety
            4. Scam awareness
            5. Emergency contacts
            6. Health and medical considerations
            7. Personal security tips
            8. Digital safety
            9. Travel insurance recommendations
            10. Local emergency services
            
            Focus on practical safety advice for Bangladeshi travelers.
            """, destination);
    }

    private String buildTransportationPrompt(String destination) {
        return String.format("""
            Provide transportation options and tips for %s.
            
            Include:
            1. Airport transfers
            2. Public transportation options
            3. Taxi and ride-sharing services
            4. Car rental considerations
            5. Walking and cycling options
            6. Inter-city transportation
            7. Cost comparisons
            8. Safety considerations
            9. Accessibility options
            10. Local transportation apps
            
            Provide practical transportation advice for Bangladeshi travelers.
            """, destination);
    }

    private String buildAccommodationPrompt(String destination, double budget, String accommodationType) {
        return String.format("""
            Provide accommodation recommendations for %s with a budget of $%.2f, preferring %s.
            
            Include:
            1. Budget-friendly options
            2. Location recommendations
            3. Booking tips and platforms
            4. Safety considerations
            5. Amenities to look for
            6. Alternative accommodation types
            7. Group vs solo traveler options
            8. Cultural considerations
            9. Booking cancellation policies
            10. Local vs international chains
            
            Tailor recommendations for Bangladeshi travelers.
            """, destination, budget, accommodationType);
    }

    // Data classes for AI service
    public static class TravelPreferences {
        private String destination;
        private Double budget;
        private Integer duration;
        private List<String> interests;
        private String travelStyle;
        private String ageGroup;

        // Getters and setters
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public Double getBudget() { return budget; }
        public void setBudget(Double budget) { this.budget = budget; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
        public List<String> getInterests() { return interests; }
        public void setInterests(List<String> interests) { this.interests = interests; }
        public String getTravelStyle() { return travelStyle; }
        public void setTravelStyle(String travelStyle) { this.travelStyle = travelStyle; }
        public String getAgeGroup() { return ageGroup; }
        public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    }

    public static class TravelBuddyProfile {
        private int age;
        private List<String> interests;
        private String travelStyle;
        private List<String> destinations;
        private String experienceLevel;

        // Getters and setters
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public List<String> getInterests() { return interests; }
        public void setInterests(List<String> interests) { this.interests = interests; }
        public String getTravelStyle() { return travelStyle; }
        public void setTravelStyle(String travelStyle) { this.travelStyle = travelStyle; }
        public List<String> getDestinations() { return destinations; }
        public void setDestinations(List<String> destinations) { this.destinations = destinations; }
        public String getExperienceLevel() { return experienceLevel; }
        public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    }
}



