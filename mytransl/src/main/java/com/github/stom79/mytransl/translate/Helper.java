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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

/**
 * Created by Thomas on 28/11/2017.
 * Some static references
 * Changed 10/01/2021
 */

public class Helper {


    private static final String YANDEX_BASE_URL = "https://translate.yandex.net/api/v1.5/tr.json/translate?";
    private static final String DEEPL_BASE_URL = "https://api.deepl.com/v2/translate?";
    private static final String DEEPL_BASE_FREE_URL = "https://api-free.deepl.com/v2/translate?";
    private static final String SYSTRAN_BASE_URL = "https://api-platform.systran.net/translation/text/translate?";
    private static final String[] deeplAvailableLang = {"EN", "DE", "FR", "ES", "IT", "NL", "PL"};


    /***
     * Returns the URL for Yandex
     * @param content String - Content to translate
     * @param apikey String - The Yandex API Key
     * @param toLanguage String - The targeted locale
     * @return String - absolute URL for Yandex
     */
    public static String getYandexAbsoluteUrl(String content, String apikey, String toLanguage) {
        String key = "key=" + apikey + "&";
        toLanguage = toLanguage.replace("null", "");
        String lang = "lang=" + toLanguage + "&";
        String text;
        try {
            text = "text=" + URLEncoder.encode(content, "utf-8") + "&";
        } catch (UnsupportedEncodingException e) {
            text = "text=" + content + "&";
            e.printStackTrace();
        }
        String format = "format=html&";
        return Helper.YANDEX_BASE_URL + key + lang + format + text;
    }


    /***
     * Returns the URL for Deepl
     * @param content String - Content to translate
     * @param toLanguage String - The targeted locale
     * @param deepLParams DeepLParams - The deepl paramaters see: https://www.deepl.com/api.html#api_reference_article
     * @param isPro boolean - Whether to use pro API endpoint
     * @return String - absolute URL for Deepl (without auth_key, use Authorization header instead)
     */
    public static String getDeeplAbsoluteUrl(String content, String toLanguage, Params deepLParams, boolean isPro) {
        toLanguage = toLanguage.replace("null", "");
        String lang = "target_lang=" + toLanguage.toUpperCase();
        String text;
        try {
            text = "text=" + URLEncoder.encode(content, "utf-8") + "&";
        } catch (UnsupportedEncodingException e) {
            text = "text=" + content + "&";
            e.printStackTrace();
        }
        String params = "";
        if (deepLParams.isPreserve_formatting())
            params += "&preserve_formatting=1";
        else
            params += "&preserve_formatting=0";

        if (deepLParams.isSplit_sentences())
            params += "&split_sentences=1";
        else
            params += "&split_sentences=0";

        if (deepLParams.getSource_lang() != null && Arrays.asList(deeplAvailableLang).contains(deepLParams.getSource_lang().toUpperCase()))
            params += "&split_sentences=" + deepLParams.getSource_lang();

        if (deepLParams.getIgnore_tags() != null)
            params += "&ignore_tags=" + deepLParams.getIgnore_tags();

        if (deepLParams.getTag_handling() != null)
            params += "&tag_handling=" + deepLParams.getTag_handling();

        if (deepLParams.getNon_splitting_tags() != null)
            params += "&tag_handling=" + deepLParams.getNon_splitting_tags();

        if (isPro) {
            return Helper.DEEPL_BASE_URL + text + lang + params;
        } else {
            return Helper.DEEPL_BASE_FREE_URL + text + lang + params;
        }
    }

    /***
     * Returns the Authorization header value for DeepL API
     * @param apikey String - The Deepl API Key
     * @return String - Authorization header value
     */
    public static String getDeeplAuthorizationHeader(String apikey) {
        return "DeepL-Auth-Key " + apikey;
    }


    /***
     * Returns the URL for Systran
     * @param content String - Content to translate
     * @param toLanguage String - The targeted locale
     * @param apikey String - The Systran API Key
     * @return String - absolute URL for Systran
     */
    public static String getSystranAbsoluteUrl(String content, String apikey, String toLanguage) {
        String key = "key=" + apikey + "&";
        String from = "source=auto&";
        toLanguage = toLanguage.replace("null", "");
        String lang = "target=" + toLanguage + "&";
        String text;
        try {
            text = "input=" + URLEncoder.encode(content, "utf-8") + "&";
        } catch (UnsupportedEncodingException e) {
            text = "input=" + content + "&";
            e.printStackTrace();
        }
        String encoding = "encoding=utf-8&";
        return Helper.SYSTRAN_BASE_URL + key + from + lang + encoding + text;
    }

}
