package de.timfreiheit.mathjax.android;

import android.webkit.JavascriptInterface;

/**
 * Bridge to enable callbacks for MathJax
 * Wrapped MathJaxView to disable global visibility of {@link MathJaxView#laTexRendered()}
 * <p>
 * Created by timfreiheit on 30.05.15.
 */
class MathJaxJavaScriptBridge {

    MathJaxView mOwner;

    public MathJaxJavaScriptBridge(MathJaxView owner) {
        this.mOwner = owner;
    }

    @JavascriptInterface
    public void rendered() {
        mOwner.rendered();
    }

}
