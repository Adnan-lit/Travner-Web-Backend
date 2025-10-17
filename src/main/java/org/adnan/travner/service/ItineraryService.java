package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ItineraryDTO;
import org.adnan.travner.dto.ItineraryRequest;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.entry.ItineraryEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.ItineraryRepository;
import org.adnan.travner.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for itinerary operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;

    @Transactional
    public ItineraryDTO createItinerary(String username, ItineraryRequest request) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        ItineraryEntry itinerary = ItineraryEntry.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .destination(request.getDestination())
                .destinationCountry(request.getDestinationCountry())
                .destinationCity(request.getDestinationCity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .author(user)
                .tags(request.getTags())
                .isPublic(request.isPublic())
                .isTemplate(request.isTemplate())
                .estimatedBudget(request.getEstimatedBudget())
                .currency(request.getCurrency())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .likes(0)
                .shares(0)
                .views(0)
                .build();

        // Convert request items to entity items
        if (request.getItems() != null) {
            List<ItineraryEntry.ItineraryItem> items = request.getItems().stream()
                    .map(this::convertToEntityItem)
                    .collect(Collectors.toList());
            itinerary.setItems(items);
        }

        ItineraryEntry savedItinerary = itineraryRepository.save(itinerary);
        return convertToDTO(savedItinerary);
    }

    public Page<ItineraryDTO> getPublicItineraries(Pageable pageable) {
        return itineraryRepository.findByIsPublicTrueOrderByLikesDesc(pageable)
                .map(this::convertToDTO);
    }

    public Page<ItineraryDTO> getItinerariesByDestination(String destination, Pageable pageable) {
        return itineraryRepository.findByDestinationContainingIgnoreCaseAndIsPublicTrue(destination, pageable)
                .map(this::convertToDTO);
    }

    public Page<ItineraryDTO> getItinerariesByUser(String username, Pageable pageable) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return itineraryRepository.findByAuthor_Id(user.getId(), pageable)
                .map(this::convertToDTO);
    }

    public Page<ItineraryDTO> searchItineraries(String query, Pageable pageable) {
        return itineraryRepository.searchItineraries(query, pageable)
                .map(this::convertToDTO);
    }

    public Page<ItineraryDTO> getItinerariesByTags(List<String> tags, Pageable pageable) {
        return itineraryRepository.findByTagsInAndIsPublicTrue(tags, pageable)
                .map(this::convertToDTO);
    }

    public Page<ItineraryDTO> getItinerariesByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return itineraryRepository.findByDateRange(startDate, endDate, pageable)
                .map(this::convertToDTO);
    }

    public Page<ItineraryDTO> getTemplateItineraries(Pageable pageable) {
        return itineraryRepository.findByIsTemplateTrueAndIsPublicTrue(pageable)
                .map(this::convertToDTO);
    }

    public Optional<ItineraryDTO> getItineraryById(String id) {
        return itineraryRepository.findById(new ObjectId(id))
                .map(this::convertToDTO);
    }

    @Transactional
    public ItineraryDTO updateItinerary(String id, String username, ItineraryRequest request) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<ItineraryEntry> itineraryOptional = itineraryRepository.findById(new ObjectId(id));
        if (itineraryOptional.isEmpty()) {
            throw new RuntimeException("Itinerary not found");
        }

        ItineraryEntry itinerary = itineraryOptional.get();
        if (!itinerary.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to update this itinerary");
        }

        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setDestination(request.getDestination());
        itinerary.setDestinationCountry(request.getDestinationCountry());
        itinerary.setDestinationCity(request.getDestinationCity());
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setTags(request.getTags());
        itinerary.setPublic(request.isPublic());
        itinerary.setTemplate(request.isTemplate());
        itinerary.setEstimatedBudget(request.getEstimatedBudget());
        itinerary.setCurrency(request.getCurrency());
        itinerary.setUpdatedAt(LocalDateTime.now());

        if (request.getItems() != null) {
            List<ItineraryEntry.ItineraryItem> items = request.getItems().stream()
                    .map(this::convertToEntityItem)
                    .collect(Collectors.toList());
            itinerary.setItems(items);
        }

        ItineraryEntry updatedItinerary = itineraryRepository.save(itinerary);
        return convertToDTO(updatedItinerary);
    }

    @Transactional
    public void deleteItinerary(String id, String username) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<ItineraryEntry> itineraryOptional = itineraryRepository.findById(new ObjectId(id));
        if (itineraryOptional.isEmpty()) {
            throw new RuntimeException("Itinerary not found");
        }

        ItineraryEntry itinerary = itineraryOptional.get();
        if (!itinerary.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to delete this itinerary");
        }

        itineraryRepository.delete(itinerary);
    }

    @Transactional
    public ItineraryDTO likeItinerary(String id, String username) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<ItineraryEntry> itineraryOptional = itineraryRepository.findById(new ObjectId(id));
        if (itineraryOptional.isEmpty()) {
            throw new RuntimeException("Itinerary not found");
        }

        ItineraryEntry itinerary = itineraryOptional.get();
        itinerary.setLikes(itinerary.getLikes() + 1);
        ItineraryEntry updatedItinerary = itineraryRepository.save(itinerary);
        return convertToDTO(updatedItinerary);
    }

    private ItineraryEntry.ItineraryItem convertToEntityItem(ItineraryRequest.ItineraryItemRequest requestItem) {
        return ItineraryEntry.ItineraryItem.builder()
                .title(requestItem.getTitle())
                .description(requestItem.getDescription())
                .location(requestItem.getLocation())
                .startTime(requestItem.getStartTime())
                .endTime(requestItem.getEndTime())
                .category(requestItem.getCategory())
                .latitude(requestItem.getLatitude())
                .longitude(requestItem.getLongitude())
                .address(requestItem.getAddress())
                .notes(requestItem.getNotes())
                .estimatedCost(requestItem.getEstimatedCost())
                .currency(requestItem.getCurrency())
                .attachments(requestItem.getAttachments())
                .build();
    }

    private ItineraryDTO convertToDTO(ItineraryEntry itinerary) {
        UserEntry author = itinerary.getAuthor();
        UserSummaryDTO authorDTO = null;
        if (author != null) {
            authorDTO = UserSummaryDTO.builder()
                    .id(author.getId().toString())
                    .userName(author.getUserName())
                    .firstName(author.getFirstName())
                    .lastName(author.getLastName())
                    .build();
        }

        List<ItineraryDTO.ItineraryItemDTO> itemsDTO = itinerary.getItems().stream()
                .map(this::convertToDTOItem)
                .collect(Collectors.toList());

        return ItineraryDTO.builder()
                .id(itinerary.getId().toString())
                .title(itinerary.getTitle())
                .description(itinerary.getDescription())
                .destination(itinerary.getDestination())
                .destinationCountry(itinerary.getDestinationCountry())
                .destinationCity(itinerary.getDestinationCity())
                .startDate(itinerary.getStartDate())
                .endDate(itinerary.getEndDate())
                .author(authorDTO)
                .items(itemsDTO)
                .tags(itinerary.getTags())
                .isPublic(itinerary.isPublic())
                .isTemplate(itinerary.isTemplate())
                .estimatedBudget(itinerary.getEstimatedBudget())
                .currency(itinerary.getCurrency())
                .createdAt(itinerary.getCreatedAt())
                .updatedAt(itinerary.getUpdatedAt())
                .likes(itinerary.getLikes())
                .shares(itinerary.getShares())
                .views(itinerary.getViews())
                .build();
    }

    private ItineraryDTO.ItineraryItemDTO convertToDTOItem(ItineraryEntry.ItineraryItem item) {
        return ItineraryDTO.ItineraryItemDTO.builder()
                .title(item.getTitle())
                .description(item.getDescription())
                .location(item.getLocation())
                .startTime(item.getStartTime())
                .endTime(item.getEndTime())
                .category(item.getCategory())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .address(item.getAddress())
                .notes(item.getNotes())
                .estimatedCost(item.getEstimatedCost())
                .currency(item.getCurrency())
                .attachments(item.getAttachments())
                .build();
    }
}
