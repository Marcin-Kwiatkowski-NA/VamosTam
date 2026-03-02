package com.vamigo.location.photon;

import java.util.List;

public record PhotonResponse(
        List<PhotonFeature> features
) {
}
