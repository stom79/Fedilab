package app.fedilab.android.mastodon.helper;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class EmojiAnimationLifecycle implements RecyclerView.OnChildAttachStateChangeListener {

    private static final EmojiAnimationLifecycle INSTANCE = new EmojiAnimationLifecycle();

    public static void attach(RecyclerView recyclerView) {
        recyclerView.addOnChildAttachStateChangeListener(INSTANCE);
    }

    @Override
    public void onChildViewAttachedToWindow(@NonNull View view) {
        applyToAllTextViews(view, true);
    }

    @Override
    public void onChildViewDetachedFromWindow(@NonNull View view) {
        applyToAllTextViews(view, false);
    }

    private void applyToAllTextViews(View view, boolean start) {
        if (view instanceof TextView) {
            if (start) {
                CustomEmoji.startAnimations((TextView) view);
            } else {
                CustomEmoji.stopAnimations((TextView) view);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyToAllTextViews(group.getChildAt(i), start);
            }
        }
    }
}
