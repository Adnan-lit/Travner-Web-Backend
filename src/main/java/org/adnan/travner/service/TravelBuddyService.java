package org.adnan.travner.service;

import org.adnan.travner.dto.TravelBuddyDTO;
import org.adnan.travner.dto.TravelBuddyRequest;
import org.adnan.travner.entry.TravelBuddyEntry;
import org.adnan.travner.repository.TravelBuddyRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for travel buddy operations
 */
@Service
public class TravelBuddyService {

    @Autowired
    private TravelBuddyRepository travelBuddyRepository;

    /**
     * Get travel buddy by ID
     */
    public Optional<TravelBuddyDTO> getTravelBuddyById(String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            return travelBuddyRepository.findById(objectId)
                    .map(this::convertToDTO);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Create a new travel buddy post
     */
    public TravelBuddyDTO createTravelBuddy(TravelBuddyRequest request, ObjectId userId) {
        TravelBuddyEntry buddy = TravelBuddyEntry.builder()
                .requester(new org.adnan.travner.entry.UserEntry())
                .destination(request.getDestination())
                .destinationCountry(request.getDestinationCountry())
                .destinationCity(request.getDestinationCity())
                .travelDate(request.getTravelDate())
                .returnDate(request.getReturnDate())
                .description(request.getDescription())
                .travelPurpose(request.getTravelPurpose())
                .status(TravelBuddyEntry.BuddyStatus.ACTIVE)
                .maxAge(request.getMaxAge())
                .minAge(request.getMinAge())
                .preferredGender(request.getPreferredGender())
                .interests(request.getInterests())
                .budgetRange(request.getBudgetRange())
                .build();

        // Set user reference
        buddy.getRequester().setId(userId);

        TravelBuddyEntry savedBuddy = travelBuddyRepository.save(buddy);
        return convertToDTO(savedBuddy);
    }

    /**
     * Update travel buddy post
     */
    public Optional<TravelBuddyDTO> updateTravelBuddy(String id, TravelBuddyRequest request, ObjectId userId) {
        try {
            ObjectId objectId = new ObjectId(id);
            return travelBuddyRepository.findById(objectId)
                    .filter(buddy -> buddy.getRequester().getId().equals(userId))
                    .map(buddy -> {
                        buddy.setDestination(request.getDestination());
                        buddy.setDestinationCountry(request.getDestinationCountry());
                        buddy.setDestinationCity(request.getDestinationCity());
                        buddy.setTravelDate(request.getTravelDate());
                        buddy.setReturnDate(request.getReturnDate());
                        buddy.setDescription(request.getDescription());
                        buddy.setTravelPurpose(request.getTravelPurpose());
                        buddy.setMaxAge(request.getMaxAge());
                        buddy.setMinAge(request.getMinAge());
                        buddy.setPreferredGender(request.getPreferredGender());
                        buddy.setInterests(request.getInterests());
                        buddy.setBudgetRange(request.getBudgetRange());

                        TravelBuddyEntry updatedBuddy = travelBuddyRepository.save(buddy);
                        return convertToDTO(updatedBuddy);
                    });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Delete travel buddy post
     */
    public boolean deleteTravelBuddy(String id, ObjectId userId) {
        try {
            ObjectId objectId = new ObjectId(id);
            return travelBuddyRepository.findById(objectId)
                    .filter(buddy -> buddy.getRequester().getId().equals(userId))
                    .map(buddy -> {
                        travelBuddyRepository.delete(buddy);
                        return true;
                    })
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get travel buddies by user
     */
    public List<TravelBuddyDTO> getTravelBuddiesByUser(String userId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            TravelBuddyEntry buddy = travelBuddyRepository.findByRequester_Id(objectId);
            if (buddy != null) {
                return List.of(convertToDTO(buddy));
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Convert TravelBuddyEntry to TravelBuddyDTO
     */
    public TravelBuddyDTO convertToDTO(TravelBuddyEntry entry) {
        return TravelBuddyDTO.builder()
                .id(entry.getId().toString())
                .requester(org.adnan.travner.dto.UserSummaryDTO.builder()
                        .id(entry.getRequester().getId().toString())
                        .userName(entry.getRequester().getUserName())
                        .email(entry.getRequester().getEmail())
                        .build())
                .destination(entry.getDestination())
                .destinationCountry(entry.getDestinationCountry())
                .destinationCity(entry.getDestinationCity())
                .travelDate(entry.getTravelDate())
                .returnDate(entry.getReturnDate())
                .description(entry.getDescription())
                .travelPurpose(entry.getTravelPurpose())
                .status(entry.getStatus())
                .maxAge(entry.getMaxAge())
                .minAge(entry.getMinAge())
                .preferredGender(entry.getPreferredGender())
                .interests(entry.getInterests())
                .budgetRange(entry.getBudgetRange())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .expiresAt(entry.getExpiresAt())
                .interestedUserIds(entry.getInterestedUsers().stream()
                        .map(ObjectId::toString)
                        .collect(java.util.stream.Collectors.toList()))
                .matchedUserIds(entry.getMatchedUsers().stream()
                        .map(ObjectId::toString)
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }
}
