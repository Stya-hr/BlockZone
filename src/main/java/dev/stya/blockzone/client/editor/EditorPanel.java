package dev.stya.blockzone.client.editor;

import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.GradientDrawable;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.InputFilter;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

/** All views live on Modern UI's thread; all draft mutations are queued on the game thread. */
public final class EditorPanel extends Fragment {
    private final ModernEditorScreen screen;
    private volatile FrameLayout root;
    private LinearLayout body;
    private TextView status;
    private Button save;
    private volatile boolean updatePending;
    private ListView listView;
    private RowAdapter listAdapter;
    private java.util.List<ListRow> lastRows;
    record ListRow(String label, boolean selected, Runnable click) {}
    private int builtWidth, builtHeight;
    private float builtDensity, builtScaledDensity;
    private boolean helpOpen;
    private boolean syncing;
    private final java.util.List<EditText> fields = new java.util.ArrayList<>();

    EditorPanel(ModernEditorScreen screen) { this.screen = screen; }
    static String tr(String key, Object... args) { return Component.translatable(key, args).getString(); }
    void action(Runnable action) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen == screen) action.run();
        });
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, DataSet saved) {
        root = new FrameLayout(requireContext());
        root.setFocusableInTouchMode(true);
        rebuild();
        return root;
    }
    @Override public void onDestroyView() { root = null; screen.textFocused = false; super.onDestroyView(); }
    void refresh() {
        var view = root;
        if (view != null) view.post(() -> { if (root == view) rebuild(); });
    }
    void clearTextFocus() {
        var view = root;
        if (view != null) view.post(() -> { if (root == view) view.requestFocus(); });
    }
    void update() {
        var view = root;
        if (view == null || updatePending) return;
        updatePending = true;
        // Snapshot game-owned state before crossing to the UI thread.
        screen.updatePanel(this);
        view.post(() -> {
            if (root == view) {
                var metrics = requireContext().getResources().getDisplayMetrics();
                if (view.getWidth() != builtWidth || view.getHeight() != builtHeight
                        || metrics.density != builtDensity || metrics.scaledDensity != builtScaledDensity) rebuild();
            }
            updatePending = false;
        });
    }
    void status(String value, boolean canSave, boolean busy) {
        var view = root;
        if (view != null) view.post(() -> {
            if (root != view) return;
            if (status != null && !status.getText().toString().equals(value)) status.setText(value);
            if (save != null) save.setEnabled(canSave && !busy);
            if (body != null) enable(body, !busy);
        });
    }
    private void enable(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) enable(group.getChildAt(i), enabled);
    }
    private void rebuild() {
        root.removeAllViews();
        fields.clear();
        listView = null; listAdapter = null; lastRows = null;
        screen.textFocused = false;
        var panel = column();
        panel.setPadding(panel.dp(10), panel.dp(8), panel.dp(10), panel.dp(8));
        background(panel, 0xe8182028);
        var params = new FrameLayout.LayoutParams(panelWidth(), MATCH_PARENT, Gravity.RIGHT);
        root.addView(panel, params);
        panel.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            int target = panelWidth();
            if (params.width != target) { params.width = target; panel.setLayoutParams(params); }
            screen.panelFraction = Math.min(1f, (float) target / Math.max(1, root.getWidth()));
            if (listView != null) sizeList();
        });
        label(panel, screen.getTitle().getString(), 15);
        var scroll = new ScrollView(requireContext());
        scroll.setFillViewport(false);
        panel.addView(scroll, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
        body = column(); scroll.addView(body, new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        screen.buildPanel(this);
        status = label(panel, "", 12); status.setTextColor(0xffb8c8d8);
        divider(panel);
        var footer = row(panel);
        button(footer, tr(screen.hasSaveAction() ? "editor.blockzone.exit" : "gui.back"), screen::onClose);
        save = screen.hasSaveAction() ? button(footer, tr("editor.blockzone.save"), this::screenSave) : null;
        if (save != null) accent(save, 0xff70b7ff);
        builtWidth = root.getWidth(); builtHeight = root.getHeight();
        var metrics = requireContext().getResources().getDisplayMetrics();
        builtDensity = metrics.density; builtScaledDensity = metrics.scaledDensity;
        action(() -> screen.updatePanel(this));
    }
    private int panelWidth() {
        return root.getWidth() == 0 ? root.dp(280) : Math.max(1, Math.min(root.dp(280), Math.round(root.getWidth() * .35f)));
    }
    private void screenSave() {
        if (screen instanceof PoisonEditorScreen poison) poison.save();
        else if (screen instanceof LootCrateEditorScreen loot) loot.save();
    }
    LinearLayout body() { return body; }
    FrameLayout root() { return root; }
    LinearLayout column() { var v = new LinearLayout(requireContext()); v.setOrientation(LinearLayout.VERTICAL); return v; }
    LinearLayout row(LinearLayout parent) {
        var row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(row, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)); return row;
    }
    private void divider(LinearLayout parent) {
        var line = new View(requireContext()); background(line, 0xff34414e);
        var params = new LinearLayout.LayoutParams(MATCH_PARENT, line.dp(1));
        params.setMargins(0, line.dp(6), 0, line.dp(6)); parent.addView(line, params);
    }
    void section(String key) {
        divider(body);
        var title = label(body, tr("editor.blockzone.ui." + key), 12);
        title.setTextColor(0xff83b8e7);
    }
    private void accent(Button button, int color) {
        button.setTextColor(new icyllis.modernui.util.ColorStateList(
                new int[][]{{-icyllis.modernui.R.attr.state_enabled}, {}},
                new int[]{0xff687581, color}));
    }
    Button danger(Button button) { accent(button, 0xffff9b93); return button; }
    TextView label(LinearLayout parent, String value, int size) {
        var label = new TextView(requireContext()); label.setText(value); label.setTextSize(size); label.setTextColor(0xffe7eef4);
        label.setPadding(0, label.dp(4), 0, label.dp(4));
        parent.addView(label, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)); return label;
    }
    Button button(LinearLayout parent, String value, Runnable handler) {
        var button = new Button(requireContext()); button.setText(value); button.setTextSize(12);
        button.setMinHeight(button.dp(30)); button.setMinimumWidth(0);
        button.setOnClickListener(v -> action(handler));
        var params = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1); params.setMargins(0, 0, button.dp(3), button.dp(3));
        parent.addView(button, params); return button;
    }
    EditText field(LinearLayout parent, String caption, String value, int max, Consumer<String> changed) {
        var box = column();
        var boxParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1);
        boxParams.setMargins(0, 0, box.dp(4), box.dp(5)); parent.addView(box, boxParams);
        label(box, caption, 12);
        var field = new EditText(requireContext()); field.setSingleLine(); field.setTextSize(13);
        field.setMinHeight(field.dp(30));
        field.setPadding(field.dp(6), field.dp(4), field.dp(6), field.dp(4));
        background(field, 0xff101820); field.setFilters(new InputFilter.LengthFilter(max)); field.setText(value);
        field.setOnFocusChangeListener((v, focused) -> screen.textFocused = focused);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (!syncing) { String value = s.toString(); action(() -> changed.accept(value)); }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        fields.add(field);
        box.addView(field, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)); return field;
    }
    void showList(java.util.List<ListRow> rows, boolean visible) {
        var view = root;
        if (view == null) return;
        view.post(() -> {
            if (root != view) return;
            if (!visible) { if (listView != null) listView.setVisibility(View.GONE); return; }
            if (listView == null) {
                listView = new ListView(requireContext());
                background(listView, 0xe8182028);
                listAdapter = new RowAdapter(); listView.setAdapter(listAdapter);
                listView.setOnItemClickListener((parent, child, position, id) -> action(listAdapter.rows.get(position).click()));
                root.addView(listView, new FrameLayout.LayoutParams(1, MATCH_PARENT, Gravity.LEFT));
                sizeList();
            }
            listView.setVisibility(View.VISIBLE);
            // The game supplies immutable labels and selection state; callbacks are never evaluated on the UI thread.
            boolean changed = lastRows == null || lastRows.size() != rows.size();
            if (!changed) for (int i = 0; i < rows.size(); i++) {
                if (!rows.get(i).label().equals(lastRows.get(i).label()) || rows.get(i).selected() != lastRows.get(i).selected()) { changed = true; break; }
            }
            if (changed) { listAdapter.rows = rows; lastRows = rows; listAdapter.notifyDataSetChanged(); }
        });
    }
    private void sizeList() {
        var params = (FrameLayout.LayoutParams) listView.getLayoutParams();
        int gap = root.dp(8);
        params.width = Math.max(1, Math.round(root.getWidth() * (1-screen.panelFraction)) - gap*2);
        params.setMargins(gap, gap, gap, gap); listView.setLayoutParams(params);
    }
    private final class RowAdapter extends BaseAdapter {
        java.util.List<ListRow> rows = java.util.List.of();
        @Override public int getCount() { return rows.size(); }
        @Override public ListRow getItem(int position) { return rows.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View recycled, ViewGroup parent) {
            var label = recycled instanceof TextView t ? t : new TextView(requireContext());
            var row = rows.get(position);
            label.setText(row.label()); label.setTextSize(13); label.setTextColor(0xffe7eef4);
            label.setPadding(label.dp(10), label.dp(8), label.dp(10), label.dp(8));
            background(label, row.selected() ? 0xff355d46 : 0x00182028);
            return label;
        }
    }
    void syncValues(java.util.List<String> values) {
        var view = root;
        if (view != null) view.post(() -> {
            if (root != view || fields.size() != values.size()) return;
            syncing = true;
            try { for (int i = 0; i < values.size(); i++) fields.get(i).setText(values.get(i)); }
            finally { syncing = false; }
        });
    }
    void check(String caption, boolean value, Consumer<Boolean> changed) {
        var check = new CheckBox(requireContext()); check.setText(caption); check.setTextSize(12); check.setChecked(value);
        check.setOnCheckedChangeListener((v, selected) -> action(() -> changed.accept(selected)));
        body.addView(check, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }
    void help(String... keys) {
        var content = column();
        for (String key : keys) label(content, tr(key), 12);
        content.setVisibility(helpOpen ? View.VISIBLE : View.GONE);
        var toggle = new Button(requireContext()); toggle.setText(tr("editor.blockzone.ui.help")); toggle.setTextSize(12);
        toggle.setOnClickListener(v -> { helpOpen = !helpOpen; content.setVisibility(helpOpen ? View.VISIBLE : View.GONE); });
        body.addView(toggle, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        body.addView(content, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }
    static void background(View view, int color) {
        var bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(view.dp(6)); view.setBackground(bg);
    }
}
