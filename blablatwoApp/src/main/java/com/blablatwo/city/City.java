package com.blablatwo.city;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name = "idx_city_norm_name_pl", columnList = "normNamePl"),
        @Index(name = "idx_city_norm_name_en", columnList = "normNameEn")
})
public class City {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private Long placeId;

    @NotNull
    @Column(nullable = false)
    private String namePl;

    private String nameEn;

    @NotNull
    @Column(nullable = false)
    private String normNamePl;

    private String normNameEn;

    @Column(length = 2)
    private String countryCode;

    private Long population;

    /**
     * Get the display name for the city in the requested language.
     *
     * @param lang Language code ("pl" or "en")
     * @return Name in the requested language, falling back to Polish if not available
     */
    public String getDisplayName(String lang) {
        if ("en".equalsIgnoreCase(lang) && nameEn != null && !nameEn.isBlank()) {
            return nameEn;
        }
        return namePl;
    }

    @Override
    public String toString() {
        return "City{id=%d, placeId=%d, namePl='%s', nameEn='%s'}".formatted(id, placeId, namePl, nameEn);
    }
}
