package de.timfreiheit.mathjax.android;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;


/**
 * Containerview for an WebView which renders LaTex using MathJax
 * https://github.com/mathjax/MathJax
 * Created by timfreiheit on 26.05.15.
 */
public class MathJaxView extends FrameLayout {

    private static final String HTML_LOCATION = "file:///android_asset/MathJaxAndroid/mathjax_android.html";
    private final Handler handler = new Handler();
    private final Context context;
    protected MathJaxJavaScriptBridge mBridge;
    private String inputText = null;
    private WebView mWebView;
    /**
     * laTex can only be rendered when WebView is already loaded
     */
    private boolean webViewLoaded = false;
    private OnMathJaxRenderListener onMathJaxRenderListener;

    public MathJaxView(Context context) {
        super(context);
        this.context = context;
        init(context, null, null);
    }

    public MathJaxView(Context context, MathJaxConfig config) {
        super(context);
        this.context = context;
        init(context, null, config);
    }

    public MathJaxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context, attrs, null);
    }

    public MathJaxView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(context, attrs, null);
    }

    public void setColor(String color) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                }
            }

        }, 500); // 5000ms delay
    }

    public void forceDark() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mWebView.getSettings().setForceDark(WebSettings.FORCE_DARK_ON);
        }
    }

    public void setRenderListener(OnMathJaxRenderListener onMathJaxRenderListener) {
        this.onMathJaxRenderListener = onMathJaxRenderListener;
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void init(Context context, AttributeSet attrSet, MathJaxConfig config) {
        mWebView = new WebView(context);

        int gravity = Gravity.START;
        boolean verticalScrollbarsEnabled = false;
        boolean horizontalScrollbarsEnabled = true;
        String textColor = config != null ? config.getTextColor() : null;

        if (attrSet != null) {
            TypedArray attrs = context.obtainStyledAttributes(attrSet, R.styleable.MathJaxView);
            gravity = attrs.getInteger(R.styleable.MathJaxView_android_gravity, Gravity.START);
            verticalScrollbarsEnabled = attrs.getBoolean(R.styleable.MathJaxView_verticalScrollbarsEnabled, false);
            horizontalScrollbarsEnabled = attrs.getBoolean(R.styleable.MathJaxView_horizontalScrollbarsEnabled, true);
            textColor = attrs.getString(R.styleable.MathJaxView_textColor);
            config = new MathJaxConfig(attrs);
            attrs.recycle();
        }
        if (config == null) {
            config = new MathJaxConfig();
        }

        addView(
                mWebView,
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        gravity
                )
        );

        // callback when WebView is loading completed
        webViewLoaded = false;
        String finalTextColor = textColor;
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && finalTextColor != null) {
                    view.evaluateJavascript("document.body.style.color=\"" + finalTextColor + "\";", null);
                }
                if (webViewLoaded) {
                    // WebView was already finished
                    // do not load content again
                    return;
                }
                webViewLoaded = true;
                if (!TextUtils.isEmpty(inputText)) {
                    setInputText(inputText);
                }
            }
        });

        mBridge = new MathJaxJavaScriptBridge(this);
        mWebView.addJavascriptInterface(mBridge, "Bridge");
        mWebView.addJavascriptInterface(config, "BridgeConfig");

        // be careful, we do not need internet access
        mWebView.getSettings().setBlockNetworkLoads(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(HTML_LOCATION);
        mWebView.setVerticalScrollBarEnabled(verticalScrollbarsEnabled);
        mWebView.setHorizontalScrollBarEnabled(horizontalScrollbarsEnabled);
        mWebView.setBackgroundColor(0);
    }

    /**
     * called when webView is ready with rendering LaTex
     */
    protected void rendered() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mWebView.setVisibility(View.VISIBLE);
                if (onMathJaxRenderListener != null)
                    onMathJaxRenderListener.onRendered();
            }
        }, 100);
    }

    /**
     * @return the current laTex-String
     * null if not set
     */
    public String getInputText() {
        return inputText;
    }

    /**
     * change the displayed LaTex
     *
     * @param inputText formatted string
     */
    public void setInputText(String inputText) {
        this.inputText = inputText;

        //wait for WebView to finish loading
        if (!webViewLoaded) {
            return;
        }

        String laTexString;
        if (inputText != null) {
            laTexString = doubleEscapeTeX(inputText);
        } else {
            laTexString = "";
        }

        mWebView.setVisibility(View.INVISIBLE);

        String javascriptCommand = "javascript:changeLatexText(\"" + laTexString + "\")";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(javascriptCommand, null);
        } else {
            mWebView.loadUrl(javascriptCommand);
        }

    }

    private String doubleEscapeTeX(String s) {
        String t = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\'') t += '\\';
            if (s.charAt(i) == '\\') t += "\\";
            if (s.charAt(i) != '\n') t += s.charAt(i);
        }
        return t;
    }

    public interface OnMathJaxRenderListener {
        void onRendered();

    }
}
