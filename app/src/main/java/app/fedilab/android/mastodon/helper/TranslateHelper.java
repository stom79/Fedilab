package app.fedilab.android.mastodon.helper;
/* Copyright 2022 Thomas Schneider
 *
 * This file is a part of Fedilab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Fedilab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Fedilab; if not,
 * see <http://www.gnu.org/licenses>. */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.Html;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.github.stom79.mytransl.MyTransL;
import com.github.stom79.mytransl.client.HttpsConnectionException;
import com.github.stom79.mytransl.client.Results;
import com.github.stom79.mytransl.translate.Params;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Set;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.Languages;
import es.dmoral.toasty.Toasty;

public class TranslateHelper {

    public static void translate(Context context, String toTranslate, Translate callback) {
        String statusToTranslate;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            statusToTranslate = Html.fromHtml(toTranslate, Html.FROM_HTML_MODE_LEGACY).toString();
        else
            statusToTranslate = Html.fromHtml(toTranslate).toString();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String translator = sharedpreferences.getString(context.getString(R.string.SET_TRANSLATOR), "FEDILAB");
        MyTransL.translatorEngine et;
        if (translator.compareToIgnoreCase("FEDILAB") == 0) {
            et = MyTransL.translatorEngine.LIBRETRANSLATE;
        } else if (translator.compareToIgnoreCase("LINGVA") == 0) {
            et = MyTransL.translatorEngine.LINGVA;
        } else {
            et = MyTransL.translatorEngine.DEEPL;
        }
        final MyTransL myTransL = MyTransL.getInstance(et);
        myTransL.setObfuscation(true);
        Params params = new Params();
        params.setSplit_sentences(false);
        params.setFormat(Params.fType.TEXT);
        params.setSource_lang("auto");
        if (translator.compareToIgnoreCase("FEDILAB") == 0) {
            myTransL.setLibretranslateDomain("translate.fedilab.app");
        } else if (translator.compareToIgnoreCase("LINGVA") == 0) {
            String host = sharedpreferences.getString(context.getString(R.string.SET_TRANSLATOR_DOMAIN), context.getString(R.string.SET_TRANSLATOR_HOST_LINGVA));
            if (host == null || host.trim().isEmpty()) {
                host = context.getString(R.string.SET_TRANSLATOR_HOST_LINGVA);
            }
            myTransL.setLingvaDomain(host);
        } else {
            String translatorVersion = sharedpreferences.getString(context.getString(R.string.SET_TRANSLATOR_VERSION), "PRO");
            params.setPro(translatorVersion.equals("PRO"));
            String apikey = sharedpreferences.getString(context.getString(R.string.SET_TRANSLATOR_API_KEY), null);
            if (apikey != null) {
                myTransL.setDeeplAPIKey(apikey.trim());
            }
        }

        Set<String> translates = sharedpreferences.getStringSet(context.getString(R.string.SET_LIVE_TRANSLATE_MULTIPLE), null);
        String translate;
        if (translates == null || translates.size() <= 1) {
            translate = MyTransL.getLocale();
            if(translates != null && translates.size() == 1 ) {
                for (String val : translates) {
                    translate = val;
                }
            }
            myTransL.translate(statusToTranslate, translate, params, new Results() {
                @Override
                public void onSuccess(com.github.stom79.mytransl.translate.Translate translate) {
                    if (translate.getTranslatedContent() != null) {
                        callback.onTranslate(translate.getTranslatedContent());
                    } else {
                        callback.onTranslate("");
                        Toasty.error(context, context.getString(R.string.toast_error_translate), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFail(HttpsConnectionException httpsConnectionException) {
                    callback.onTranslate("");
                    Toasty.error(context, context.getString(R.string.toast_error_translate), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            String[] codesArr = new String[translates.size()];
            String[] languagesArr = new String[translates.size()];
            int j = 0;
            List<Languages.Language> languages = Languages.get(context);
            if (languages == null) {
                return;
            }
            for (String val : translates) {
                codesArr[j] = val;
                for (Languages.Language language : languages) {
                    if (language.code.trim().equalsIgnoreCase(val.trim())) {
                        languagesArr[j] = language.language;
                        break;
                    }
                }
                j++;
            }
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(context.getString(R.string.translate_in));
            builder.setItems(languagesArr, (dialogInterface, i) -> {
                myTransL.translate(statusToTranslate, codesArr[i], params, new Results() {
                    @Override
                    public void onSuccess(com.github.stom79.mytransl.translate.Translate translate) {
                        if (translate.getTranslatedContent() != null) {
                            callback.onTranslate(translate.getTranslatedContent());
                        } else {
                            callback.onTranslate("");
                            Toasty.error(context, context.getString(R.string.toast_error_translate), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFail(HttpsConnectionException httpsConnectionException) {
                        callback.onTranslate("");
                        Toasty.error(context, context.getString(R.string.toast_error_translate), Toast.LENGTH_LONG).show();
                    }
                });
                dialogInterface.dismiss();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.create().show();
        }


    }

    public interface Translate {
        void onTranslate(String translated);
    }
}
