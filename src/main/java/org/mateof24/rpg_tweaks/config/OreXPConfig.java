package org.mateof24.rpg_tweaks.config;

import java.util.List;

public class OreXPConfig {

    public java.util.Map<String, XPValues> blockConfigs = new java.util.HashMap<>();
    public java.util.Map<String, XPValues> tagConfigs = new java.util.HashMap<>();

    public XPValues getConfigForBlock(String blockId, List<String> blockTags) {
        if (blockConfigs.containsKey(blockId)) {
            return blockConfigs.get(blockId);
        }
        for (String tag : blockTags) {
            if (tagConfigs.containsKey(tag)) {
                return tagConfigs.get(tag);
            }
        }
        return null;
    }

    public void validate() {
        if (blockConfigs != null) {
            blockConfigs.values().forEach(XPValues::validate);
        }
        if (tagConfigs != null) {
            tagConfigs.values().forEach(XPValues::validate);
        }
    }
}