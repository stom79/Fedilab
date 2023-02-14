package com.github.stom79.mytransl.translate;
/* Copyright 2017 Thomas Schneider
 *
 * This file is a part of MyTransL
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * MyTransL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MyTransL; if not,
 * see <http://www.gnu.org/licenses>. */


import android.os.Build;
import android.text.Html;
import android.text.SpannableString;
import android.util.Patterns;

import com.github.stom79.mytransl.MyTransL;
import com.github.stom79.mytransl.client.HttpsConnectionException;
import com.github.stom79.mytransl.client.Results;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by @stom79 on 28/11/2017.
 * The class which manages the replies
 * Changed 10/01/2021
 */

@SuppressWarnings({"unused", "RedundantSuppression"})
public class Translate {

    private static final Pattern hashtagPattern = Pattern.compile("(#[\\w_À-ú-]+)");
    private static final Pattern mentionPattern = Pattern.compile("(@[\\w]+)");
    private static final Pattern mentionOtherInstancePattern = Pattern.compile("(@[\\w]*@[\\w.-]+)");
    private final Translate translate;
    private String targetedLanguage;
    private String initialLanguage;
    private MyTransL.translatorEngine translatorEngine;
    private String initialContent;
    private String obfuscateContent;
    private String translatedContent;
    private Params.fType format;
    private HashMap<String, String> tagConversion, mentionConversion, urlConversion, mailConversion;

    public Translate() {
        this.translate = this;
    }

    private static String replacer(StringBuffer outBuffer) throws UnsupportedEncodingException {
        String data = outBuffer.toString();
        data = data.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        data = data.replaceAll("\\+", "%2B");
        data = URLDecoder.decode(data, "utf-8");
        return data;
    }

    public Params.fType getFormat() {
        return format;
    }

    public void setFormat(Params.fType format) {
        this.format = format;
    }

    public String getTargetedLanguage() {
        return targetedLanguage;
    }

    public void setTargetedLanguage(String targetedLanguage) {
        this.targetedLanguage = targetedLanguage;
    }

    public String getInitialLanguage() {
        return initialLanguage;
    }

    private void setInitialLanguage(String initialLanguage) {
        this.initialLanguage = initialLanguage;
    }

    public String getInitialContent() {
        return initialContent;
    }

    public void setInitialContent(String initialContent) {
        this.initialContent = initialContent;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    private void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }

    public MyTransL.translatorEngine getTranslatorEngine() {
        return translatorEngine;
    }

    public void setTranslatorEngine(MyTransL.translatorEngine translatorEngine) {
        this.translatorEngine = translatorEngine;
    }

    public HashMap<String, String> getTagConversion() {
        return this.tagConversion;
    }

    public HashMap<String, String> getMentionConversion() {
        return this.mentionConversion;
    }

    public HashMap<String, String> getUrlConversion() {
        return this.urlConversion;
    }

    public HashMap<String, String> getMailConversion() {
        return this.mailConversion;
    }

    public String getObfuscateContent() {
        return this.obfuscateContent;
    }

    public void obfuscate() {

        this.tagConversion = new HashMap<>();
        this.mentionConversion = new HashMap<>();
        this.urlConversion = new HashMap<>();
        this.mailConversion = new HashMap<>();
        SpannableString spannableString;
        String content = this.translate.getInitialContent();
        content = content.replaceAll("\n", "<br/>");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spannableString = new SpannableString(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        } else {
            spannableString = new SpannableString(Html.fromHtml(content));
        }
        String text = spannableString.toString();
        Matcher matcher;

        //Mentions with instances (@name@domain) will be replaced by __o0__, __o1__, etc.
        int i = 0;
        matcher = mentionOtherInstancePattern.matcher(text);
        while (matcher.find()) {
            String key = "$o" + i;
            String value = matcher.group(0);
            if (value != null) {
                this.mentionConversion.put(key, value);
                text = text.replace(value, key);
            }
            i++;
        }
        //Extracts Emails
        matcher = Patterns.EMAIL_ADDRESS.matcher(text);
        i = 0;
        //replaces them by a kind of variable which shouldn't be translated ie: __e0__, __e1__, etc.
        while (matcher.find()) {
            String key = "$e" + i;
            String value = matcher.group(0);
            if (value != null) {
                this.mailConversion.put(key, value);
                text = text.replace(value, key);
            }
            i++;
        }

        //Same for mentions with __m0__, __m1__, etc.
        i = 0;
        matcher = mentionPattern.matcher(text);
        while (matcher.find()) {
            String key = "$m" + i;
            String value = matcher.group(0);
            if (value != null) {
                this.mentionConversion.put(key, value);
                text = text.replace(value, key);
            }
            i++;
        }

        //Extracts urls
        matcher = Patterns.WEB_URL.matcher(text);
        i = 0;
        //replaces them by a kind of variable which shouldn't be translated ie: __u0__, __u1__, etc.
        while (matcher.find()) {
            String key = "$u" + i;
            String value = matcher.group(0);
            int end = matcher.end();
            if (spannableString.length() > end && spannableString.charAt(end) == '/') {
                text = spannableString.toString().substring(0, end).
                        concat(spannableString.toString().substring(end + 1, spannableString.length()));
            }
            if (value != null) {
                this.urlConversion.put(key, value);
                text = text.replace(value, key);
            }
            i++;
        }
        i = 0;
        //Same for tags with __t0__, __t1__, etc.
        matcher = hashtagPattern.matcher(text);
        while (matcher.find()) {
            String key = "$t" + i;
            String value = matcher.group(0);
            if (value != null) {
                this.tagConversion.put(key, value);
                text = text.replace(value, key);
            }
            i++;
        }
        this.obfuscateContent = text;
    }

    public void deobfuscate() {
        String aJsonString = null;
        try {
            if (translatorEngine == MyTransL.translatorEngine.YANDEX)
                aJsonString = yandexTranslateToText(translatedContent);
            else
                aJsonString = translatedContent;
            if (aJsonString != null) {
                if (this.urlConversion != null) {
                    Iterator<Map.Entry<String, String>> itU = this.urlConversion.entrySet().iterator();
                    while (itU.hasNext()) {
                        Map.Entry<String, String> pair = itU.next();
                        aJsonString = aJsonString.replace(pair.getKey(), pair.getValue());
                        itU.remove();
                    }
                }
                if (this.tagConversion != null) {
                    Iterator<Map.Entry<String, String>> itT = this.tagConversion.entrySet().iterator();
                    while (itT.hasNext()) {
                        Map.Entry<String, String> pair = itT.next();
                        aJsonString = aJsonString.replace(pair.getKey(), pair.getValue());
                        itT.remove();
                    }
                }
                if (this.mentionConversion != null) {
                    Iterator<Map.Entry<String, String>> itM = this.mentionConversion.entrySet().iterator();
                    while (itM.hasNext()) {
                        Map.Entry<String, String> pair = itM.next();
                        aJsonString = aJsonString.replace(pair.getKey(), pair.getValue());
                        itM.remove();
                    }
                }
                if (this.mailConversion != null) {
                    Iterator<Map.Entry<String, String>> itE = this.mailConversion.entrySet().iterator();
                    while (itE.hasNext()) {
                        Map.Entry<String, String> pair = itE.next();
                        aJsonString = aJsonString.replace(pair.getKey(), pair.getValue());
                        itE.remove();
                    }
                }
            }
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        if (aJsonString != null)
            translatedContent = aJsonString;
    }

    private String yandexTranslateToText(String text) throws UnsupportedEncodingException {
        if (text == null)
            return null;
        /* The one instance where I've seen this happen,
            the special tag was originally a hashtag ("__t1__"),
            that Yandex decided to change to a "__q1 - __".
         */
        text = text.replaceAll("__q(\\d+) - __", "\\$t$1");
        // Noticed this in the very same toot
        text = text.replace("&amp;", "&");
        text = replacer(new StringBuffer(text));
        return text;
    }

    /***
     * Method to parse result coming from the Yandex translator
     * More about Yandex translate API - https://tech.yandex.com/translate/
     * @param response String - Response of the engine translator
     * @param listener - Results Listener
     */
    public void parseYandexResult(String response, Results listener) {
        translate.setTranslatorEngine(MyTransL.translatorEngine.YANDEX);
        try {
            JSONObject translationJson = new JSONObject(response);
            //Retrieves the translated content
            JSONArray aJsonArray = translationJson.getJSONArray("text");
            String aJsonString = aJsonArray.get(0).toString();
            aJsonString = aJsonString.replace("&amp;", "&");
            aJsonString = replacer(new StringBuffer(aJsonString));
            translate.setTranslatedContent(aJsonString);
            //Retrieves the translation direction
            String translationDirection = translationJson.get("lang").toString();
            String[] td = translationDirection.split("-");
            translate.setInitialLanguage(td[0]);
            translate.setTargetedLanguage(td[1]);
        } catch (JSONException | UnsupportedEncodingException e1) {
            HttpsConnectionException httpsConnectionException = new HttpsConnectionException(-1, e1.getMessage());
            listener.onFail(httpsConnectionException);
        }
    }

    /***
     * Method to parse result coming from the Libre Translate
     * @param response String - Response of the engine translator
     * @param listener - Results Listener
     */
    public void parseLibreTranslateResult(String response, Results listener) {
        translate.setTranslatorEngine(MyTransL.translatorEngine.LIBRETRANSLATE);
        try {
            JSONObject translationJson = new JSONObject(response);
            //Retrieves the translated content
            translate.setTranslatedContent(translationJson.getString("translatedText"));
            //Retrieves the initial language
            translate.setInitialLanguage(initialLanguage);
        } catch (JSONException e1) {
            e1.printStackTrace();
            HttpsConnectionException httpsConnectionException = new HttpsConnectionException(-1, e1.getMessage());
            listener.onFail(httpsConnectionException);
        }
    }


    /***
     * Method to parse result coming from the Lingva
     * @param response String - Response of the engine translator
     * @param listener - Results Listener
     */
    public void parseLingvaResult(String response, Results listener) {
        translate.setTranslatorEngine(MyTransL.translatorEngine.LINGVA);
        try {
            JSONObject translationJson = new JSONObject(response);
            //Retrieves the translated content
            translate.setTranslatedContent(translationJson.getString("translation"));
            //Retrieves the initial language
            translate.setInitialLanguage(initialLanguage);
        } catch (JSONException e1) {
            e1.printStackTrace();
            HttpsConnectionException httpsConnectionException = new HttpsConnectionException(-1, e1.getMessage());
            listener.onFail(httpsConnectionException);
        }
    }

    /***
     * Method to parse result coming from the Deepl translator
     * More about Deepl translate API - https://www.deepl.com/api-reference.html
     * @param response String - Response of the engine translator
     * @param listener - Results Listener
     */
    public void parseDeeplResult(String response, Results listener) {
        translate.setTranslatorEngine(MyTransL.translatorEngine.DEEPL);
        try {
            JSONObject translationJson = new JSONObject(response);
            //Retrieves the translated content
            JSONArray aJsonArray = translationJson.getJSONArray("translations");
            JSONObject aJsonString = aJsonArray.getJSONObject(0);
            translate.setTranslatedContent(aJsonString.getString("text"));
            //Retrieves the initial language
            translate.setInitialLanguage(initialLanguage);
        } catch (JSONException e1) {
            e1.printStackTrace();
            HttpsConnectionException httpsConnectionException = new HttpsConnectionException(-1, e1.getMessage());
            listener.onFail(httpsConnectionException);
        }
    }


    /***
     * Method to parse result coming from the Systrans translator
     * More about Systran translate API - https://platform.systran.net/reference/translation
     * @param response String - Response of the engine translator
     * @param listener - Results Listener
     */
    public void parseSystranlResult(String response, Results listener) {
        translate.setTranslatorEngine(MyTransL.translatorEngine.SYSTRAN);
        try {
            JSONObject translationJson = new JSONObject(response);
            //Retrieves the translated content
            JSONArray aJsonArray = translationJson.getJSONArray("outputs");
            JSONObject aJsonString = aJsonArray.getJSONObject(0);
            translate.setTranslatedContent(aJsonString.getString("output"));
            //Retrieves the initial language
            translate.setInitialLanguage(initialLanguage);
        } catch (JSONException e1) {
            e1.printStackTrace();
            HttpsConnectionException httpsConnectionException = new HttpsConnectionException(-1, e1.getMessage());
            listener.onFail(httpsConnectionException);
        }
    }
}
