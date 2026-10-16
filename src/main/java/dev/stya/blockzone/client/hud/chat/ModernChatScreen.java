package dev.stya.blockzone.client.hud.chat;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.client.ui.*;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

/** Native chat input; vanilla retains outgoing-message validation, commands and sent history. */
public final class ModernChatScreen extends ModernUiScreen {
    private final Panel panel;
    private final ChatMessagePanel messages = new ChatMessagePanel();
    private ChatScreen sender;
    private int scroll, history;
    private String draft = "";
    public ModernChatScreen(String initial) {
        super(Component.translatable("chat_screen.title"));
        panel = new Panel(this, initial);
    }
    @Override protected void init() {
        super.init();
        sender = new ChatScreen("");
        sender.init(minecraft, width, height);
        history = minecraft.gui.getChat().getRecentChat().size();
    }
    @Override public Fragment getFragment() { return panel; }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        var recorded = new ModernUiGraphics();
        if (minecraft.options.chatVisibility().get() != net.minecraft.world.entity.player.ChatVisiblity.HIDDEN)
            messages.render(recorded, ChatHud.messages(), minecraft.player.getUUID(), net.minecraft.Util.getMillis(), 1, true, scroll);
        panel.frames = java.util.List.of(recorded.frame());
        if (panel.overlay != null) panel.overlay.postInvalidate();
        float pixels = HudCanvas.scale() * (float)minecraft.getWindow().getGuiScale();
        float panelWidth = Math.min(ChatMessagePanel.WIDTH, minecraft.getWindow().getWidth() / pixels - ChatMessagePanel.X * 2);
        panel.position(pixels, panelWidth);
        super.render(graphics, x, y, partial);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        scroll = Math.max(0, Math.min(Math.max(0, ChatHud.messages().size() - 1), scroll + (int)Math.signum(delta) * 3));
        return true;
    }
    private void send(String text) {
        if (minecraft.screen != this) return;
        sender.handleChatInput(text, true);
        if (minecraft.screen == this) onClose();
    }
    private void history(int direction, String current) {
        var entries = minecraft.gui.getChat().getRecentChat();
        if (history == entries.size()) draft = current;
        history = Math.max(0, Math.min(entries.size(), history + direction));
        panel.value(history == entries.size() ? draft : entries.get(history));
    }
    private void complete(String text) {
        if (!text.startsWith("/") || minecraft.getConnection() == null) return;
        var dispatcher = minecraft.getConnection().getCommands();
        dispatcher.getCompletionSuggestions(dispatcher.parse(text.substring(1), minecraft.getConnection().getSuggestionsProvider()))
                .thenAccept(result -> {
                    if (!result.isEmpty()) panel.value("/" + result.getList().get(0).apply(text.substring(1)));
                });
    }
    public static final class Panel extends Fragment {
        private final ModernChatScreen screen;
        private String text;
        private volatile java.util.List<ModernUiGraphics.Frame> frames = java.util.List.of();
        private volatile ModernUiView overlay;
        private EditText input;
        private float lastScale, lastWidth;
        private Panel(ModernChatScreen screen, String initial) { this.screen = screen; this.text = initial; }
        private void value(String text) {
            var view = input;
            if (view != null) view.post(() -> { view.setText(text); view.setSelection(view.getText().length()); });
        }
        private void position(float scale, float width) {
            var view = input;
            if (view == null || scale == lastScale && width == lastWidth) return;
            lastScale = scale; lastWidth = width;
            view.post(() -> {
                var params = new FrameLayout.LayoutParams(Math.round((width - 8) * scale),
                        Math.round(ChatMessagePanel.INPUT_HEIGHT * scale), Gravity.TOP | Gravity.LEFT);
                params.leftMargin = Math.round((ChatMessagePanel.X + 4) * scale);
                params.topMargin = Math.round((ChatMessagePanel.Y + ChatMessagePanel.EXPANDED_HEIGHT - ChatMessagePanel.INPUT_HEIGHT) * scale);
                view.setLayoutParams(params);
                view.setTextSize(icyllis.modernui.resources.TypedValue.COMPLEX_UNIT_PX, 20 * scale);
                int padding = Math.round(4 * scale);
                view.setPadding(padding, 0, padding, 0);
            });
        }
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, DataSet saved) {
            var root = new FrameLayout(requireContext());
            overlay = new ModernUiView(requireContext(), () -> frames);
            root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
            input = new EditText(requireContext());
            input.setSingleLine(); input.setTextSize(16); input.setTextColor(HudCanvas.TEXT);
            input.setHint(Component.translatable("chat.editBox").getString());
            input.setFilters(new icyllis.modernui.text.InputFilter.LengthFilter(256));
            input.setPadding(input.dp(10), input.dp(4), input.dp(10), input.dp(4));
            input.setBackground(null);
            input.setText(text);
            input.addTextChangedListener(new icyllis.modernui.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { text = s.toString(); }
                @Override public void afterTextChanged(icyllis.modernui.text.Editable s) {}
            });
            var params = new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT);
            root.addView(input, params);
            input.setOnKeyListener((v, key, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                String text = input.getText().toString();
                var mc = Minecraft.getInstance();
                if (key == KeyEvent.KEY_ENTER || key == KeyEvent.KEY_KP_ENTER) {
                    mc.execute(() -> screen.send(text)); return true;
                }
                if (key == KeyEvent.KEY_UP || key == KeyEvent.KEY_DOWN) {
                    mc.execute(() -> screen.history(key == KeyEvent.KEY_UP ? -1 : 1, text)); return true;
                }
                if (key == KeyEvent.KEY_TAB) { mc.execute(() -> screen.complete(text)); return true; }
                return false;
            });
            input.post(() -> { input.requestFocus(); input.setSelection(input.getText().length()); });
            return root;
        }
        @Override public void onDestroyView() { overlay = null; input = null; lastScale = lastWidth = 0; super.onDestroyView(); }
    }
}
