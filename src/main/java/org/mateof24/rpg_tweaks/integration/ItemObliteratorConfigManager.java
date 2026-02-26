package org.mateof24.rpg_tweaks.integration;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemObliteratorConfigManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("item_obliterator.json5");

    private static final Pattern BLACKLIST_ARRAY_PATTERN = Pattern.compile(
            "\"blacklisted_items\"\\s*:\\s*\\[(.*?)\\]",
            Pattern.DOTALL
    );


    public static boolean isItemObliteratorInstalled() {
        return Files.exists(CONFIG_PATH);
    }


    public static boolean addToBlacklist(String itemId) {
        try {
            String content = Files.readString(CONFIG_PATH);
            Matcher matcher = BLACKLIST_ARRAY_PATTERN.matcher(content);
            if (!matcher.find()) {
                LOGGER.error("[ItemObliterator] The array 'blacklisted_items' was not found in {}",
                        CONFIG_PATH);
                return false;
            }

            String arrayBody = matcher.group(1);
            if (isItemAlreadyListed(arrayBody, itemId)) {
                LOGGER.info("[ItemObliterator] The item '{}' is already in blacklisted_items", itemId);
                return false;
            }

            String newArrayBody = appendItemToArrayBody(arrayBody, itemId);
            String newContent = content.substring(0, matcher.start(1))
                    + newArrayBody
                    + content.substring(matcher.end(1));
            Files.writeString(CONFIG_PATH, newContent);
            LOGGER.info("[ItemObliterator] Item '{}' added to blacklisted_items", itemId);
            return true;
        } catch (IOException e) {
            LOGGER.error("[ItemObliterator] I/O error modifying {}: {}",
                    CONFIG_PATH, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            LOGGER.error("[ItemObliterator] Unexpected error: {}", e.getMessage(), e);
            return false;
        }
    }

    public static boolean removeFromBlacklist(String itemId) {
        try {
            String content = Files.readString(CONFIG_PATH);

            Matcher matcher = BLACKLIST_ARRAY_PATTERN.matcher(content);
            if (!matcher.find()) {
                LOGGER.error("[ItemObliterator] Array 'blacklisted_items' not found");
                return false;
            }

            String arrayBody = matcher.group(1);

            if (!isItemAlreadyListed(arrayBody, itemId)) {
                LOGGER.info("[ItemObliterator] The item '{}' is not in blacklisted_items", itemId);
                return false;
            }

            String newArrayBody = removeItemFromArrayBody(arrayBody, itemId);

            String newContent = content.substring(0, matcher.start(1))
                    + newArrayBody
                    + content.substring(matcher.end(1));

            Files.writeString(CONFIG_PATH, newContent);

            LOGGER.info("[ItemObliterator] Item '{}' removed from blacklisted_items", itemId);
            return true;

        } catch (IOException e) {
            LOGGER.error("[ItemObliterator] I/O error: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            LOGGER.error("[ItemObliterator] Unexpected error: {}", e.getMessage(), e);
            return false;
        }
    }

    public static boolean isBlacklisted(String itemId) {
        try {
            String content = Files.readString(CONFIG_PATH);
            Matcher matcher = BLACKLIST_ARRAY_PATTERN.matcher(content);
            if (!matcher.find()) return false;
            return isItemAlreadyListed(matcher.group(1), itemId);
        } catch (Exception e) {
            LOGGER.error("[ItemObliterator] Error querying blacklist: {}", e.getMessage());
            return false;
        }
    }

    public static List<String> getBlacklist() {
        List<String> items = new ArrayList<>();
        try {
            String content = Files.readString(CONFIG_PATH);
            Matcher matcher = BLACKLIST_ARRAY_PATTERN.matcher(content);
            if (!matcher.find()) return items;
            Matcher entryMatcher = Pattern.compile("\"([^\"]+)\"")
                    .matcher(matcher.group(1));
            while (entryMatcher.find()) {
                items.add(entryMatcher.group(1));
            }
        } catch (Exception e) {
            LOGGER.error("[ItemObliterator] Error reading blacklist: {}", e.getMessage());
        }
        return items;
    }

    public static boolean isValidItemId(String itemId) {
        if (itemId == null || !itemId.contains(":")) return false;
        String[] parts = itemId.split(":", 2);
        return parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank();
    }

    private static boolean isItemAlreadyListed(String arrayBody, String itemId) {
        Pattern exactMatch = Pattern.compile("\"" + Pattern.quote(itemId) + "\"");
        return exactMatch.matcher(arrayBody).find();
    }

    private static String appendItemToArrayBody(String arrayBody, String itemId) {
        String indent = detectIndent(arrayBody);
        boolean hasEntries = Pattern.compile("\"[^\"]+\"").matcher(arrayBody).find();

        if (!hasEntries) {
            return "\n" + indent + "\"" + itemId + "\"\n  ";
        }

        int lastQuoteClose = arrayBody.lastIndexOf('"');
        if (lastQuoteClose == -1) {
            return arrayBody + ",\n" + indent + "\"" + itemId + "\"\n  ";
        }

        String afterLastQuote = arrayBody.substring(lastQuoteClose + 1);
        boolean hasTrailingComma = afterLastQuote.stripLeading().startsWith(",");

        String separator = hasTrailingComma ? "" : ",";
        String tail = arrayBody.substring(lastQuoteClose + 1);

        return arrayBody.substring(0, lastQuoteClose + 1)
                + separator
                + "\n" + indent + "\"" + itemId + "\""
                + tail;
    }

    private static String removeItemFromArrayBody(String arrayBody, String itemId) {
        String quotedItem = Pattern.quote("\"" + itemId + "\"");
        String result = arrayBody.replaceAll(
                "[ \\t]*" + quotedItem + "[ \\t]*,[ \\t]*\\r?\\n?", "");
        if (result.equals(arrayBody)) {
            result = arrayBody.replaceAll(
                    ",[ \\t]*\\r?\\n[ \\t]*" + quotedItem + "[ \\t]*\\r?\\n?", "\n");
        }
        if (result.equals(arrayBody)) {
            result = arrayBody.replaceAll(
                    "[ \\t]*" + quotedItem + "[ \\t]*\\r?\\n?", "");
        }

        return result;
    }

    private static String detectIndent(String arrayBody) {
        Matcher m = Pattern.compile("(?m)^([ \\t]+)\"").matcher(arrayBody);
        if (m.find()) {
            return m.group(1);
        }
        return "    ";
    }
}