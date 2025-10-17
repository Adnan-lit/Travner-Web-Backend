package org.adnan.travner.service;

import org.adnan.travner.dto.LocalGuideDTO;
import org.adnan.travner.dto.LocalGuideRequest;
import org.adnan.travner.entry.LocalGuideEntry;
import org.adnan.travner.repository.LocalGuideRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for local guide operations
 */
@Service
public class LocalGuideService {

    @Autowired
    private LocalGuideRepository localGuideRepository;

    /**
     * Get local guide by ID
     */
    public Optional<LocalGuideDTO> getLocalGuideById(String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            return localGuideRepository.findById(objectId)
                    .map(this::convertToDTO);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Create a new local guide
     */
    public LocalGuideDTO createLocalGuide(LocalGuideRequest request, ObjectId userId) {
        LocalGuideEntry guide = LocalGuideEntry.builder()
                .user(new org.adnan.travner.entry.UserEntry())
                .location(request.getLocation())
                .city(request.getCity())
                .country(request.getCountry())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .bio(request.getBio())
                .specialties(request.getSpecialties())
                .languages(request.getLanguages())
                .hourlyRate(request.getHourlyRate())
                .currency(request.getCurrency())
                .availability(request.getAvailability())
                .contactMethod(request.getContactMethod())
                .contactInfo(request.getContactInfo())
                .certifications(request.getCertifications())
                .experience(request.getExperience())
                .services(request.getServices())
                .build();

        // Set user reference
        guide.getUser().setId(userId);

        LocalGuideEntry savedGuide = localGuideRepository.save(guide);
        return convertToDTO(savedGuide);
    }

    /**
     * Update local guide
     */
    public Optional<LocalGuideDTO> updateLocalGuide(String id, LocalGuideRequest request, ObjectId userId) {
        try {
            ObjectId objectId = new ObjectId(id);
            return localGuideRepository.findById(objectId)
                    .filter(guide -> guide.getUser().getId().equals(userId))
                    .map(guide -> {
                        guide.setLocation(request.getLocation());
                        guide.setCity(request.getCity());
                        guide.setCountry(request.getCountry());
                        guide.setLatitude(request.getLatitude());
                        guide.setLongitude(request.getLongitude());
                        guide.setBio(request.getBio());
                        guide.setSpecialties(request.getSpecialties());
                        guide.setLanguages(request.getLanguages());
                        guide.setHourlyRate(request.getHourlyRate());
                        guide.setCurrency(request.getCurrency());
                        guide.setAvailability(request.getAvailability());
                        guide.setContactMethod(request.getContactMethod());
                        guide.setContactInfo(request.getContactInfo());
                        guide.setCertifications(request.getCertifications());
                        guide.setExperience(request.getExperience());
                        guide.setServices(request.getServices());

                        LocalGuideEntry updatedGuide = localGuideRepository.save(guide);
                        return convertToDTO(updatedGuide);
                    });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Delete local guide
     */
    public boolean deleteLocalGuide(String id, ObjectId userId) {
        try {
            ObjectId objectId = new ObjectId(id);
            return localGuideRepository.findById(objectId)
                    .filter(guide -> guide.getUser().getId().equals(userId))
                    .map(guide -> {
                        localGuideRepository.delete(guide);
                        return true;
                    })
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get local guides by user
     */
    public List<LocalGuideDTO> getLocalGuidesByUser(String userId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            LocalGuideEntry guide = localGuideRepository.findByUser_Id(objectId);
            if (guide != null) {
                return List.of(convertToDTO(guide));
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Convert LocalGuideEntry to LocalGuideDTO
     */
    public LocalGuideDTO convertToDTO(LocalGuideEntry entry) {
        return LocalGuideDTO.builder()
                .id(entry.getId().toString())
                .user(org.adnan.travner.dto.UserSummaryDTO.builder()
                        .id(entry.getUser().getId().toString())
                        .userName(entry.getUser().getUserName())
                        .email(entry.getUser().getEmail())
                        .build())
                .location(entry.getLocation())
                .city(entry.getCity())
                .country(entry.getCountry())
                .latitude(entry.getLatitude())
                .longitude(entry.getLongitude())
                .bio(entry.getBio())
                .specialties(entry.getSpecialties())
                .languages(entry.getLanguages())
                .hourlyRate(entry.getHourlyRate())
                .currency(entry.getCurrency())
                .isAvailable(entry.isAvailable())
                .availability(entry.getAvailability())
                .contactMethod(entry.getContactMethod())
                .contactInfo(entry.getContactInfo())
                .rating(entry.getRating())
                .reviewCount(entry.getReviewCount())
                .totalBookings(entry.getTotalBookings())
                .certifications(entry.getCertifications())
                .experience(entry.getExperience())
                .portfolio(entry.getPortfolio())
                .services(entry.getServices())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .lastActiveAt(entry.getLastActiveAt())
                .build();
    }
}
