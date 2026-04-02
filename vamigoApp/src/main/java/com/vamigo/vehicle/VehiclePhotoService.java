package com.vamigo.vehicle;

import com.vamigo.config.StorageProperties;
import com.vamigo.user.exception.InvalidAvatarContentTypeException;
import com.vamigo.user.exception.AvatarNotUploadedException;
import com.vamigo.user.exception.StorageUnavailableException;
import com.vamigo.vehicle.dto.VehiclePhotoConfirmResponse;
import com.vamigo.vehicle.dto.VehiclePhotoPresignResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class VehiclePhotoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VehiclePhotoService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final StorageProperties storageProperties;
    private final VehicleRepository vehicleRepository;
    private final VehiclePhotoUrlResolver photoUrlResolver;
    private final TaskExecutor cleanupExecutor;

    public VehiclePhotoService(
            S3Presigner s3Presigner,
            S3Client s3Client,
            StorageProperties storageProperties,
            VehicleRepository vehicleRepository,
            VehiclePhotoUrlResolver photoUrlResolver,
            @Qualifier("avatarCleanupExecutor") TaskExecutor cleanupExecutor
    ) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
        this.vehicleRepository = vehicleRepository;
        this.photoUrlResolver = photoUrlResolver;
        this.cleanupExecutor = cleanupExecutor;
    }

    public VehiclePhotoPresignResponse generatePresignedUrl(Long userId, Long vehicleId, String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidAvatarContentTypeException(contentType);
        }

        vehicleRepository.findByIdAndOwnerId(vehicleId, userId)
                .orElseThrow(() -> new NoSuchElementException("Vehicle with ID " + vehicleId + " not found."));

        String extension = CONTENT_TYPE_EXTENSIONS.get(contentType);
        String objectKey = "vehicles/%d/photo/%s.%s".formatted(vehicleId, UUID.randomUUID(), extension);

        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(storageProperties.vehiclePhotoPresignTtlMinutes()))
                .putObjectRequest(put -> put
                        .bucket(storageProperties.bucket())
                        .key(objectKey)
                        .contentType(contentType))
                .build();

        var presignedUrl = s3Presigner.presignPutObject(presignRequest);

        return new VehiclePhotoPresignResponse(
                presignedUrl.url().toString(),
                objectKey,
                storageProperties.vehiclePhotoMaxSizeBytes()
        );
    }

    @Transactional
    public VehiclePhotoConfirmResponse confirmPhoto(Long userId, Long vehicleId, String objectKey) {
        Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(vehicleId, userId)
                .orElseThrow(() -> new NoSuchElementException("Vehicle with ID " + vehicleId + " not found."));

        String expectedPrefix = "vehicles/%d/photo/".formatted(vehicleId);
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Object key does not belong to this vehicle");
        }

        HeadObjectResponse head;
        try {
            head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(storageProperties.bucket())
                    .key(objectKey)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new AvatarNotUploadedException(objectKey);
        } catch (SdkException e) {
            LOGGER.error("S3 unavailable during vehicle photo confirm for key: {}", objectKey, e);
            throw new StorageUnavailableException(e);
        }

        String actualContentType = head.contentType();
        if (actualContentType != null && !ALLOWED_CONTENT_TYPES.contains(actualContentType)) {
            throw new InvalidAvatarContentTypeException(actualContentType);
        }

        if (head.contentLength() != null && head.contentLength() > storageProperties.vehiclePhotoMaxSizeBytes()) {
            deleteObjectAsync(objectKey);
            throw new AvatarNotUploadedException("File too large: %d bytes (max %d)"
                    .formatted(head.contentLength(), storageProperties.vehiclePhotoMaxSizeBytes()));
        }

        String oldKey = vehicle.getPhotoObjectKey();
        vehicle.setPhotoObjectKey(objectKey);
        vehicleRepository.save(vehicle);

        if (oldKey != null) {
            deleteObjectAsync(oldKey);
        }

        String photoUrl = photoUrlResolver.resolve(objectKey);
        return new VehiclePhotoConfirmResponse(photoUrl);
    }

    @Transactional
    public void removePhoto(Long userId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(vehicleId, userId)
                .orElseThrow(() -> new NoSuchElementException("Vehicle with ID " + vehicleId + " not found."));

        String oldKey = vehicle.getPhotoObjectKey();
        vehicle.setPhotoObjectKey(null);
        vehicleRepository.save(vehicle);

        if (oldKey != null) {
            deleteObjectAsync(oldKey);
        }
    }

    private void deleteObjectAsync(String objectKey) {
        cleanupExecutor.execute(() -> {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(storageProperties.bucket())
                        .key(objectKey)
                        .build());
            } catch (Exception e) {
                LOGGER.warn("Failed to delete old vehicle photo object: {}", objectKey, e);
            }
        });
    }
}
