package com.vamigo.user;

import com.vamigo.config.StorageProperties;
import com.vamigo.user.dto.AvatarConfirmRequest;
import com.vamigo.user.dto.AvatarConfirmResponse;
import com.vamigo.user.dto.AvatarPresignResponse;
import com.vamigo.user.exception.AvatarKeyMismatchException;
import com.vamigo.user.exception.AvatarNotUploadedException;
import com.vamigo.user.exception.InvalidAvatarContentTypeException;
import com.vamigo.user.exception.NoSuchUserException;
import com.vamigo.user.exception.StorageUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvatarService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final StorageProperties storageProperties;
    private final UserProfileRepository userProfileRepository;
    private final TaskExecutor avatarCleanupExecutor;
    private final RestClient avatarDownloadClient;

    public AvatarService(
            S3Presigner s3Presigner,
            S3Client s3Client,
            StorageProperties storageProperties,
            UserProfileRepository userProfileRepository,
            @Qualifier("avatarCleanupExecutor") TaskExecutor avatarCleanupExecutor
    ) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
        this.userProfileRepository = userProfileRepository;
        this.avatarCleanupExecutor = avatarCleanupExecutor;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.avatarDownloadClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Async
    public void importAvatarFromUrl(Long userId, String imageUrl) {
        try {
            String upscaledUrl = imageUrl.replace("=s96-c", "=s400-c");

            var response = avatarDownloadClient.get()
                    .uri(upscaledUrl)
                    .retrieve()
                    .toEntity(byte[].class);

            byte[] imageBytes = response.getBody();
            if (imageBytes == null || imageBytes.length == 0) {
                LOGGER.warn("Empty response downloading avatar for user {}", userId);
                return;
            }

            MediaType contentType = response.getHeaders().getContentType();
            String mimeType = contentType != null ? contentType.getType() + "/" + contentType.getSubtype() : "image/jpeg";
            String extension = CONTENT_TYPE_EXTENSIONS.getOrDefault(mimeType, "jpg");

            String objectKey = "users/%d/avatar/%s.%s".formatted(userId, UUID.randomUUID(), extension);

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(storageProperties.bucket())
                            .key(objectKey)
                            .contentType(mimeType)
                            .build(),
                    RequestBody.fromBytes(imageBytes)
            );

            UserProfile profile = userProfileRepository.findById(userId).orElse(null);
            if (profile == null) {
                LOGGER.warn("User profile {} not found during avatar import", userId);
                return;
            }
            if (profile.getAvatarObjectKey() != null) {
                LOGGER.debug("User {} already has avatar, skipping import", userId);
                return;
            }

            profile.updateAvatar(objectKey);
            userProfileRepository.save(profile);
            LOGGER.info("Imported Google avatar for user {}: {}", userId, objectKey);
        } catch (Exception e) {
            LOGGER.warn("Failed to import avatar from URL for user {}: {}", userId, e.getMessage());
        }
    }

    public AvatarPresignResponse generatePresignedUrl(Long userId, String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidAvatarContentTypeException(contentType);
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new InvalidAvatarContentTypeException(contentType);
        };

        String objectKey = "users/%d/avatar/%s.%s".formatted(userId, UUID.randomUUID(), extension);

        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(storageProperties.avatarPresignTtlMinutes()))
                .putObjectRequest(put -> put
                        .bucket(storageProperties.bucket())
                        .key(objectKey)
                        .contentType(contentType))
                .build();

        var presignedUrl = s3Presigner.presignPutObject(presignRequest);

        return new AvatarPresignResponse(
                presignedUrl.url().toString(),
                objectKey,
                storageProperties.avatarMaxSizeBytes()
        );
    }

    @Transactional
    public AvatarConfirmResponse confirmAvatar(Long userId, AvatarConfirmRequest request) {
        String objectKey = request.objectKey();
        String expectedPrefix = "users/%d/avatar/".formatted(userId);

        if (!objectKey.startsWith(expectedPrefix)) {
            throw new AvatarKeyMismatchException(objectKey, userId);
        }

        // HEAD check — verify the object was actually uploaded
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(storageProperties.bucket())
                    .key(objectKey)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new AvatarNotUploadedException(objectKey);
        } catch (SdkException e) {
            LOGGER.error("S3 unavailable during avatar confirm for key: {}", objectKey, e);
            throw new StorageUnavailableException(e);
        }

        // Validate content type
        String actualContentType = head.contentType();
        if (actualContentType != null && !ALLOWED_CONTENT_TYPES.contains(actualContentType)) {
            throw new InvalidAvatarContentTypeException(actualContentType);
        }

        // Validate size
        if (head.contentLength() != null && head.contentLength() > storageProperties.avatarMaxSizeBytes()) {
            deleteObjectAsync(objectKey);
            throw new AvatarNotUploadedException("File too large: %d bytes (max %d)"
                    .formatted(head.contentLength(), storageProperties.avatarMaxSizeBytes()));
        }

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        String oldKey = profile.getAvatarObjectKey();

        profile.updateAvatar(objectKey);
        userProfileRepository.save(profile);

        // Async-delete old avatar if it existed
        if (oldKey != null) {
            deleteObjectAsync(oldKey);
        }

        String avatarUrl = storageProperties.publicUrlBase() + "/" + objectKey;
        return new AvatarConfirmResponse(avatarUrl);
    }

    @Transactional
    public void removeAvatar(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        String oldKey = profile.getAvatarObjectKey();

        profile.clearAvatar();
        userProfileRepository.save(profile);

        if (oldKey != null) {
            deleteObjectAsync(oldKey);
        }
    }

    private void deleteObjectAsync(String objectKey) {
        avatarCleanupExecutor.execute(() -> {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(storageProperties.bucket())
                        .key(objectKey)
                        .build());
            } catch (Exception e) {
                LOGGER.warn("Failed to delete old avatar object: {}", objectKey, e);
            }
        });
    }
}
