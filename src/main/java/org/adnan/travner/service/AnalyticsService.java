package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.entry.*;
import org.adnan.travner.repository.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;

/**
 * Analytics Service for tracking user engagement and platform metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostVoteRepository postVoteRepository;
    private final CommentRepository commentRepository;
    private final ItineraryRepository itineraryRepository;
    private final TravelBuddyRepository travelBuddyRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * Get platform overview statistics
     */
    @Cacheable(value = "platformStats", key = "'overview'")
    public Map<String, Object> getPlatformOverview() {
        Map<String, Object> stats = new HashMap<>();
        
        // User statistics
        long totalUsers = userRepository.count();
        long activeUsers = getActiveUsersCount();
        long newUsersThisMonth = getNewUsersThisMonth();
        
        // Content statistics
        long totalPosts = postRepository.count();
        long totalComments = commentRepository.count();
        long totalItineraries = itineraryRepository.count();
        long totalTravelBuddies = travelBuddyRepository.count();
        
        // Marketplace statistics
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        double totalRevenue = getTotalRevenue();
        
        // Engagement statistics
        long totalVotes = postVoteRepository.count();
        long totalChatMessages = chatMessageRepository.count();
        
        stats.put("users", Map.of(
            "total", totalUsers,
            "active", activeUsers,
            "newThisMonth", newUsersThisMonth
        ));
        
        stats.put("content", Map.of(
            "posts", totalPosts,
            "comments", totalComments,
            "itineraries", totalItineraries,
            "travelBuddies", totalTravelBuddies
        ));
        
        stats.put("marketplace", Map.of(
            "products", totalProducts,
            "orders", totalOrders,
            "revenue", totalRevenue
        ));
        
        stats.put("engagement", Map.of(
            "votes", totalVotes,
            "chatMessages", totalChatMessages
        ));
        
        return stats;
    }

    /**
     * Get user engagement metrics
     */
    @Cacheable(value = "userEngagement", key = "#userId")
    public Map<String, Object> getUserEngagement(String userId) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Post engagement
        long userPosts = postRepository.countByAuthor_Id(new ObjectId(userId));
        long userComments = commentRepository.countByAuthor_Id(new ObjectId(userId));
        long userVotes = postVoteRepository.countByUserId(new ObjectId(userId));
        
        // Content creation
        long userItineraries = itineraryRepository.countByAuthor_Id(new ObjectId(userId));
        long userTravelBuddies = travelBuddyRepository.countByRequester_Id(new ObjectId(userId));
        
        // Marketplace activity
        long userOrders = orderRepository.countByUserId(userId);
        long userProducts = productRepository.countBySellerId(userId);
        
        // Chat activity
        long userChatMessages = chatMessageRepository.countBySenderId(userId);
        
        metrics.put("posts", userPosts);
        metrics.put("comments", userComments);
        metrics.put("votes", userVotes);
        metrics.put("itineraries", userItineraries);
        metrics.put("travelBuddies", userTravelBuddies);
        metrics.put("orders", userOrders);
        metrics.put("products", userProducts);
        metrics.put("chatMessages", userChatMessages);
        
        // Engagement score calculation
        int engagementScore = calculateEngagementScore(metrics);
        metrics.put("engagementScore", engagementScore);
        
        return metrics;
    }

    /**
     * Get content performance metrics
     */
    @Cacheable(value = "contentPerformance", key = "#contentType + '_' + #timeframe")
    public Map<String, Object> getContentPerformance(String contentType, String timeframe) {
        Map<String, Object> metrics = new HashMap<>();
        
        LocalDateTime startDate = getStartDate(timeframe);
        
        switch (contentType.toLowerCase()) {
            case "posts":
                return getPostPerformance(startDate);
            case "itineraries":
                return getItineraryPerformance(startDate);
            case "travelbuddies":
                return getTravelBuddyPerformance(startDate);
            case "products":
                return getProductPerformance(startDate);
            default:
                return new HashMap<>();
        }
    }

    /**
     * Get trending content
     */
    @Cacheable(value = "trendingContent", key = "#timeframe")
    public Map<String, Object> getTrendingContent(String timeframe) {
        Map<String, Object> trending = new HashMap<>();
        
        LocalDateTime startDate = getStartDate(timeframe);
        
        // Trending posts
        List<PostEntry> trendingPosts = postRepository.findTop10ByCreatedAtAfterOrderByUpvotesDesc(startDate);
        trending.put("posts", trendingPosts.stream()
            .map(this::mapPostToSummary)
            .collect(Collectors.toList()));
        
        // Trending itineraries
        List<ItineraryEntry> trendingItineraries = itineraryRepository.findTop10ByCreatedAtAfterOrderByViewsDesc(startDate);
        trending.put("itineraries", trendingItineraries.stream()
            .map(this::mapItineraryToSummary)
            .collect(Collectors.toList()));
        
        // Trending travel buddies
        List<TravelBuddyEntry> trendingBuddies = travelBuddyRepository.findTop10ByCreatedAtAfterOrderByCreatedAtDesc(startDate);
        trending.put("travelBuddies", trendingBuddies.stream()
            .map(this::mapTravelBuddyToSummary)
            .collect(Collectors.toList()));
        
        return trending;
    }

    /**
     * Get user activity timeline
     */
    @Cacheable(value = "userActivity", key = "#userId + '_' + #timeframe")
    public List<Map<String, Object>> getUserActivityTimeline(String userId, String timeframe) {
        LocalDateTime startDate = getStartDate(timeframe);
        
        List<Map<String, Object>> activities = new ArrayList<>();
        
        // Get user posts
        postRepository.findByAuthor_IdAndCreatedAtAfter(new ObjectId(userId), startDate)
            .forEach(post -> activities.add(Map.of(
                "type", "post",
                "title", post.getTitle(),
                "timestamp", post.getCreatedAt(),
                "engagement", post.getUpvotes() - post.getDownvotes()
            )));
        
        // Get user comments
        commentRepository.findByAuthor_IdAndCreatedAtAfter(new ObjectId(userId), startDate)
            .forEach(comment -> activities.add(Map.of(
                "type", "comment",
                "title", "Commented on: " + comment.getPostId(),
                "timestamp", comment.getCreatedAt(),
                "engagement", 0
            )));
        
        // Get user itineraries
        itineraryRepository.findByAuthor_IdAndCreatedAtAfter(new ObjectId(userId), startDate)
            .forEach(itinerary -> activities.add(Map.of(
                "type", "itinerary",
                "title", itinerary.getTitle(),
                "timestamp", itinerary.getCreatedAt(),
                "engagement", itinerary.getViews()
            )));
        
        // Sort by timestamp
        activities.sort((a, b) -> 
            ((LocalDateTime) b.get("timestamp")).compareTo((LocalDateTime) a.get("timestamp")));
        
        return activities;
    }

    /**
     * Get marketplace analytics
     */
    @Cacheable(value = "marketplaceAnalytics", key = "#timeframe")
    public Map<String, Object> getMarketplaceAnalytics(String timeframe) {
        Map<String, Object> analytics = new HashMap<>();
        
        LocalDateTime startDate = getStartDate(timeframe);
        
        // Sales metrics
        long totalOrders = orderRepository.countByOrderedAtAfter(startDate);
        double totalRevenue = orderRepository.findByOrderedAtAfter(startDate)
            .stream()
            .mapToDouble(order -> order.getTotalAmount().doubleValue())
            .sum();
        
        // Product metrics
        long totalProducts = productRepository.countByCreatedAtAfter(startDate);
        long activeProducts = productRepository.countByCreatedAtAfterAndIsAvailable(startDate, true);
        
        // Category breakdown
        Map<String, Long> categoryBreakdown = productRepository.findByCreatedAtAfter(startDate)
            .stream()
            .collect(Collectors.groupingBy(
                ProductEntry::getCategory,
                Collectors.counting()
            ));
        
        analytics.put("orders", totalOrders);
        analytics.put("revenue", totalRevenue);
        analytics.put("products", Map.of(
            "total", totalProducts,
            "active", activeProducts
        ));
        analytics.put("categoryBreakdown", categoryBreakdown);
        
        return analytics;
    }

    /**
     * Get chat analytics
     */
    @Cacheable(value = "chatAnalytics", key = "#timeframe")
    public Map<String, Object> getChatAnalytics(String timeframe) {
        Map<String, Object> analytics = new HashMap<>();
        
        LocalDateTime startDate = getStartDate(timeframe);
        
        long totalMessages = chatMessageRepository.countByCreatedAtAfter(startDate);
        long uniqueConversations = chatMessageRepository.findDistinctConversationIdsByCreatedAtAfter(startDate).size();
        
        // Message distribution by hour
        Map<Integer, Long> messagesByHour = chatMessageRepository.findByCreatedAtAfter(startDate)
            .stream()
            .collect(Collectors.groupingBy(
                message -> message.getCreatedAt().getHour(),
                Collectors.counting()
            ));
        
        analytics.put("totalMessages", totalMessages);
        analytics.put("uniqueConversations", uniqueConversations);
        analytics.put("messagesByHour", messagesByHour);
        
        return analytics;
    }

    private long getActiveUsersCount() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        return userRepository.countByLastLoginAtAfter(thirtyDaysAgo);
    }

    private long getNewUsersThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return userRepository.countByCreatedAtAfter(startOfMonth);
    }

    private double getTotalRevenue() {
        return orderRepository.findAll()
            .stream()
            .mapToDouble(order -> order.getTotalAmount().doubleValue())
            .sum();
    }

    private int calculateEngagementScore(Map<String, Object> metrics) {
        int score = 0;
        score += ((Long) metrics.get("posts")) * 10;
        score += ((Long) metrics.get("comments")) * 5;
        score += ((Long) metrics.get("votes")) * 2;
        score += ((Long) metrics.get("itineraries")) * 15;
        score += ((Long) metrics.get("travelBuddies")) * 12;
        score += ((Long) metrics.get("chatMessages")) * 3;
        return score;
    }

    private LocalDateTime getStartDate(String timeframe) {
        LocalDateTime now = LocalDateTime.now();
        switch (timeframe.toLowerCase()) {
            case "day":
                return now.minus(1, ChronoUnit.DAYS);
            case "week":
                return now.minus(1, ChronoUnit.WEEKS);
            case "month":
                return now.minus(1, ChronoUnit.MONTHS);
            case "year":
                return now.minus(1, ChronoUnit.YEARS);
            default:
                return now.minus(1, ChronoUnit.WEEKS);
        }
    }

    private Map<String, Object> getPostPerformance(LocalDateTime startDate) {
        Map<String, Object> performance = new HashMap<>();
        
        long totalPosts = postRepository.countByCreatedAtAfter(startDate);
        long totalVotes = postVoteRepository.countByCreatedAtAfter(startDate);
        double avgVotesPerPost = totalPosts > 0 ? (double) totalVotes / totalPosts : 0;
        
        performance.put("totalPosts", totalPosts);
        performance.put("totalVotes", totalVotes);
        performance.put("avgVotesPerPost", avgVotesPerPost);
        
        return performance;
    }

    private Map<String, Object> getItineraryPerformance(LocalDateTime startDate) {
        Map<String, Object> performance = new HashMap<>();
        
        long totalItineraries = itineraryRepository.countByCreatedAtAfter(startDate);
        long totalViews = itineraryRepository.findByCreatedAtAfter(startDate)
            .stream()
            .mapToLong(ItineraryEntry::getViews)
            .sum();
        double avgViewsPerItinerary = totalItineraries > 0 ? (double) totalViews / totalItineraries : 0;
        
        performance.put("totalItineraries", totalItineraries);
        performance.put("totalViews", totalViews);
        performance.put("avgViewsPerItinerary", avgViewsPerItinerary);
        
        return performance;
    }

    private Map<String, Object> getTravelBuddyPerformance(LocalDateTime startDate) {
        Map<String, Object> performance = new HashMap<>();
        
        long totalBuddies = travelBuddyRepository.countByCreatedAtAfter(startDate);
        long totalViews = travelBuddyRepository.findByCreatedAtAfter(startDate)
            .stream()
            .mapToLong(buddy -> 0) // TravelBuddyEntry doesn't have views field
            .sum();
        double avgViewsPerBuddy = totalBuddies > 0 ? (double) totalViews / totalBuddies : 0;
        
        performance.put("totalBuddies", totalBuddies);
        performance.put("totalViews", totalViews);
        performance.put("avgViewsPerBuddy", avgViewsPerBuddy);
        
        return performance;
    }

    private Map<String, Object> getProductPerformance(LocalDateTime startDate) {
        Map<String, Object> performance = new HashMap<>();
        
        long totalProducts = productRepository.countByCreatedAtAfter(startDate);
        long totalOrders = orderRepository.countByOrderedAtAfter(startDate);
        double conversionRate = totalProducts > 0 ? (double) totalOrders / totalProducts : 0;
        
        performance.put("totalProducts", totalProducts);
        performance.put("totalOrders", totalOrders);
        performance.put("conversionRate", conversionRate);
        
        return performance;
    }

    private Map<String, Object> mapPostToSummary(PostEntry post) {
        return Map.of(
            "id", post.getId().toString(),
            "title", post.getTitle(),
            "author", post.getAuthor().getId().toString(),
            "votes", post.getUpvotes() - post.getDownvotes(),
            "createdAt", post.getCreatedAt()
        );
    }

    private Map<String, Object> mapItineraryToSummary(ItineraryEntry itinerary) {
        return Map.of(
            "id", itinerary.getId().toString(),
            "title", itinerary.getTitle(),
            "author", itinerary.getAuthor().getId().toString(),
            "views", itinerary.getViews(),
            "createdAt", itinerary.getCreatedAt()
        );
    }

    private Map<String, Object> mapTravelBuddyToSummary(TravelBuddyEntry buddy) {
        return Map.of(
            "id", buddy.getId().toString(),
            "title", buddy.getDestination(),
            "author", buddy.getRequester().getId().toString(),
            "views", 0, // TravelBuddyEntry doesn't have views field
            "createdAt", buddy.getCreatedAt()
        );
    }
}
