package com.pixelmcn.mazeeconomy.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    // Matches numbers followed by s, m, h, d, w, or M
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwwM])");

    /**
     * Parses a shorthand time string into server ticks (20 ticks = 1 second).
     * Supported: s (sec), m (min), h (hour), d (day), w (week), M (month=30d).
     * Example: "10m" -> 12000 ticks.
     */
    public static long parseTicks(String input) {
        if (input == null || input.isEmpty())
            return 0;

        Matcher matcher = TIME_PATTERN.matcher(input);
        long totalTicks = 0;
        boolean matched = false;

        while (matcher.find()) {
            matched = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s" -> totalTicks += value * 20L;
                case "m" -> totalTicks += value * 60L * 20L;
                case "h" -> totalTicks += value * 60L * 60L * 20L;
                case "d" -> totalTicks += value * 24L * 60L * 60L * 20L;
                case "w" -> totalTicks += value * 7L * 24L * 60L * 60L * 20L;
                case "M" -> totalTicks += value * 30L * 24L * 60L * 60L * 20L; // 30 days
            }
        }

        // Fallback: If no letters matched, assume it was raw ticks.
        if (!matched && totalTicks == 0) {
            try {
                totalTicks = Long.parseLong(input);
            } catch (NumberFormatException ignored) {
            }
        }

        return totalTicks;
    }
}
