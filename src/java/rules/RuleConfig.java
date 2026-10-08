package rules;

import java.math.BigDecimal;

public final class RuleConfig {

    private RuleConfig() {
    }

    public static final int DUPLICATE_WINDOW_SECONDS = 60;
    public static final int BURST_WINDOW_SECONDS = 60;
    public static final int BURST_LIMIT = 5;
    public static final BigDecimal HIGH_AMOUNT = new BigDecimal("5000000");
}
