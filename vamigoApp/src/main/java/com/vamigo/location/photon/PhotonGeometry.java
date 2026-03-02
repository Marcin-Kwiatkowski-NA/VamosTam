package com.vamigo.location.photon;

import java.util.List;

public record PhotonGeometry(
        String type,
        List<Double> coordinates
) {
}
