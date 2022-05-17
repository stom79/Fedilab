package app.fedilab.android.helper;

import static app.fedilab.android.ui.drawer.ComposeAdapter.autocomplete;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.CallSuper;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.emoji.Emoji;

import app.fedilab.android.R;
import app.fedilab.android.interfaces.EmojiEditTextInterface;

public class FedilabAutoCompleteTextView extends MaterialAutoCompleteTextView implements EmojiEditTextInterface {

    private float emojiSize;
    private boolean emoji;
    private String[] imgTypeString;
    private KeyBoardInputCallbackListener keyBoardInputCallbackListener;
    final InputConnectionCompat.OnCommitContentListener callback =
            new InputConnectionCompat.OnCommitContentListener() {
                @Override
                public boolean onCommitContent(@NonNull InputContentInfoCompat inputContentInfo,
                                               int flags, Bundle opts) {

                    // read and display inputContentInfo asynchronously
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && (flags &
                            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                        try {
                            inputContentInfo.requestPermission();
                        } catch (Exception e) {
                            return false; // return false if failed
                        }
                    }
                    boolean supported = false;
                    for (final String mimeType : imgTypeString) {
                        if (inputContentInfo.getDescription().hasMimeType(mimeType)) {
                            supported = true;
                            break;
                        }
                    }
                    if (!supported) {
                        return false;
                    }

                    if (keyBoardInputCallbackListener != null) {
                        keyBoardInputCallbackListener.onCommitContent(inputContentInfo, flags, opts);
                    }
                    return true;  // return true if succeeded
                }
            };


    public FedilabAutoCompleteTextView(Context context) {
        super(context);
        initView();
    }

    public FedilabAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        final float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        emoji = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_EMOJI), false);
        if (attrs == null) {
            emojiSize = defaultEmojiSize;
        } else {
            final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiMultiAutoCompleteTextView);

            try {
                emojiSize = a.getDimension(R.styleable.EmojiMultiAutoCompleteTextView_emojiSize, defaultEmojiSize);
            } finally {
                a.recycle();
            }
        }

        setText(getText());
        initView();
    }

    public FedilabAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        final InputConnection ic = super.onCreateInputConnection(outAttrs);
        EditorInfoCompat.setContentMimeTypes(outAttrs,
                imgTypeString);
        return InputConnectionCompat.createWrapper(ic, outAttrs, callback);
    }

    private void initView() {
        imgTypeString = new String[]{"image/png",
                "image/gif",
                "image/jpeg",
                "image/webp"};
    }

    public void setKeyBoardInputCallbackListener(KeyBoardInputCallbackListener keyBoardInputCallbackListener) {
        this.keyBoardInputCallbackListener = keyBoardInputCallbackListener;
    }

    @SuppressWarnings("unused")
    public String[] getImgTypeString() {
        return imgTypeString;
    }

    @SuppressWarnings("unused")
    public void setImgTypeString(String[] imgTypeString) {
        this.imgTypeString = imgTypeString;
    }

    @Override
    @CallSuper
    protected void onTextChanged(final CharSequence text, final int start, final int lengthBefore, final int lengthAfter) {
        final Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        final float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
        if (emoji && !autocomplete) {
            EmojiManager.getInstance().replaceWithImages(getContext(), getText(), emojiSize, defaultEmojiSize);
        }
    }

    @Override
    public void backspace() {
        final KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
        dispatchKeyEvent(event);
    }

    @Override
    public float getEmojiSize() {
        return emojiSize;
    }

    @Override
    public final void setEmojiSize(@Px final int pixels) {
        setEmojiSize(pixels, true);
    }

    @Override
    @CallSuper
    public void input(final Emoji emoji) {
        if (emoji != null && !autocomplete) {
            final int start = getSelectionStart();
            final int end = getSelectionEnd();

            if (start < 0) {
                append(emoji.getUnicode());
            } else {
                getText().replace(Math.min(start, end), Math.max(start, end), emoji.getUnicode(), 0, emoji.getUnicode().length());
            }
        }
    }

    @Override
    public final void setEmojiSize(@Px final int pixels, final boolean shouldInvalidate) {
        emojiSize = pixels;

        if (shouldInvalidate && !autocomplete) {
            setText(getText());
        }
    }

    @Override
    public final void setEmojiSizeRes(@DimenRes final int res) {
        setEmojiSizeRes(res, true);
    }

    @Override
    public final void setEmojiSizeRes(@DimenRes final int res, final boolean shouldInvalidate) {
        setEmojiSize(getResources().getDimensionPixelSize(res), shouldInvalidate);
    }

    public interface KeyBoardInputCallbackListener {
        void onCommitContent(InputContentInfoCompat inputContentInfo,
                             int flags, Bundle opts);
    }
}
