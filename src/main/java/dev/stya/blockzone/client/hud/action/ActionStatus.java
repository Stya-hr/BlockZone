package dev.stya.blockzone.client.hud.action;

import dev.stya.blockzone.client.hud.common.HudProgress;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/** Render data only. A null progress is a key prompt, not a timed operation. */
public record ActionStatus(Component title, Component detail, KeyMapping key, HudProgress progress, int color) {}
