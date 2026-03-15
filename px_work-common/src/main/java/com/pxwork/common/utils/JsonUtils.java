package com.pxwork.common.utils;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static String cleanMarkdownJson(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            if (firstLineEnd > -1) {
                text = text.substring(firstLineEnd + 1).trim();
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3).trim();
            }
        }
        if (text.startsWith("`") && text.endsWith("`") && text.length() > 1) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return text;
    }
}
