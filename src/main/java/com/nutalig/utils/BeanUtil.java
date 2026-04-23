package com.nutalig.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanUtil {

    /**
     * <p>Example</p>
     * Customer customer = BeanUtils.safeGetter(() -> Customer().gtNameTh()).orElse(new Customer());
     *
     * @param <T>
     * @param resolver
     * @return
     */
    public static <T> Optional<T> safeGetter(Supplier<T> resolver) {
        try {
            final T result = resolver.get();
            return Optional.ofNullable(result);
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    public static boolean isDifferent(String existingValue, String newValue) {
        return !Objects.equals(existingValue, newValue);
    }

}
