package com.cloudledger.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monetary calculations utility using standard Banker's Rounding (HALF_EVEN)
 * and four decimal places precision (NUMERIC 19,4).
 */
public final class MoneyUtils {

    public static final int DEFAULT_SCALE = 4;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);

    private MoneyUtils() {}

    public static BigDecimal format(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        return amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isZero(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isGreaterThanOrEqual(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;
        return a.compareTo(b) >= 0;
    }
}
