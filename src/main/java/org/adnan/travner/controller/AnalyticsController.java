package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Analytics Controller for platform metrics and user engagement tracking
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get platform overview statistics
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformOverview() {
        try {
            Map<String, Object> overview = analyticsService.getPlatformOverview();
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(overview)
                    .message("Platform overview retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting platform overview", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to retrieve platform overview")
                    .build()
            );
        }
    }

    /**
     * Get user engagement metrics
     */
    @GetMapping("/user/{userId}/engagement")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserEngagement(@PathVariable String userId) {
        try {
            Map<String, Object> engagement = analyticsService.getUserEngagement(userId);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(engagement)
                    .message("User engagement metrics retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting user engagement for user: {}", userId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to retrieve user engagement metrics")
                    .build()
            );
        }
    }

    /**
     * Get content performance metrics
     */
    @GetMapping("/content/{contentType}/performance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContentPerformance(
            @PathVariable String contentType,
            @RequestParam(defaultValue = "week") String timeframe) {
        try {
            Map<String, Object> performance = analyticsService.getContentPerformance(contentType, timeframe);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(performance)
                    .message("Content performance metrics retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting content performance for type: {} and timeframe: {}", contentType, timeframe, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to retrieve content performance metrics")
                    .build()
            );
        }
    }

    /**
     * Get trending content
     */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrendingContent(
            @RequestParam(defaultValue = "week") String timeframe) {
        try {
            Map<String, Object> trending = analyticsService.getTrendingContent(timeframe);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(trending)
                    .message("Trending content retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting trending content for timeframe: {}", timeframe, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to retrieve trending content")
                    .build()
            );
        }
    }

    /**
     * Get user activity timeline
     */
    @GetMapping("/user/{userId}/activity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserActivityTimeline(
            @PathVariable String userId,
            @RequestParam(defaultValue = "week") String timeframe) {
        try {
            List<Map<String, Object>> activities = analyticsService.getUserActivityTimeline(userId, timeframe);
            return ResponseEntity.ok(
                ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true)
                    .data(activities)
                    .message("User activity timeline retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting user activity timeline for user: {} and timeframe: {}", userId, timeframe, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<List<Map<String, Object>>>builder()
                    .success(false)
                    .message("Failed to retrieve user activity timeline")
                    .build()
            );
        }
    }

    /**
     * Get marketplace analytics
     */
    @GetMapping("/marketplace")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMarketplaceAnalytics(
            @RequestParam(defaultValue = "month") String timeframe) {
        try {
            Map<String, Object> analytics = analyticsService.getMarketplaceAnalytics(timeframe);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(analytics)
                    .message("Marketplace analytics retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting marketplace analytics for timeframe: {}", timeframe, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to retrieve marketplace analytics")
                    .build()
            );
        }
    }

    /**
     * Get chat analytics
     */
    @GetMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatAnalytics(
            @RequestParam(defaultValue = "week") String timeframe) {
        try {
            Map<String, Object> analytics = analyticsService.getChatAnalytics(timeframe);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(analytics)
                    .message("Chat analytics retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting chat analytics for timeframe: {}", timeframe, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to retrieve chat analytics")
                    .build()
            );
        }
    }

    /**
     * Get dashboard data for admin
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardData(
            @RequestParam(defaultValue = "week") String timeframe) {
        try {
            Map<String, Object> dashboard = Map.of(
                "overview", analyticsService.getPlatformOverview(),
                "trending", analyticsService.getTrendingContent(timeframe),
                "marketplace", analyticsService.getMarketplaceAnalytics(timeframe),
                "chat", analyticsService.getChatAnalytics(timeframe)
            );
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(dashboard)
                    .message("Dashboard data retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting dashboard data for timeframe: {}", timeframe, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to retrieve dashboard data")
                    .build()
            );
        }
    }
}
