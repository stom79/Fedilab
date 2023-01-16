package de.timfreiheit.mathjax.android;

import android.content.res.TypedArray;
import android.os.Build;
import android.webkit.JavascriptInterface;

/**
 * http://docs.mathjax.org/en/latest/options/
 * for more information
 * <p>
 * Created by timfreiheit on 06.06.15.
 */
public class MathJaxConfig {

    private String input = Input.TeX.value;
    private String output = Output.SVG.value;
    private int outputScale = 100;
    private int minScaleAdjust = 100;
    private boolean automaticLinebreaks = false;
    private int blacker = 1;
    private String textColor;

    public MathJaxConfig() {
        if (Build.VERSION.SDK_INT >= 14) {
            output = Output.SVG.value;
        } else {
            output = Output.HTML_CSS.value;
        }
    }


    public MathJaxConfig(TypedArray attrs) {
        this();
        int inputIndex = attrs.getInteger(R.styleable.MathJaxView_input, -1);
        if (inputIndex > 0) {
            setInput(Input.values()[inputIndex]);
        }
        int outputIndex = attrs.getInteger(R.styleable.MathJaxView_output, -1);
        if (outputIndex > 0) {
            setOutput(Output.values()[outputIndex]);
        }
        setAutomaticLinebreaks(attrs.getBoolean(R.styleable.MathJaxView_automaticLinebreaks, automaticLinebreaks));
        setMinScaleAdjust(attrs.getInteger(R.styleable.MathJaxView_minScaleAdjust, minScaleAdjust));
        setOutputScale(attrs.getInteger(R.styleable.MathJaxView_outputScale, outputScale));
        setBlacker(attrs.getInteger(R.styleable.MathJaxView_blacker, blacker));
    }

    @JavascriptInterface
    public String getInput() {
        return input;
    }

    public void setInput(Input input) {
        this.input = input.value;
    }

    public String getTextColor() {
        return this.textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    @JavascriptInterface
    public String getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output.value;
    }

    @JavascriptInterface
    public int getOutputScale() {
        return outputScale;
    }

    public void setOutputScale(int outputScale) {
        this.outputScale = outputScale;
    }

    @JavascriptInterface
    public int getMinScaleAdjust() {
        return minScaleAdjust;
    }

    public void setMinScaleAdjust(int scale) {
        this.minScaleAdjust = scale;
    }

    @JavascriptInterface
    public boolean getAutomaticLinebreaks() {
        return automaticLinebreaks;
    }

    public void setAutomaticLinebreaks(boolean b) {
        this.automaticLinebreaks = b;
    }

    @JavascriptInterface
    public int getBlacker() {
        return blacker;
    }

    public void setBlacker(int blacker) {
        this.blacker = blacker;
    }

    public enum Output {
        SVG("output/SVG"),
        HTML_CSS("output/HTML-CSS"),
        CommonHTML("output/CommonHTML"),
        NativeMML("output/NativeMML");

        final String value;

        Output(String s) {
            value = s;
        }
    }

    public enum Input {
        TeX("input/TeX"),
        MathML("input/MathML"),
        AsciiMath("input/AsciiMath");

        final String value;

        Input(String s) {
            value = s;
        }
    }

}
