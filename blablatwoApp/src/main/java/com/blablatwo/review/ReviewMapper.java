package com.blablatwo.review;

import com.blablatwo.review.dto.ReviewResponseDto;
import com.blablatwo.user.AvatarUrlResolver;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    private final UserProfileRepository userProfileRepository;
    private final AvatarUrlResolver avatarUrlResolver;

    public ReviewMapper(UserProfileRepository userProfileRepository,
                        AvatarUrlResolver avatarUrlResolver) {
        this.userProfileRepository = userProfileRepository;
        this.avatarUrlResolver = avatarUrlResolver;
    }

    public ReviewResponseDto toDto(Review review) {
        UserProfile authorProfile = userProfileRepository.findById(review.getAuthor().getId())
                .orElse(null);

        String authorName = authorProfile != null ? authorProfile.getDisplayName() : "User";
        String authorAvatarUrl = avatarUrlResolver.resolve(authorProfile);

        return ReviewResponseDto.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .authorId(review.getAuthor().getId())
                .authorName(authorName)
                .authorAvatarUrl(authorAvatarUrl)
                .authorRole(review.getAuthorRole())
                .stars(review.getStars())
                .comment(review.getComment())
                .tags(review.getTags())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .publishedAt(review.getPublishedAt())
                .build();
    }
}
