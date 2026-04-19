package com.vamigo.seat;

import com.vamigo.domain.AbstractExternalMeta;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "seat_external_meta")
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@SuperBuilder
public class SeatExternalMeta extends AbstractExternalMeta {

    @OneToOne
    @MapsId
    @JoinColumn(name = "seat_id")
    private Seat seat;

    public Long getSeatId() {
        return getId();
    }
}
