package org.adnan.travner.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.entry.*;
import org.adnan.travner.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Data seeder to populate the database with sample data for development and testing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CommentRepository commentRepository;
    private final ItineraryRepository itineraryRepository;
    private final LocalGuideRepository localGuideRepository;
    private final TravelBuddyRepository travelBuddyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Only seed data if MongoDB is available and no users exist
        if (isMongoDbAvailable() && userRepository.count() == 0) {
            log.info("Starting data seeding...");
            seedUsers();
            seedPosts();
            seedChatConversations();
            seedItineraries();
            seedLocalGuides();
            seedTravelBuddies();
            log.info("Data seeding completed successfully!");
        } else if (userRepository.count() > 0) {
            log.info("Database already contains data. Skipping seeding.");
        } else {
            log.warn("MongoDB not available. Skipping data seeding.");
        }
    }

    private boolean isMongoDbAvailable() {
        try {
            userRepository.count();
            return true;
        } catch (Exception e) {
            log.debug("MongoDB not available: {}", e.getMessage());
            return false;
        }
    }

    private void seedUsers() {
        log.info("Seeding users...");
        
        List<UserEntry> users = Arrays.asList(
            UserEntry.builder()
                .userName("john_doe")
                .password(passwordEncoder.encode("password123"))
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .bio("Travel enthusiast and adventure seeker")
                .location("New York, USA")
                .profileImageUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face")
                .roles(Arrays.asList("USER"))
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build(),
                
            UserEntry.builder()
                .userName("jane_smith")
                .password(passwordEncoder.encode("password123"))
                .email("jane.smith@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .bio("Photographer and travel blogger")
                .location("London, UK")
                .profileImageUrl("https://images.unsplash.com/photo-1494790108755-2616b612b786?w=150&h=150&fit=crop&crop=face")
                .roles(Arrays.asList("USER"))
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(25))
                .build(),
                
            UserEntry.builder()
                .userName("mike_wilson")
                .password(passwordEncoder.encode("password123"))
                .email("mike.wilson@example.com")
                .firstName("Mike")
                .lastName("Wilson")
                .bio("Backpacker and budget travel expert")
                .location("Sydney, Australia")
                .profileImageUrl("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face")
                .roles(Arrays.asList("USER"))
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(20))
                .build(),
                
            UserEntry.builder()
                .userName("sarah_jones")
                .password(passwordEncoder.encode("password123"))
                .email("sarah.jones@example.com")
                .firstName("Sarah")
                .lastName("Jones")
                .bio("Luxury travel consultant")
                .location("Paris, France")
                .profileImageUrl("https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop&crop=face")
                .roles(Arrays.asList("USER"))
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build(),
                
            UserEntry.builder()
                .userName("alex_chen")
                .password(passwordEncoder.encode("password123"))
                .email("alex.chen@example.com")
                .firstName("Alex")
                .lastName("Chen")
                .bio("Food blogger and cultural explorer")
                .location("Tokyo, Japan")
                .profileImageUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&h=150&fit=crop&crop=face")
                .roles(Arrays.asList("USER"))
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build()
        );
        
        userRepository.saveAll(users);
        log.info("Seeded {} users", users.size());
    }

    private void seedPosts() {
        log.info("Seeding posts...");
        
        List<UserEntry> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found. Cannot seed posts.");
            return;
        }
        
        List<PostEntry> posts = Arrays.asList(
            PostEntry.builder()
                .title("Amazing Sunset in Santorini")
                .content("Just witnessed the most breathtaking sunset in Santorini! The colors were absolutely incredible. This place truly lives up to its reputation as one of the most beautiful islands in Greece.")
                .location("Santorini, Greece")
                .author(users.get(0))
                .tags(Arrays.asList("sunset", "greece", "santorini", "photography"))
                .upvotes(15)
                .downvotes(1)
                .published(true)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build(),
                
            PostEntry.builder()
                .title("Hidden Gems in Tokyo")
                .content("Found some incredible hidden spots in Tokyo that most tourists never discover! From tiny ramen shops in narrow alleys to secret gardens in the middle of the city.")
                .location("Tokyo, Japan")
                .author(users.get(1))
                .tags(Arrays.asList("japan", "tokyo", "hidden-gems", "culture"))
                .upvotes(23)
                .downvotes(0)
                .published(true)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build(),
                
            PostEntry.builder()
                .title("Budget Travel Tips for Southeast Asia")
                .content("Just completed a 3-month backpacking trip through Southeast Asia on a tight budget! Here are my top tips for budget travel.")
                .location("Southeast Asia")
                .author(users.get(2))
                .tags(Arrays.asList("budget-travel", "backpacking", "southeast-asia", "tips"))
                .upvotes(45)
                .downvotes(2)
                .published(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build(),
                
            PostEntry.builder()
                .title("Luxury Resort Experience in Maldives")
                .content("Just spent a week at an overwater villa in the Maldives and it was absolutely magical! The crystal clear waters and pristine beaches made this a once-in-a-lifetime experience.")
                .location("Maldives")
                .author(users.get(3))
                .tags(Arrays.asList("luxury", "maldives", "resort", "marine-life"))
                .upvotes(12)
                .downvotes(0)
                .published(true)
                .createdAt(LocalDateTime.now().minusHours(6))
                .build(),
                
            PostEntry.builder()
                .title("Street Food Adventure in Bangkok")
                .content("Embarked on a culinary journey through Bangkok street food scene. From pad thai to mango sticky rice, every dish was amazing.")
                .location("Bangkok, Thailand")
                .author(users.get(4))
                .tags(Arrays.asList("food", "bangkok", "street-food", "culinary"))
                .upvotes(18)
                .downvotes(1)
                .published(true)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build()
        );
        
        postRepository.saveAll(posts);
        log.info("Seeded {} posts", posts.size());
    }

    private void seedChatConversations() {
        log.info("Seeding chat conversations...");
        
        List<UserEntry> users = userRepository.findAll();
        if (users.size() < 2) {
            log.warn("Not enough users to create conversations.");
            return;
        }
        
        // Create a group conversation
        ChatConversation groupConversation = ChatConversation.builder()
            .title("Travel Enthusiasts")
            .type("GROUP")
            .participantIds(Arrays.asList(users.get(0).getId().toString(), users.get(1).getId().toString(), users.get(2).getId().toString()))
            .createdAt(LocalDateTime.now().minusDays(10))
            .build();
        
        chatConversationRepository.save(groupConversation);
        
        // Create direct conversations
        ChatConversation directConversation1 = ChatConversation.builder()
            .title("Direct Message")
            .type("DIRECT")
            .participantIds(Arrays.asList(users.get(0).getId().toString(), users.get(1).getId().toString()))
            .createdAt(LocalDateTime.now().minusDays(5))
            .build();
        
        ChatConversation directConversation2 = ChatConversation.builder()
            .title("Direct Message")
            .type("DIRECT")
            .participantIds(Arrays.asList(users.get(1).getId().toString(), users.get(2).getId().toString()))
            .createdAt(LocalDateTime.now().minusDays(3))
            .build();
        
        chatConversationRepository.saveAll(Arrays.asList(directConversation1, directConversation2));
        
        // Add some sample messages
        seedChatMessages(groupConversation, users);
        seedChatMessages(directConversation1, Arrays.asList(users.get(0), users.get(1)));
        
        log.info("Seeded chat conversations and messages");
    }

    private void seedChatMessages(ChatConversation conversation, List<UserEntry> participants) {
        List<ChatMessage> messages = Arrays.asList(
            ChatMessage.builder()
                .conversationId(conversation.getId().toString())
                .senderId(participants.get(0).getId().toString())
                .senderUsername(participants.get(0).getUserName())
                .content("Hey everyone! How is your travel planning going?")
                .messageType("TEXT")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build(),
                
            ChatMessage.builder()
                .conversationId(conversation.getId().toString())
                .senderId(participants.get(1).getId().toString())
                .senderUsername(participants.get(1).getUserName())
                .content("Great! Just booked my flight to Japan. So excited!")
                .messageType("TEXT")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build(),
                
            ChatMessage.builder()
                .conversationId(conversation.getId().toString())
                .senderId(participants.get(0).getId().toString())
                .senderUsername(participants.get(0).getUserName())
                .content("That is awesome! I have been to Japan before. You will love it!")
                .messageType("TEXT")
                .createdAt(LocalDateTime.now().minusHours(12))
                .build()
        );
        
        chatMessageRepository.saveAll(messages);
    }

    private void seedItineraries() {
        log.info("Seeding itineraries...");
        
        List<UserEntry> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found. Cannot seed itineraries.");
            return;
        }
        
        List<ItineraryEntry> itineraries = Arrays.asList(
            ItineraryEntry.builder()
                .title("7 Days in Japan")
                .description("A comprehensive 7-day itinerary covering Tokyo, Kyoto, and Osaka")
                .destination("Japan")
                .destinationCountry("Japan")
                .destinationCity("Tokyo")
                .estimatedBudget(1500)
                .currency("USD")
                .author(users.get(0))
                .tags(Arrays.asList("japan", "tokyo", "kyoto", "osaka"))
                .isPublic(true)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build(),
                
            ItineraryEntry.builder()
                .title("European Adventure")
                .description("2 weeks exploring major European cities")
                .destination("Europe")
                .destinationCountry("Europe")
                .destinationCity("Paris")
                .estimatedBudget(3000)
                .currency("EUR")
                .author(users.get(1))
                .tags(Arrays.asList("europe", "cities", "culture"))
                .isPublic(true)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build()
        );
        
        itineraryRepository.saveAll(itineraries);
        log.info("Seeded {} itineraries", itineraries.size());
    }

    private void seedLocalGuides() {
        log.info("Seeding local guides...");
        
        List<UserEntry> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found. Cannot seed local guides.");
            return;
        }
        
        List<LocalGuideEntry> guides = Arrays.asList(
            LocalGuideEntry.builder()
                .user(users.get(4))
                .location("Tokyo, Japan")
                .city("Tokyo")
                .country("Japan")
                .bio("Experience authentic Japanese cuisine with a local food expert")
                .specialties(Arrays.asList("food", "culture", "history"))
                .languages(Arrays.asList("English", "Japanese"))
                .hourlyRate(java.math.BigDecimal.valueOf(50.0))
                .currency("USD")
                .isAvailable(true)
                .contactMethod("email")
                .contactInfo("alex.chen@example.com")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build(),
                
            LocalGuideEntry.builder()
                .user(users.get(3))
                .location("Paris, France")
                .city("Paris")
                .country("France")
                .bio("Discover hidden art galleries and street art in Paris")
                .specialties(Arrays.asList("art", "culture", "history"))
                .languages(Arrays.asList("English", "French"))
                .hourlyRate(java.math.BigDecimal.valueOf(35.0))
                .currency("EUR")
                .isAvailable(true)
                .contactMethod("email")
                .contactInfo("sarah.jones@example.com")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build()
        );
        
        localGuideRepository.saveAll(guides);
        log.info("Seeded {} local guides", guides.size());
    }

    private void seedTravelBuddies() {
        log.info("Seeding travel buddies...");
        
        List<UserEntry> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found. Cannot seed travel buddies.");
            return;
        }
        
        List<TravelBuddyEntry> buddies = Arrays.asList(
            TravelBuddyEntry.builder()
                .requester(users.get(2))
                .destination("Nepal")
                .destinationCountry("Nepal")
                .destinationCity("Kathmandu")
                .travelDate(LocalDateTime.now().plusDays(30))
                .returnDate(LocalDateTime.now().plusDays(44))
                .description("Planning a 2-week trek in the Himalayas. Looking for someone with similar fitness level and love for adventure!")
                .travelPurpose("adventure")
                .status(TravelBuddyEntry.BuddyStatus.ACTIVE)
                .interests(Arrays.asList("hiking", "mountains", "adventure", "nature"))
                .budgetRange("$500-1000")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build(),
                
            TravelBuddyEntry.builder()
                .requester(users.get(0))
                .destination("United States")
                .destinationCountry("United States")
                .destinationCity("New York")
                .travelDate(LocalDateTime.now().plusDays(45))
                .returnDate(LocalDateTime.now().plusDays(60))
                .description("Planning a coast-to-coast road trip. Looking for travel companions to share costs and experiences!")
                .travelPurpose("leisure")
                .status(TravelBuddyEntry.BuddyStatus.ACTIVE)
                .interests(Arrays.asList("road-trip", "sightseeing", "adventure", "photography"))
                .budgetRange("$1000-2000")
                .createdAt(LocalDateTime.now().minusHours(6))
                .build()
        );
        
        travelBuddyRepository.saveAll(buddies);
        log.info("Seeded {} travel buddies", buddies.size());
    }
}