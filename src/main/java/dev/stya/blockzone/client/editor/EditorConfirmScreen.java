package dev.stya.blockzone.client.editor;

import dev.stya.blockzone.client.ui.ModernUiScreen;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/** Shared native confirmation for editor drafts, including synthetic coordinate input. */
public final class EditorConfirmScreen extends ModernUiScreen {
    private final Consumer<Boolean> decision;
    private final Panel panel;
    private boolean answered;
    private EditorConfirmScreen(Consumer<Boolean> decision,Component title,Component message) {
        super(title);this.decision=decision;panel=new Panel(this,title.getString(),message.getString());
    }
    static Screen create(Consumer<Boolean> decision,Component title,Component message) {
        return new EditorConfirmScreen(decision,title,message);
    }
    private void answer(boolean yes) {if(!answered){answered=true;decision.accept(yes);}}
    @Override public Fragment getFragment(){return panel;}
    @Override public void onClose(){answer(false);}

    public static final class Panel extends Fragment {
        private final EditorConfirmScreen screen;
        private final String title,message;
        private Panel(EditorConfirmScreen screen,String title,String message){this.screen=screen;this.title=title;this.message=message;}
        @Override public View onCreateView(LayoutInflater inflater,ViewGroup container,DataSet saved) {
            var root=new FrameLayout(requireContext());
            root.setBackground(new icyllis.modernui.graphics.drawable.ColorDrawable(0x99000000));
            var body=new LinearLayout(requireContext());body.setOrientation(LinearLayout.VERTICAL);
            int padding=body.dp(16);body.setPadding(padding,padding,padding,padding);
            EditorTheme.background(body,0xF0182028);
            var controls=new EditorControls(requireContext(),r->Minecraft.getInstance().execute(r));
            controls.label(body,title,18);controls.label(body,message,14);
            var row=controls.row(body);
            controls.danger(controls.button(row,Component.translatable("gui.yes").getString(),()->screen.answer(true)));
            controls.button(row,Component.translatable("gui.no").getString(),()->screen.answer(false));
            root.addView(body,new FrameLayout.LayoutParams(body.dp(360),-2,Gravity.CENTER));
            return root;
        }
    }
}
