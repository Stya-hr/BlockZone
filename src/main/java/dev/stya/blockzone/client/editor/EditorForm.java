package dev.stya.blockzone.client.editor;

import icyllis.modernui.core.Context;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.InputFilter;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.CheckBox;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.LinearLayout;
import java.util.function.Consumer;

import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

/** UI-thread form state; programmatic synchronization never emits draft changes. */
final class EditorForm {
    private final Context context;
    private final EditorControls controls;
    private final Consumer<Runnable> dispatch;
    private final Consumer<Boolean> focus;
    private LinearLayout body;
    private boolean syncing, helpOpen;
    private final java.util.List<EditText> fields = new java.util.ArrayList<>();
    EditorForm(Context context, EditorControls controls, Consumer<Runnable> dispatch, Consumer<Boolean> focus, boolean helpOpen) {
        this.context = context; this.controls = controls; this.dispatch = dispatch; this.focus = focus; this.helpOpen = helpOpen;
    }
    boolean helpOpen() { return helpOpen; }
    void reset(LinearLayout body) { this.body = body; fields.clear(); }
    private void action(Runnable callback) { dispatch.accept(callback); }
    EditText field(LinearLayout parent, String caption, String value, int max, Consumer<String> changed) {
        var box = controls.column();
        var boxParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1);
        boxParams.setMargins(0, 0, box.dp(4), box.dp(5)); parent.addView(box, boxParams);
        controls.label(box, caption, 12);
        var field = new EditText(context); field.setSingleLine(); field.setTextSize(13);
        field.setMinHeight(field.dp(30));
        field.setPadding(field.dp(6), field.dp(4), field.dp(6), field.dp(4));
        EditorTheme.background(field, 0xff101820); field.setFilters(new InputFilter.LengthFilter(max)); field.setText(value);
        field.setOnFocusChangeListener((v, focused) -> focus.accept(focused));
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
    void syncValues(java.util.List<String> values) {
        if (fields.size() != values.size()) return;
        syncing = true;
        try { for (int i = 0; i < values.size(); i++) fields.get(i).setText(values.get(i)); }
        finally { syncing = false; }
    }
    void check(String caption, boolean value, Consumer<Boolean> changed) {
        var check = new CheckBox(context); check.setText(caption); check.setTextSize(12); check.setChecked(value);
        check.setOnCheckedChangeListener((v, selected) -> action(() -> changed.accept(selected)));
        body.addView(check, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }
    void help(String caption, String... descriptions) {
        var content = controls.column();
        for (String description : descriptions) controls.label(content, description, 12);
        content.setVisibility(helpOpen ? View.VISIBLE : View.GONE);
        var toggle = new Button(context); toggle.setText(caption); toggle.setTextSize(12);
        EditorTheme.styleButton(toggle);
        toggle.setOnClickListener(v -> { helpOpen = !helpOpen; content.setVisibility(helpOpen ? View.VISIBLE : View.GONE); });
        body.addView(toggle, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        body.addView(content, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }
}
