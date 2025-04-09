package de.adorsys.keycloak.config.util;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PaginationUtil {
    private static final int DEFAULT_PAGE_SIZE = 20;

    public static <T> Stream<T> findAll(BiFunction<Integer, Integer, List<T>> getPage) {
        return findAll(DEFAULT_PAGE_SIZE, getPage);
    }

    public static <T> Stream<T> findAll(int pageSize, BiFunction<Integer, Integer, List<T>> getPage) {
        return IntStream.iterate(0, i -> i + 1)
                .mapToObj(i -> getPage.apply(i * pageSize, pageSize))
                .takeWhile(r -> !r.isEmpty())
                .flatMap(List::stream);
    }
}
