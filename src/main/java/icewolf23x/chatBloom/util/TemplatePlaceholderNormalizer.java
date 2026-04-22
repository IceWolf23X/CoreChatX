package icewolf23x.chatBloom.util;

import java.util.regex.Pattern;

public final class TemplatePlaceholderNormalizer {

    private static final Pattern BRACED_PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_.-]+)}");

    private TemplatePlaceholderNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return BRACED_PLACEHOLDER.matcher(input).replaceAll("<$1>");
    }
}
