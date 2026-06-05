package app.fedilab.android.mastodon.helper;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class EmojiRecyclerView extends RecyclerView {

    public EmojiRecyclerView(@NonNull Context context) {
        super(context);
        init();
    }

    public EmojiRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EmojiRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        EmojiAnimationLifecycle.attach(this);
    }
}
