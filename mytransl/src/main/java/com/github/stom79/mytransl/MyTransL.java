package com.github.stom79.mytransl;
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


import com.github.stom79.mytransl.async.TransAsync;
import com.github.stom79.mytransl.client.Results;
import com.github.stom79.mytransl.translate.Params;

import java.util.Locale;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class MyTransL {

    public static String TAG = "MyTrans_TAG";
    private static MyTransL myTransL;
    private static String libretranslateDomain;
    private static String lingvaDomain;
    private static String mintDomain;
    private translatorEngine te;
    private String yandexAPIKey, deeplAPIKey, systranAPIKey, libreTranslateAPIKey, lingvaAPIKey;
    private int timeout = 30;
    private boolean obfuscation = false;

    private MyTransL(translatorEngine te) {
        this.te = te;
    }

    public void setTranslator(translatorEngine te) {
        this.te = te;
    }

    public static synchronized MyTransL getInstance(translatorEngine te) {
        if (myTransL == null)
            myTransL = new MyTransL(te);
        return myTransL;
    }

    /**
     * Allows to get the current domain for libre translate
     *
     * @return locale String
     */
    public static String getLibreTranslateUrl() {
        return "https://" + libretranslateDomain + "/translate?";
    }

    /**
     * Allows to get the current domain for lingva
     *
     * @return locale String
     */
    public static String getLingvaUrl() {
        return "https://" + lingvaDomain + "/api/v1/";
    }

    /**
     * Allows to get the current domain for Mint
     *
     * @return locale String
     */
    public static String getMintUrl() {
        return "https://" + mintDomain + "/api/translate";
    }



    /**
     * Allows to get the current locale of the device
     *
     * @return locale String
     */
    public static String getLocale() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * Timeout in seconds
     *
     * @param timeout - int
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setObfuscation(boolean obfuscation) {
        this.obfuscation = obfuscation;
    }

    public boolean isObfuscated() {
        return this.obfuscation;
    }

    public String getDeeplAPIKey() {
        return deeplAPIKey;
    }

    public void setDeeplAPIKey(String deeplAPIKey) {
        this.deeplAPIKey = deeplAPIKey;
    }

    public String getYandexAPIKey() {
        return this.yandexAPIKey;
    }

    public void setYandexAPIKey(String key) {
        this.yandexAPIKey = key;
    }

    public String getSystranAPIKey() {
        return this.systranAPIKey;
    }

    public void setSystranAPIKey(String key) {
        this.systranAPIKey = key;
    }

    public String getLibretranslateDomain() {
        return libretranslateDomain;
    }

    public void setLibretranslateDomain(String libretranslateDomain) {
        MyTransL.libretranslateDomain = libretranslateDomain;
    }


    public String getLingvaDomain() {
        return lingvaDomain;
    }

    public void setLingvaDomain(String lingvaDomain) {
        MyTransL.lingvaDomain = lingvaDomain;
    }


    public String getMintDomain() {
        return mintDomain;
    }

    public void setMintDomain(String mintDomain) {
        MyTransL.mintDomain = mintDomain;
    }

    public String getLibreTranslateAPIKey() {
        return libreTranslateAPIKey;
    }

    public void setLibreTranslateAPIKey(String libreTranslateAPIKey) {
        this.libreTranslateAPIKey = libreTranslateAPIKey;
    }

    public String getLingvaAPIKey() {
        return lingvaAPIKey;
    }

    public void setLingvaAPIKey(String lingvaAPIKey) {
        this.lingvaAPIKey = lingvaAPIKey;
    }

    /**
     * Asynchronous call for the translation
     *
     * @param content    String - Content to translate
     * @param toLanguage - String the targeted language
     * @param listener   - Callback for the asynchronous call
     */
    public void translate(final String content, final String toLanguage, Params params, final Results listener) {
        new TransAsync(te, content, toLanguage, params, timeout, obfuscation, listener);
    }

    /**
     * Asynchronous call for the translation
     *
     * @param content    String - Content to translate
     * @param toLanguage - String the targeted language
     * @param listener   - Callback for the asynchronous call
     */
    public void translate(final String content, Params.fType format, final String toLanguage, final Results listener) {
        new TransAsync(te, content, format, toLanguage, timeout, obfuscation, listener);
    }

    public enum translatorEngine {
        YANDEX,
        DEEPL,
        SYSTRAN,
        LIBRETRANSLATE,
        LINGVA,
        MINT
    }

}
