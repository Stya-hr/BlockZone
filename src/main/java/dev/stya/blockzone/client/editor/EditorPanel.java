package dev.stya.blockzone.client.editor;

import icyllis.modernui.fragment.Fragment;
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
    private EditorStatusBar status;
    private Button save;
    private volatile boolean updatePending;
    private ListView listView;
    private EditorRowAdapter listAdapter;
    private java.util.List<ListRow> lastRows;
    record ListRow(String label, boolean selected, Runnable click) {}
    private int builtWidth, builtHeight;
    private float builtDensity, builtScaledDensity;
    private EditorControls controls;
    private EditorForm form;

    EditorPanel(ModernEditorScreen screen) { this.screen = screen; }
    static String tr(String key, Object... args) { return Component.translatable(key, args).getString(); }
    void action(Runnable action) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen == screen) action.run();
        });
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, DataSet saved) {
        controls = new EditorControls(requireContext(), this::action);
        form = new EditorForm(requireContext(), controls, this::action, focused -> screen.textFocused = focused, form != null && form.helpOpen());
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
            if (status != null) status.show(value);
            if (listView != null) sizeList();
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
        listView = null; listAdapter = null; lastRows = null;
        screen.textFocused = false;
        var sidebar = new EditorSidebar(requireContext(), controls, screen.getTitle().getString(),
                tr(screen.hasSaveAction() ? "editor.blockzone.exit" : "gui.back"), tr("editor.blockzone.save"),
                screen::onClose, screen.hasSaveAction() ? screen::save : null);
        var panel = sidebar.view;
        body = sidebar.body; save = sidebar.save;
        form.reset(body);
        var params = new FrameLayout.LayoutParams(panelWidth(), MATCH_PARENT, Gravity.RIGHT);
        int gap = root.dp(8);
        params.setMargins(0, gap, gap, gap);
        root.addView(panel, params);
        status = new EditorStatusBar(controls, root);
        status.view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> { if (listView != null) sizeList(); });
        panel.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            int target = panelWidth();
            if (params.width != target) { params.width = target; panel.setLayoutParams(params); }
            screen.panelFraction = Math.min(1f, (float) (target + gap) / Math.max(1, root.getWidth()));
            status.resize(Math.max(0, root.getWidth() - target - gap));
            if (listView != null) sizeList();
        });
        screen.buildPanel(this);
        builtWidth = root.getWidth(); builtHeight = root.getHeight();
        var metrics = requireContext().getResources().getDisplayMetrics();
        builtDensity = metrics.density; builtScaledDensity = metrics.scaledDensity;
        action(() -> screen.updatePanel(this));
    }
    private int panelWidth() {
        return root.getWidth() == 0 ? root.dp(280) : Math.max(1, Math.min(root.dp(280), Math.round(root.getWidth() * .35f) - root.dp(8)));
    }
    LinearLayout body() { return body; }
    FrameLayout root() { return root; }
    LinearLayout column() { return controls.column(); }
    LinearLayout row(LinearLayout parent) { return controls.row(parent); }
    void section(String key) { controls.section(body, tr("editor.blockzone.ui." + key)); }
    Button danger(Button button) { return controls.danger(button); }
    TextView label(LinearLayout parent, String value, int size) { return controls.label(parent, value, size); }
    Button button(LinearLayout parent, String value, Runnable handler) { return controls.button(parent, value, handler); }
    EditText field(LinearLayout parent, String caption, String value, int max, Consumer<String> changed) {
        return form.field(parent, caption, value, max, changed);
    }
    void check(String caption, boolean value, Consumer<Boolean> changed) { form.check(caption, value, changed); }
    void help(String... keys) {
        form.help(tr("editor.blockzone.ui.help"), java.util.Arrays.stream(keys).map(EditorPanel::tr).toArray(String[]::new));
    }
    void showList(java.util.List<ListRow> rows, boolean visible) {
        var view = root;
        if (view == null) return;
        view.post(() -> {
            if (root != view) return;
            if (!visible) { if (listView != null) listView.setVisibility(View.GONE); return; }
            if (listView == null) {
                listView = new ListView(requireContext());
                EditorTheme.background(listView, 0xe8182028);
                listAdapter = new EditorRowAdapter(requireContext()); listView.setAdapter(listAdapter);
                listView.setOnItemClickListener((parent, child, position, id) -> action(listAdapter.rows.get(position).click()));
                root.addView(listView, new FrameLayout.LayoutParams(1, MATCH_PARENT, Gravity.LEFT));
                sizeList();
                root.bringChildToFront(status.view);
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
        int width = Math.max(1, Math.round(root.getWidth() * (1-screen.panelFraction)) - gap*2);
        int bottom = status.listBottomMargin();
        if (params.width != width || params.bottomMargin != bottom) {
            params.width = width; params.setMargins(gap, gap, gap, bottom); listView.setLayoutParams(params);
        }
    }
    void syncValues(java.util.List<String> values) {
        var view = root;
        if (view != null) view.post(() -> { if (root == view) form.syncValues(values); });
    }
}
