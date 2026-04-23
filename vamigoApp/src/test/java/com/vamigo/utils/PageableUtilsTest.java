package com.vamigo.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageableUtils")
class PageableUtilsTest {

    @Nested
    @DisplayName("withStableSort(Pageable)")
    class WithStablePageable {

        @Test
        @DisplayName("Appends id ASC when the pageable's sort does not already reference id")
        void appendsIdAsc_whenAbsent() {
            Pageable input = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "departureTime"));

            Pageable result = PageableUtils.withStableSort(input);

            assertThat(result.getSort()).containsExactly(
                    new Sort.Order(Sort.Direction.ASC, "departureTime"),
                    new Sort.Order(Sort.Direction.ASC, "id")
            );
        }

        @Test
        @DisplayName("Returns the input unchanged when id is already part of the sort")
        void isIdempotent_whenIdAlreadyPresent() {
            Pageable input = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

            Pageable result = PageableUtils.withStableSort(input);

            assertThat(result).isSameAs(input);
        }

        @Test
        @DisplayName("Preserves page number and page size")
        void preservesPageNumberAndSize() {
            Pageable input = PageRequest.of(3, 7, Sort.by(Sort.Direction.ASC, "departureTime"));

            Pageable result = PageableUtils.withStableSort(input);

            assertThat(result.getPageNumber()).isEqualTo(3);
            assertThat(result.getPageSize()).isEqualTo(7);
        }

        @Test
        @DisplayName("Produces id ASC as the sole order when the input was unsorted")
        void producesIdOnly_whenInputUnsorted() {
            Pageable input = PageRequest.of(0, 10);

            Pageable result = PageableUtils.withStableSort(input);

            assertThat(result.getSort()).containsExactly(
                    new Sort.Order(Sort.Direction.ASC, "id")
            );
        }

        @Test
        @DisplayName("Returns unpaged input unchanged")
        void returnsUnpagedUnchanged() {
            Pageable input = Pageable.unpaged();

            Pageable result = PageableUtils.withStableSort(input);

            assertThat(result).isSameAs(input);
        }
    }

    @Nested
    @DisplayName("withStableSort(Sort)")
    class WithStableSortOnly {

        @Test
        @DisplayName("Appends id ASC when missing")
        void appendsIdAsc_whenAbsent() {
            Sort result = PageableUtils.withStableSort(Sort.by(Sort.Direction.DESC, "departureTime"));

            assertThat(result).containsExactly(
                    new Sort.Order(Sort.Direction.DESC, "departureTime"),
                    new Sort.Order(Sort.Direction.ASC, "id")
            );
        }

        @Test
        @DisplayName("Preserves any explicit id direction already in the sort")
        void preservesExistingId() {
            Sort input = Sort.by(Sort.Order.asc("departureTime"), Sort.Order.desc("id"));

            Sort result = PageableUtils.withStableSort(input);

            assertThat(result).isSameAs(input);
        }
    }
}
