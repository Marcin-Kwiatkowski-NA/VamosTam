package com.vamigo.ride;

import com.vamigo.domain.AbstractExternalMeta;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ride_external_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RideExternalMeta extends AbstractExternalMeta {

    @OneToOne
    @MapsId
    @JoinColumn(name = "ride_id")
    private Ride ride;

    public Long getRideId() {
        return getId();
    }
}
