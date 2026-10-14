package dev.stya.blockzone.client.inventory;

/** Translation IDs identify physical keys independently of language and registered actions. */
final class InventoryKeyIcons {
    static String icon(String key) {
        return switch (key) {
            case "key.mouse.left" -> "mouse_left";
            case "key.mouse.right" -> "mouse_right";
            case "key.mouse.middle" -> "mouse_middle";
            case "key.keyboard.left.shift", "key.keyboard.right.shift" -> "shift";
            case "key.keyboard.left.control", "key.keyboard.right.control" -> "control";
            case "key.keyboard.left.alt", "key.keyboard.right.alt" -> "alt";
            case "key.keyboard.space" -> "space";
            case "key.keyboard.enter", "key.keyboard.keypad.enter" -> "enter";
            case "key.keyboard.tab" -> "tab";
            case "key.keyboard.backspace" -> "backspace";
            case "key.keyboard.up" -> "up";
            case "key.keyboard.down" -> "down";
            case "key.keyboard.left" -> "left";
            case "key.keyboard.right" -> "right";
            default -> null;
        };
    }
    private InventoryKeyIcons() {}
}
