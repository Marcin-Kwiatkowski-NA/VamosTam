package com.vamigo.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pagination plumbing helpers. Kept separate from {@link SortMappingUtil}
 * (which translates API sort strings to {@link Sort}) so each class has a
 * single responsibility.
 */
public final class PageableUtils {

    /** Property appended as the last {@link Sort.Order} to break ties. */
    public static final String TIEBREAKER_PROPERTY = "id";

    private PageableUtils() {
    }

    /**
     * Returns a {@link Pageable} whose sort ends with {@code id ASC} so that
     * rows with equal values in the preceding sort keys have a deterministic
     * order across paginated queries. Idempotent: if the current sort already
     * references {@code id}, the input pageable is returned unchanged.
     */
    public static Pageable withStableSort(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        Sort stable = withStableSort(pageable.getSort());
        if (stable == pageable.getSort()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stable);
    }

    /**
     * Returns a {@link Sort} that ends with {@code id ASC}. Idempotent.
     */
    public static Sort withStableSort(Sort sort) {
        boolean alreadyStable = sort.stream()
                .anyMatch(o -> TIEBREAKER_PROPERTY.equals(o.getProperty()));
        if (alreadyStable) {
            return sort;
        }
        return sort.and(Sort.by(Sort.Direction.ASC, TIEBREAKER_PROPERTY));
    }
}
