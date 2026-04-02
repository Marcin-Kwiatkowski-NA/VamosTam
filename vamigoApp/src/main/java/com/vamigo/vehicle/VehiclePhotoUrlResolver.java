package com.vamigo.vehicle;

import com.vamigo.config.StorageProperties;
import org.springframework.stereotype.Component;

@Component
public class VehiclePhotoUrlResolver {

    private final StorageProperties storageProperties;

    public VehiclePhotoUrlResolver(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public String resolve(Vehicle vehicle) {
        if (vehicle == null || vehicle.getPhotoObjectKey() == null) {
            return null;
        }
        return storageProperties.publicUrlBase() + "/" + vehicle.getPhotoObjectKey();
    }

    public String resolve(String photoObjectKey) {
        if (photoObjectKey == null) {
            return null;
        }
        return storageProperties.publicUrlBase() + "/" + photoObjectKey;
    }
}
