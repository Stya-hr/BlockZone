package dev.stya.blockzone.chat;

public enum ChatChannel {
    ALL, SQUAD;
    public String translationKey() { return "chat.blockzone." + name().toLowerCase(java.util.Locale.ROOT); }
}
