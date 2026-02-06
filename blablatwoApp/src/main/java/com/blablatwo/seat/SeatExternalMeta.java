package com.blablatwo.seat;

import com.blablatwo.domain.AbstractExternalMeta;
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
@Table(name = "seat_external_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
