package org.mateof24.rpg_tweaks.util;

public final class FoodTickTracker {
    public static final ThreadLocal<Boolean> INSIDE_FOOD_TICK = ThreadLocal.withInitial(() -> false);
}