package dev.stya.blockzone.client.ui;

import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import java.util.List;

/** Native screen content with the same snapshot boundary as the passive HUD. */
public final class ModernUiPanel extends Fragment {
    private volatile List<ModernUiGraphics.Frame> frames=List.of();
    private volatile ModernUiView view;
    public void update(ModernUiGraphics graphics) {
        frames=List.of(graphics.frame());
        var current=view;if(current!=null)current.postInvalidate();
    }
    @Override public View onCreateView(LayoutInflater inflater,ViewGroup container,DataSet saved) {
        view=new ModernUiView(requireContext(),()->frames);return view;
    }
    @Override public void onDestroyView(){view=null;frames=List.of();super.onDestroyView();}
}
