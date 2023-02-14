package com.github.stom79.mytransl.async;
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

import android.os.Handler;
import android.os.Looper;

import com.github.stom79.mytransl.MyTransL;
import com.github.stom79.mytransl.client.Client;
import com.github.stom79.mytransl.client.HttpsConnectionException;
import com.github.stom79.mytransl.client.Results;
import com.github.stom79.mytransl.translate.Helper;
import com.github.stom79.mytransl.translate.Params;
import com.github.stom79.mytransl.translate.Translate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;


/**
 * Created by @stom79 on 27/11/2017.
 * Asynchronous task to get the translation
 * Changed 10/01/2021
 */

public class TransAsync {

    private final Results listener;
    private final MyTransL.translatorEngine te;
    private final int timeout;
    private final Translate translate;
    private final boolean obfuscation;
    private final String contentToSend;
    private final String toLanguage;
    private final Params params;
    private Params.fType format;
    private HttpsConnectionException e;

    public TransAsync(MyTransL.translatorEngine te, String content, Params.fType format, String toLanguage, int timeout, boolean obfuscation, Results results) {
        this.listener = results;
        this.te = te;
        this.timeout = timeout;
        this.obfuscation = obfuscation;
        //An instance of the Translate class will be hydrated depending of the translator engine
        translate = new Translate();
        translate.setTranslatorEngine(te);
        translate.setInitialContent(content);
        translate.setTargetedLanguage(toLanguage);
        translate.setFormat(format);
        //Obfuscation if asked
        if (obfuscation)
            translate.obfuscate();
        if (obfuscation) {
            contentToSend = translate.getObfuscateContent();
        } else {
            contentToSend = translate.getInitialContent();
        }
        this.toLanguage = toLanguage;
        this.params = new Params();

        new Thread(() -> {
            String response = doInBackground();
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> onPostExecute(response);
            mainHandler.post(myRunnable);
        }).start();
    }

    public TransAsync(MyTransL.translatorEngine te, String content, String toLanguage, Params params, int timeout, boolean obfuscation, Results results) {
        this.listener = results;
        this.te = te;
        this.timeout = timeout;
        this.obfuscation = obfuscation;
        //An instance of the Translate class will be hydrated depending of the translator engine
        translate = new Translate();
        translate.setTranslatorEngine(te);
        translate.setInitialContent(content);
        translate.setTargetedLanguage(toLanguage);
        //Obfuscation if asked
        if (obfuscation) {
            translate.obfuscate();
        }
        if (obfuscation) {
            contentToSend = translate.getObfuscateContent();
        } else {
            contentToSend = translate.getInitialContent();
        }
        this.toLanguage = toLanguage;
        this.params = params;
        new Thread(() -> {
            String response = doInBackground();
            Handler mainHandler = new Handler(Looper.getMainLooper());
            MyTransL.getLocale();
            Runnable myRunnable = () -> onPostExecute(response);
            mainHandler.post(myRunnable);
        }).start();
    }


    protected String doInBackground() {
        String str_response = null;
        //Some parameters
        try {
            String url;

            if (te == MyTransL.translatorEngine.YANDEX) {
                String key = MyTransL.getInstance(te).getYandexAPIKey();
                url = Helper.getYandexAbsoluteUrl(contentToSend, key, toLanguage);
                str_response = new Client().get(url, this.timeout);
            } else if (te == MyTransL.translatorEngine.DEEPL) {
                String key = MyTransL.getInstance(te).getDeeplAPIKey();
                url = Helper.getDeeplAbsoluteUrl(contentToSend, toLanguage, params, key);
                str_response = new Client().get(url, this.timeout);
            } else if (te == MyTransL.translatorEngine.SYSTRAN) {
                String key = MyTransL.getInstance(te).getSystranAPIKey();
                url = Helper.getSystranAbsoluteUrl(contentToSend, key, toLanguage);
                str_response = new Client().get(url, this.timeout);
            } else if (te == MyTransL.translatorEngine.LIBRETRANSLATE) {
                String key = MyTransL.getInstance(te).getLibreTranslateAPIKey();
                JSONObject params = new JSONObject();
                try {
                    params.put("source", this.params.getSource_lang());
                    params.put("target", toLanguage);
                    params.put("q", contentToSend);
                    params.put("format", format);
                    if (key != null) {
                        params.put("key", key);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                str_response = new Client().post(MyTransL.getLibreTranslateUrl(), this.timeout, params);
            } else if (te == MyTransL.translatorEngine.LINGVA) {
                String key = MyTransL.getInstance(te).getLibreTranslateAPIKey();
                String contentToSendEncoded = URLEncoder.encode(contentToSend, "UTF-8");
                String lingvaURL = MyTransL.getLingvaUrl() + this.params.getSource_lang() + "/" + toLanguage + "/" + contentToSendEncoded;
                str_response = new Client().get(lingvaURL, this.timeout);
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException err) {
            this.e = new HttpsConnectionException(-1, err.getMessage());
            err.printStackTrace();
        } catch (HttpsConnectionException e) {
            this.e = e;
        }
        return str_response;
    }

    protected void onPostExecute(String result) {
        if (this.e == null) {
            //Yandex response
            if (this.te == MyTransL.translatorEngine.YANDEX) {
                translate.parseYandexResult(result, listener);
            } else if (this.te == MyTransL.translatorEngine.DEEPL) {
                translate.parseDeeplResult(result, listener);
            } else if (this.te == MyTransL.translatorEngine.SYSTRAN) {
                translate.parseSystranlResult(result, listener);
            } else if (this.te == MyTransL.translatorEngine.LIBRETRANSLATE) {
                translate.parseLibreTranslateResult(result, listener);
            } else if (this.te == MyTransL.translatorEngine.LINGVA) {
                translate.parseLingvaResult(result, listener);
            }
            //Obfuscation if asked
            if (obfuscation) {
                translate.deobfuscate();
            }
            listener.onSuccess(translate);
        } else {
            listener.onFail(this.e);
        }
    }

}
