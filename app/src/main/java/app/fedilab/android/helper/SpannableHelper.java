package app.fedilab.android.helper;
/* Copyright 2021 Thomas Schneider
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


import static app.fedilab.android.BaseMainActivity.currentAccount;
import static app.fedilab.android.ui.drawer.StatusAdapter.sendAction;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.QuoteSpan;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.ContextActivity;
import app.fedilab.android.activities.HashTagActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Announcement;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Emoji;
import app.fedilab.android.client.entities.api.Filter;
import app.fedilab.android.client.entities.api.Mention;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.databinding.PopupLinksBinding;
import app.fedilab.android.viewmodel.mastodon.FiltersVM;
import es.dmoral.toasty.Toasty;

public class SpannableHelper {

    public static final String CLICKABLE_SPAN = "CLICKABLE_SPAN";

    public static Spannable convert(Context context, String text,
                                    Status status, Account account, Announcement announcement,
                                    boolean convertHtml, boolean forceMentions, WeakReference<View> viewWeakReference) {
        return convert(context, text, status, account, announcement, convertHtml, forceMentions, viewWeakReference, null);
    }


    private static int linkColor;

    public static Spannable convert(Context context, String text,
                                    Status status, Account account, Announcement announcement,
                                    boolean convertHtml,
                                    boolean forceMentions,
                                    WeakReference<View> viewWeakReference, Status.Callback callback) {

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean customLight = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_LIGHT_COLORS), false);
        boolean customDark = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_DARK_COLORS), false);
        int link_color;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO && customLight) {
            link_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_LINK), -1);
            if (link_color != -1) {
                linkColor = link_color;
            }
        } else if (currentNightMode == Configuration.UI_MODE_NIGHT_YES && customDark) {
            link_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_LINK), -1);
            if (link_color != -1) {
                linkColor = link_color;
            }
        } else {
            linkColor = ThemeHelper.getAttColor(context, R.attr.linkColor);
        }

        SpannableString initialContent;
        if (text == null) {
            return null;
        }
        Document htmlContent = Jsoup.parse(text);
        Elements mentionElements = htmlContent.select("a.mention");
        //We keep a reference to mentions
        HashMap<String, String> mentionsMap = new HashMap<>();
        if (mentionElements.size() > 0) {
            for (int i = 0; i < mentionElements.size(); i++) {
                Element mentionElement = mentionElements.get(i);
                String href = mentionElement.attr("href");
                String mention = mentionElement.text();
                mentionsMap.put(mention, href);
            }
        }

        text = text.replaceAll("((<\\s?p\\s?>|<\\s?br\\s?\\/?>)&gt;(((?!([<])).)*))", "$2<blockquote>$3</blockquote>");
        Pattern imgPattern = Pattern.compile("<img [^>]*src=\"([^\"]+)\"[^>]*>");
        Matcher matcherImg = imgPattern.matcher(text);
        HashMap<String, String> imagesToReplace = new LinkedHashMap<>();
        int inc = 0;
        while (matcherImg.find()) {
            String replacement = "[FEDI_IMG_" + inc + "]";
            imagesToReplace.put(replacement, matcherImg.group(1));
            inc++;
            text = text.replaceAll(Pattern.quote(matcherImg.group()), replacement);
        }

        SpannableStringBuilder content;
        View view = viewWeakReference.get();
        List<Mention> mentionList = null;
        List<Emoji> emojiList = null;
        if (status != null) {
            mentionList = status.mentions;
            emojiList = status.emojis;
        } else if (account != null) {
            emojiList = account.emojis;
        } else if (announcement != null) {
            emojiList = announcement.emojis;
        }
        //UrlDetails will contain links having a text different from the url
        HashMap<String, String> urlDetails = new HashMap<>();
        if (convertHtml) {
            Matcher matcherALink = Helper.aLink.matcher(text);

            //We stock details
            while (matcherALink.find()) {
                String urlText = matcherALink.group(3);
                String url = matcherALink.group(2);
                if (urlText != null && urlText.startsWith(">")) {
                    urlText = urlText.substring(1);
                }
                if (url != null && urlText != null && !url.equalsIgnoreCase(urlText) && !urlText.contains("<span")) {
                    urlDetails.put(url, urlText);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                initialContent = new SpannableString(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
            else
                initialContent = new SpannableString(Html.fromHtml(text));

            content = new SpannableStringBuilder(initialContent);
            URLSpan[] urls = content.getSpans(0, (content.length() - 1), URLSpan.class);
            for (URLSpan span : urls) {
                content.removeSpan(span);
            }
            //Make tags, mentions, groups
            interaction(context, content, status, mentionList, forceMentions, mentionsMap);
            //Make all links
            linkify(context, content, urlDetails);
            linkifyURL(context, content, urlDetails);
            emails(context, content);
            gemini(context, content);
            replaceQuoteSpans(context, content);
        } else {
            content = new SpannableStringBuilder(text);
        }
        boolean animate = !sharedpreferences.getBoolean(context.getString(R.string.SET_DISABLE_ANIMATED_EMOJI), false);
        CustomEmoji customEmoji = new CustomEmoji(new WeakReference<>(view));
        content = customEmoji.makeEmoji(content, emojiList, animate, callback);

        if (imagesToReplace.size() > 0) {
            for (Map.Entry<String, String> entry : imagesToReplace.entrySet()) {
                String key = entry.getKey();
                String url = entry.getValue();
                Matcher matcher = Pattern.compile(key, Pattern.LITERAL)
                        .matcher(content);
                while (matcher.find()) {
                    content.setSpan(customEmoji, matcher.start(), matcher.end(), 0);
                    Glide.with(view)
                            .asDrawable()
                            .load(url)
                            .into(customEmoji.getTarget(animate, null));
                }
            }

        }
        return trimSpannable(new SpannableStringBuilder(content));
    }

    private static void linkify(Context context, SpannableStringBuilder content, HashMap<String, String> urlDetails) {
        //--- URLs ----
        Matcher matcherLink = Patterns.WEB_URL.matcher(content);

        int offSetTruncate = 0;


        while (matcherLink.find()) {
            int matchStart = matcherLink.start() - offSetTruncate;
            int matchEnd = matchStart + matcherLink.group().length();
            if (matchEnd > content.toString().length()) {
                matchEnd = content.toString().length();
            }

            if (content.toString().length() < matchEnd || matchStart < 0 || matchStart > matchEnd) {
                continue;
            }


            final String url = content.toString().substring(matchStart, matchEnd);
            if (urlDetails.containsKey(url)) {
                continue;
            }
            String newURL = Helper.transformURL(context, url);
            //If URL has been transformed
            if (newURL.compareTo(url) != 0) {
                content.replace(matchStart, matchEnd, newURL);
                offSetTruncate -= (newURL.length() - url.length());
                matchEnd = matchStart + newURL.length();
            }

            //Truncate URL if needed
            //TODO: add an option to disable truncated URLs
            String urlText = newURL;
            if (newURL.length() > 30 && !urlDetails.containsKey(urlText)) {
                urlText = urlText.substring(0, 30);
                urlText += "…";
                content.replace(matchStart, matchEnd, urlText);
                matchEnd = matchStart + 31;
                offSetTruncate += (newURL.length() - urlText.length());
            }


            if (matchEnd <= content.length() && matchEnd >= matchStart) {
                content.setSpan(new LongClickableSpan() {
                    @Override
                    public void onLongClick(View view) {
                        Context mContext = view.getContext();
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
                        PopupLinksBinding popupLinksBinding = PopupLinksBinding.inflate(LayoutInflater.from(context));
                        dialogBuilder.setView(popupLinksBinding.getRoot());
                        AlertDialog alertDialog = dialogBuilder.create();
                        alertDialog.show();
                        String finalURl = newURL;
                        String uniqueUrl = newURL.endsWith("…") ? newURL : newURL + "…";
                        if (urlDetails.containsValue(uniqueUrl)) {
                            finalURl = Helper.getKeyByValue(urlDetails, uniqueUrl);
                        }
                        if (finalURl == null) {
                            return;
                        }
                        if (finalURl.startsWith("http://")) {
                            finalURl = finalURl.replace("http://", "https://");
                        }
                        String finalURl1 = finalURl;
                        popupLinksBinding.displayFullLink.setOnClickListener(v -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            builder.setMessage(finalURl1);
                            builder.setTitle(context.getString(R.string.display_full_link));
                            builder.setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                                    .show();
                            alertDialog.dismiss();
                        });
                        popupLinksBinding.shareLink.setOnClickListener(v -> {
                            Intent sendIntent = new Intent(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.shared_via));
                            sendIntent.putExtra(Intent.EXTRA_TEXT, finalURl1);
                            sendIntent.setType("text/plain");
                            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            Intent intentChooser = Intent.createChooser(sendIntent, context.getString(R.string.share_with));
                            intentChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intentChooser);
                            alertDialog.dismiss();
                        });

                        popupLinksBinding.openOtherApp.setOnClickListener(v -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(finalURl1));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            try {
                                context.startActivity(intent);
                            } catch (Exception e) {
                                Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                            }
                            alertDialog.dismiss();
                        });

                        popupLinksBinding.copyLink.setOnClickListener(v -> {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText(Helper.CLIP_BOARD, finalURl1);
                            if (clipboard != null) {
                                clipboard.setPrimaryClip(clip);
                                Toasty.info(context, context.getString(R.string.clipboard_url), Toast.LENGTH_LONG).show();
                            }
                            alertDialog.dismiss();
                        });

                        popupLinksBinding.checkRedirect.setOnClickListener(v -> {
                            try {

                                URL finalUrlCheck = new URL(finalURl1);
                                new Thread(() -> {
                                    try {
                                        String redirect = null;
                                        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) finalUrlCheck.openConnection();
                                        httpsURLConnection.setConnectTimeout(10 * 1000);
                                        httpsURLConnection.setRequestProperty("http.keepAlive", "false");
                                       // httpsURLConnection.setRequestProperty("User-Agent", USER_AGENT);
                                        httpsURLConnection.setRequestMethod("HEAD");
                                        httpsURLConnection.setInstanceFollowRedirects(false);
                                        if (httpsURLConnection.getResponseCode() == 301 || httpsURLConnection.getResponseCode() == 302) {
                                            Map<String, List<String>> map = httpsURLConnection.getHeaderFields();
                                            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                                                if (entry.toString().toLowerCase().startsWith("location")) {
                                                    Matcher matcher = Patterns.WEB_URL.matcher(entry.toString());
                                                    if (matcher.find()) {
                                                        redirect = matcher.group(1);
                                                    }
                                                }
                                            }
                                        }
                                        httpsURLConnection.getInputStream().close();
                                        if (redirect != null && redirect.compareTo(finalURl1) != 0) {
                                            URL redirectURL = new URL(redirect);
                                            String host = redirectURL.getHost();
                                            String protocol = redirectURL.getProtocol();
                                            if (protocol == null || host == null) {
                                                redirect = null;
                                            }
                                        }
                                        Handler mainHandler = new Handler(context.getMainLooper());
                                        String finalRedirect = redirect;
                                        Runnable myRunnable = () -> {
                                            AlertDialog.Builder builder1 = new AlertDialog.Builder(view.getContext());
                                            if (finalRedirect != null) {
                                                builder1.setMessage(context.getString(R.string.redirect_detected, finalURl1, finalRedirect));
                                                builder1.setNegativeButton(R.string.copy_link, (dialog, which) -> {
                                                    ClipboardManager clipboard1 = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                                    ClipData clip1 = ClipData.newPlainText(Helper.CLIP_BOARD, finalRedirect);
                                                    if (clipboard1 != null) {
                                                        clipboard1.setPrimaryClip(clip1);
                                                        Toasty.info(context, context.getString(R.string.clipboard_url), Toast.LENGTH_LONG).show();
                                                    }
                                                    dialog.dismiss();
                                                });
                                                builder1.setNeutralButton(R.string.share_link, (dialog, which) -> {
                                                    Intent sendIntent1 = new Intent(Intent.ACTION_SEND);
                                                    sendIntent1.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.shared_via));
                                                    sendIntent1.putExtra(Intent.EXTRA_TEXT, finalURl1);
                                                    sendIntent1.setType("text/plain");
                                                    context.startActivity(Intent.createChooser(sendIntent1, context.getString(R.string.share_with)));
                                                    dialog.dismiss();
                                                });
                                            } else {
                                                builder1.setMessage(R.string.no_redirect);
                                            }
                                            builder1.setTitle(context.getString(R.string.check_redirect));
                                            builder1.setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                                                    .show();

                                        };
                                        mainHandler.post(myRunnable);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }).start();
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }

                            alertDialog.dismiss();
                        });

                    }

                    @Override
                    public void onClick(@NonNull View textView) {
                        String finalURl = newURL;
                        String finalURl2 = url;
                        String uniqueNewURL = newURL.endsWith("…") ? newURL : newURL + "…";
                        if (urlDetails.containsValue(uniqueNewURL)) {
                            finalURl = Helper.getKeyByValue(urlDetails, uniqueNewURL);
                        }
                        String uniqueUrl = url.endsWith("…") ? url : url + "…";
                        if (urlDetails.containsValue(uniqueUrl)) {
                            finalURl2 = Helper.getKeyByValue(urlDetails, uniqueUrl);
                        }
                        textView.setTag(CLICKABLE_SPAN);
                        Pattern link = Pattern.compile("https?://([\\da-z.-]+\\.[a-z.]{2,10})/(@[\\w._-]*[0-9]*)(/[0-9]+)?$");
                        Matcher matcherLink = null;
                        if (finalURl2 != null) {
                            matcherLink = link.matcher(finalURl2);
                        }
                        if (finalURl2 != null && matcherLink.find() && !finalURl2.contains("medium.com")) {
                            if (matcherLink.group(3) != null && Objects.requireNonNull(matcherLink.group(3)).length() > 0) { //It's a toot
                                CrossActionHelper.fetchRemoteStatus(context, currentAccount, finalURl2, new CrossActionHelper.Callback() {
                                    @Override
                                    public void federatedStatus(Status status) {
                                        Intent intent = new Intent(context, ContextActivity.class);
                                        intent.putExtra(Helper.ARG_STATUS, status);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(intent);
                                    }

                                    @Override
                                    public void federatedAccount(Account account) {
                                    }
                                });
                            } else {//It's an account
                                CrossActionHelper.fetchRemoteAccount(context, currentAccount, matcherLink.group(2) + "@" + matcherLink.group(1), new CrossActionHelper.Callback() {
                                    @Override
                                    public void federatedStatus(Status status) {
                                    }

                                    @Override
                                    public void federatedAccount(Account account) {
                                        Intent intent = new Intent(context, ProfileActivity.class);
                                        Bundle b = new Bundle();
                                        b.putSerializable(Helper.ARG_ACCOUNT, account);
                                        intent.putExtras(b);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(intent);
                                    }
                                });
                            }
                        } else {
                            Helper.openBrowser(context, finalURl);
                        }

                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(linkColor);
                    }

                }, matchStart, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void linkifyURL(Context context, SpannableStringBuilder content, HashMap<String, String> urlDetails) {

        for (Map.Entry<String, String> entry : urlDetails.entrySet()) {
            String value = entry.getValue();
            if (value.startsWith("@") || value.startsWith("#")) {
                continue;
            }
            SpannableString contentUrl;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                contentUrl = new SpannableString(Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY));
            else
                contentUrl = new SpannableString(Html.fromHtml(value));

            Pattern word = Pattern.compile(Pattern.quote(contentUrl.toString()));
            Matcher matcherLink = word.matcher(content);
            while (matcherLink.find()) {
                String url = entry.getKey();
                int matchStart = matcherLink.start();
                int matchEnd = matchStart + matcherLink.group().length();
                if (matchEnd > content.toString().length()) {
                    matchEnd = content.toString().length();
                }

                if (content.toString().length() < matchEnd || matchStart < 0 || matchStart > matchEnd) {
                    continue;
                }
                if (matchEnd <= content.length()) {
                    content.setSpan(new LongClickableSpan() {
                        @Override
                        public void onLongClick(View view) {
                            Context mContext = view.getContext();
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
                            PopupLinksBinding popupLinksBinding = PopupLinksBinding.inflate(LayoutInflater.from(context));
                            dialogBuilder.setView(popupLinksBinding.getRoot());
                            AlertDialog alertDialog = dialogBuilder.create();
                            alertDialog.show();
                            String finalURl = url;
                            if (urlDetails.containsValue(url)) {
                                finalURl = Helper.getKeyByValue(urlDetails, url);
                            }
                            String finalURl1 = finalURl;
                            popupLinksBinding.displayFullLink.setOnClickListener(v -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                builder.setMessage(finalURl1);
                                builder.setTitle(context.getString(R.string.display_full_link));
                                builder.setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                                        .show();
                                alertDialog.dismiss();
                            });
                            popupLinksBinding.shareLink.setOnClickListener(v -> {
                                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.shared_via));
                                sendIntent.putExtra(Intent.EXTRA_TEXT, finalURl1);
                                sendIntent.setType("text/plain");
                                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Intent intentChooser = Intent.createChooser(sendIntent, context.getString(R.string.share_with));
                                intentChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intentChooser);
                                alertDialog.dismiss();
                            });

                            popupLinksBinding.openOtherApp.setOnClickListener(v -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(finalURl1));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                                }
                                alertDialog.dismiss();
                            });

                            popupLinksBinding.copyLink.setOnClickListener(v -> {
                                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText(Helper.CLIP_BOARD, finalURl1);
                                if (clipboard != null) {
                                    clipboard.setPrimaryClip(clip);
                                    Toasty.info(context, context.getString(R.string.clipboard_url), Toast.LENGTH_LONG).show();
                                }
                                alertDialog.dismiss();
                            });

                            popupLinksBinding.checkRedirect.setOnClickListener(v -> {
                                try {

                                    URL finalUrlCheck = new URL(finalURl1);
                                    new Thread(() -> {
                                        try {
                                            String redirect = null;
                                            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) finalUrlCheck.openConnection();
                                            httpsURLConnection.setConnectTimeout(10 * 1000);
                                            httpsURLConnection.setRequestProperty("http.keepAlive", "false");
                                            //httpsURLConnection.setRequestProperty("User-Agent", USER_AGENT);
                                            httpsURLConnection.setRequestMethod("HEAD");
                                            httpsURLConnection.setInstanceFollowRedirects(false);
                                            if (httpsURLConnection.getResponseCode() == 301 || httpsURLConnection.getResponseCode() == 302) {
                                                Map<String, List<String>> map = httpsURLConnection.getHeaderFields();
                                                for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                                                    if (entry.toString().toLowerCase().startsWith("location")) {
                                                        Matcher matcher = Patterns.WEB_URL.matcher(entry.toString());
                                                        if (matcher.find()) {
                                                            redirect = matcher.group(1);
                                                        }
                                                    }
                                                }
                                            }
                                            httpsURLConnection.getInputStream().close();
                                            if (redirect != null && finalURl1 != null && redirect.compareTo(finalURl1) != 0) {
                                                URL redirectURL = new URL(redirect);
                                                String host = redirectURL.getHost();
                                                String protocol = redirectURL.getProtocol();
                                                if (protocol == null || host == null) {
                                                    redirect = null;
                                                }
                                            }
                                            Handler mainHandler = new Handler(context.getMainLooper());
                                            String finalRedirect = redirect;
                                            Runnable myRunnable = () -> {
                                                AlertDialog.Builder builder1 = new AlertDialog.Builder(view.getContext());
                                                if (finalRedirect != null) {
                                                    builder1.setMessage(context.getString(R.string.redirect_detected, finalURl1, finalRedirect));
                                                    builder1.setNegativeButton(R.string.copy_link, (dialog, which) -> {
                                                        ClipboardManager clipboard1 = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                                        ClipData clip1 = ClipData.newPlainText(Helper.CLIP_BOARD, finalRedirect);
                                                        if (clipboard1 != null) {
                                                            clipboard1.setPrimaryClip(clip1);
                                                            Toasty.info(context, context.getString(R.string.clipboard_url), Toast.LENGTH_LONG).show();
                                                        }
                                                        dialog.dismiss();
                                                    });
                                                    builder1.setNeutralButton(R.string.share_link, (dialog, which) -> {
                                                        Intent sendIntent1 = new Intent(Intent.ACTION_SEND);
                                                        sendIntent1.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.shared_via));
                                                        sendIntent1.putExtra(Intent.EXTRA_TEXT, finalURl1);
                                                        sendIntent1.setType("text/plain");
                                                        context.startActivity(Intent.createChooser(sendIntent1, context.getString(R.string.share_with)));
                                                        dialog.dismiss();
                                                    });
                                                } else {
                                                    builder1.setMessage(R.string.no_redirect);
                                                }
                                                builder1.setTitle(context.getString(R.string.check_redirect));
                                                builder1.setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                                                        .show();

                                            };
                                            mainHandler.post(myRunnable);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    }).start();
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }

                                alertDialog.dismiss();
                            });

                        }

                        @Override
                        public void onClick(@NonNull View textView) {
                            String finalURl = url;
                            if (urlDetails.containsValue(url)) {
                                finalURl = Helper.getKeyByValue(urlDetails, url);
                            }

                            textView.setTag(CLICKABLE_SPAN);
                            Pattern link = Pattern.compile("https?://([\\da-z.-]+\\.[a-z.]{2,10})/(@[\\w._-]*[0-9]*)(/[0-9]+)?$");
                            Matcher matcherLink = null;
                            if (finalURl != null) {
                                matcherLink = link.matcher(finalURl);
                            }
                            if (finalURl != null && matcherLink.find() && !finalURl.contains("medium.com")) {
                                if (matcherLink.group(3) != null && Objects.requireNonNull(matcherLink.group(3)).length() > 0) { //It's a toot
                                    CrossActionHelper.fetchRemoteStatus(context, currentAccount, finalURl, new CrossActionHelper.Callback() {
                                        @Override
                                        public void federatedStatus(Status status) {
                                            Intent intent = new Intent(context, ContextActivity.class);
                                            intent.putExtra(Helper.ARG_STATUS, status);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            context.startActivity(intent);
                                        }

                                        @Override
                                        public void federatedAccount(Account account) {
                                        }
                                    });
                                } else {//It's an account
                                    CrossActionHelper.fetchRemoteAccount(context, currentAccount, matcherLink.group(2) + "@" + matcherLink.group(1), new CrossActionHelper.Callback() {
                                        @Override
                                        public void federatedStatus(Status status) {
                                        }

                                        @Override
                                        public void federatedAccount(Account account) {
                                            Intent intent = new Intent(context, ProfileActivity.class);
                                            Bundle b = new Bundle();
                                            b.putSerializable(Helper.ARG_ACCOUNT, account);
                                            intent.putExtras(b);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            context.startActivity(intent);
                                        }
                                    });
                                }
                            } else {
                                Helper.openBrowser(context, finalURl);
                            }

                        }

                        @Override
                        public void updateDrawState(@NonNull TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setUnderlineText(false);
                            ds.setColor(linkColor);
                        }
                    }, matchStart, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private static void gemini(Context context, Spannable content) {
        // --- For all patterns defined in Helper class ---
        Pattern pattern = Helper.geminiPattern;
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int matchStart = matcher.start();
            int matchEnd = matcher.end();
            String geminiLink = content.toString().substring(matchStart, matchEnd);
            if (matchStart >= 0 && matchEnd <= content.toString().length() && matchEnd >= matchStart) {
                ClickableSpan[] clickableSpans = content.getSpans(matchStart, matchEnd, ClickableSpan.class);
                if (clickableSpans != null) {
                    for (ClickableSpan clickableSpan : clickableSpans) {
                        content.removeSpan(clickableSpan);
                    }
                }
                content.removeSpan(clickableSpans);
                content.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View textView) {
                        Helper.openBrowser(context, geminiLink);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(linkColor);
                    }
                }, matchStart, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void emails(Context context, Spannable content) {
        // --- For all patterns defined in Helper class ---
        Pattern pattern = Helper.emailPattern;
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int matchStart = matcher.start();
            int matchEnd = matcher.end();
            String email = content.toString().substring(matchStart, matchEnd);
            if (matchStart >= 0 && matchEnd <= content.toString().length() && matchEnd >= matchStart) {
                ClickableSpan[] clickableSpans = content.getSpans(matchStart, matchEnd, ClickableSpan.class);
                if (clickableSpans != null) {
                    for (ClickableSpan clickableSpan : clickableSpans) {
                        content.removeSpan(clickableSpan);
                    }
                }
                content.removeSpan(clickableSpans);
                content.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View textView) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("plain/text");
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                        context.startActivity(Intent.createChooser(intent, null));
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(linkColor);
                    }
                }, matchStart, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void interaction(Context context, Spannable content, Status status, List<Mention> mentions, boolean forceMentions, HashMap<String, String> mentionsMap) {
        // --- For all patterns defined in Helper class ---
        for (Map.Entry<Helper.PatternType, Pattern> entry : Helper.patternHashMap.entrySet()) {
            Helper.PatternType patternType = entry.getKey();
            Pattern pattern = entry.getValue();
            Matcher matcher = pattern.matcher(content);
            if (pattern == Helper.mentionPattern && mentions == null && !forceMentions) {
                continue;
            } else if (pattern == Helper.mentionLongPattern && mentions == null && !forceMentions) {
                continue;
            }

            while (matcher.find()) {
                int matchStart = matcher.start();
                int matchEnd = matcher.end();
                String word = content.toString().substring(matchStart, matchEnd);
                if (matchStart >= 0 && matchEnd <= content.toString().length() && matchEnd >= matchStart) {
                    URLSpan[] span = content.getSpans(matchStart, matchEnd, URLSpan.class);
                    content.removeSpan(span);
                    content.setSpan(new LongClickableSpan() {
                        @Override
                        public void onLongClick(View textView) {
                            textView.setTag(CLICKABLE_SPAN);
                            if (patternType == Helper.PatternType.TAG && BaseMainActivity.filterFetched && MainActivity.mainFilters != null) {
                                String tag = word.trim();
                                if (!tag.startsWith("#")) {
                                    tag = "#" + tag;
                                }
                                Filter fedilabFilter = null;
                                for (Filter filter : MainActivity.mainFilters) {
                                    if (filter.title.equals(Helper.FEDILAB_MUTED_HASHTAGS)) {
                                        fedilabFilter = filter;
                                        break;
                                    }
                                }
                                //Filter for Fedilab doesn't exist we have to create it
                                if (fedilabFilter == null) {
                                    Filter.FilterParams filterParams = new Filter.FilterParams();
                                    filterParams.title = Helper.FEDILAB_MUTED_HASHTAGS;
                                    filterParams.filter_action = "hide";
                                    filterParams.context = new ArrayList<>();
                                    filterParams.context.add("home");
                                    filterParams.context.add("public");
                                    filterParams.context.add("thread");
                                    filterParams.context.add("account");
                                    String finalTag = tag;
                                    FiltersVM filtersVM = new ViewModelProvider((ViewModelStoreOwner) context).get(FiltersVM.class);
                                    filtersVM.addFilter(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, filterParams)
                                            .observe((LifecycleOwner) context, filter -> {
                                                if (filter != null) {
                                                    MainActivity.mainFilters.add(filter);
                                                    addTagToFilter(context, finalTag, status, filter);
                                                }
                                            });
                                } else {
                                    addTagToFilter(context, tag, status, fedilabFilter);
                                }
                            }
                        }

                        @Override
                        public void onClick(@NonNull View textView) {
                            textView.setTag(CLICKABLE_SPAN);
                            switch (patternType) {
                                case TAG:
                                    Intent intent = new Intent(context, HashTagActivity.class);
                                    Bundle b = new Bundle();
                                    b.putString(Helper.ARG_SEARCH_KEYWORD, word.trim());
                                    intent.putExtras(b);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                    break;
                                case GROUP:
                                    break;
                                case MENTION:
                                    intent = new Intent(context, ProfileActivity.class);
                                    b = new Bundle();
                                    Mention targetedMention = null;
                                    String acct = null;
                                    HashMap<String, Integer> countUsername = new HashMap<>();
                                    //Mentions is retrieved with associated Mentions array
                                    if (mentions != null) {
                                        for (Mention mention : mentions) {
                                            Integer count = countUsername.get(mention.username);
                                            if (count == null) {
                                                count = 0;
                                            }
                                            if (countUsername.containsKey(mention.username)) {
                                                countUsername.put(mention.username, count + 1);
                                            } else {
                                                countUsername.put(mention.username, 1);
                                            }
                                        }
                                        for (Mention mention : mentions) {
                                            Integer count = countUsername.get(mention.username);
                                            if (count == null) {
                                                count = 0;
                                            }
                                            if (word.trim().compareToIgnoreCase("@" + mention.username) == 0 && count == 1) {
                                                targetedMention = mention;
                                                break;
                                            }
                                        }
                                    } else if (mentionsMap.containsKey(word.trim())) {//Mentions will be find through its URL
                                        URL url;
                                        try {
                                            url = new URL(mentionsMap.get(word.trim()));
                                            acct = word.trim() + "@" + url.getHost();
                                        } catch (MalformedURLException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (targetedMention != null) {
                                        b.putString(Helper.ARG_USER_ID, targetedMention.id);
                                    } else {
                                        b.putString(Helper.ARG_MENTION, acct != null ? acct : word.trim());
                                    }

                                    intent.putExtras(b);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                    break;
                                case MENTION_LONG:
                                    intent = new Intent(context, ProfileActivity.class);
                                    b = new Bundle();
                                    targetedMention = null;
                                    if (mentions != null) {
                                        for (Mention mention : mentions) {
                                            if (word.trim().substring(1).compareToIgnoreCase("@" + mention.acct) == 0) {
                                                targetedMention = mention;
                                                break;
                                            }
                                        }
                                    }
                                    if (targetedMention != null) {
                                        b.putString(Helper.ARG_USER_ID, targetedMention.id);
                                    } else {
                                        b.putString(Helper.ARG_MENTION, word.trim());
                                    }
                                    intent.putExtras(b);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                    break;
                            }
                        }

                        @Override
                        public void updateDrawState(@NonNull TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setUnderlineText(false);
                            ds.setColor(linkColor);
                        }

                    }, matchStart, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    public static void addTagToFilter(Context context, String tag, Status status, Filter filter) {
        for (Filter.KeywordsAttributes keywords : filter.keywords) {
            if (keywords.keyword.equalsIgnoreCase(tag)) {
                return;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, Helper.dialogStyle());
        builder.setMessage(context.getString(R.string.mute_tag, tag));
        builder
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Filter.FilterParams filterParams = new Filter.FilterParams();
                    filterParams.id = filter.id;
                    filterParams.keywords = new ArrayList<>();
                    Filter.KeywordsParams keywordsParams = new Filter.KeywordsParams();
                    keywordsParams.whole_word = true;
                    keywordsParams.keyword = tag;
                    filterParams.keywords.add(keywordsParams);
                    filterParams.context = filter.context;
                    FiltersVM filtersVM = new ViewModelProvider((ViewModelStoreOwner) context).get(FiltersVM.class);
                    filtersVM.editFilter(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, filterParams);
                    if (status != null) {
                        status.filteredByApp = filter;
                    }
                    sendAction(context, Helper.ARG_TIMELINE_REFRESH_ALL, null, null);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Convert HTML content to text. Also, it handles click on link
     * This needs to be run asynchronously
     *
     * @param status  {@link Status} - Status concerned by the spannable transformation
     * @param content String - text to convert, it can be content, spoiler, poll items, etc.
     * @return Spannable string
     */
    private static void convertOuich(@NonNull Status status, SpannableStringBuilder content) {

        Matcher matcher = Helper.ouichesPattern.matcher(content);
        while (matcher.find()) {
            Attachment attachment = new Attachment();
            attachment.type = "audio";
            String tag = matcher.group(1);
            attachment.id = tag;
            if (tag == null) {
                continue;
            }
            attachment.remote_url = "http://ouich.es/mp3/" + tag + ".mp3";
            attachment.url = "http://ouich.es/mp3/" + tag + ".mp3";
            if (status.media_attachments == null) {
                status.media_attachments = new ArrayList<>();
            }
            boolean alreadyAdded = false;
            for (Attachment at : status.media_attachments) {
                if (tag.compareTo(at.id) == 0) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                status.media_attachments.add(attachment);
            }
        }
    }


    private static void replaceQuoteSpans(Context context, Spannable spannable) {
        QuoteSpan[] quoteSpans = spannable.getSpans(0, spannable.length(), QuoteSpan.class);
        for (QuoteSpan quoteSpan : quoteSpans) {
            int start = spannable.getSpanStart(quoteSpan);
            int end = spannable.getSpanEnd(quoteSpan);
            int flags = spannable.getSpanFlags(quoteSpan);
            spannable.removeSpan(quoteSpan);
            int colord = ThemeHelper.getAttColor(context, R.attr.colorPrimary);
            spannable.setSpan(new CustomQuoteSpan(
                            ContextCompat.getColor(context, R.color.transparent),
                            colord,
                            10,
                            20),
                    start,
                    end,
                    flags);
        }
    }



    /**
     * Remove extra carriage returns at the bottom due to <p> tags in toots
     *
     * @param spannable SpannableStringBuilder
     * @return SpannableStringBuilder
     */
    private static SpannableStringBuilder trimSpannable(SpannableStringBuilder spannable) {

        int trimStart = 0;
        int trimEnd = 0;
        String text = spannable.toString();

        while (text.length() > 0 && text.startsWith("\n")) {
            text = text.substring(1);
            trimStart += 1;
        }

        while (text.length() > 0 && text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
            trimEnd += 1;
        }
        return spannable.delete(0, trimStart).delete(spannable.length() - trimEnd, spannable.length());
    }

    /**
     * Makes the move to account clickable
     *
     * @param context Context
     * @return SpannableString
     */
    public static SpannableString moveToText(final Context context, Account account) {
        SpannableString spannableString = null;
        if (account.moved != null) {
            spannableString = new SpannableString(context.getString(R.string.account_moved_to, account.acct, "@" + account.moved.acct));
            int startPosition = spannableString.toString().indexOf("@" + account.moved.acct);
            int endPosition = startPosition + ("@" + account.moved.acct).length();
            if (startPosition >= 0 && endPosition <= spannableString.toString().length() && endPosition >= startPosition)
                spannableString.setSpan(new ClickableSpan() {
                                            @Override
                                            public void onClick(@NonNull View textView) {
                                                Intent intent = new Intent(context, ProfileActivity.class);
                                                Bundle b = new Bundle();
                                                b.putSerializable(Helper.ARG_ACCOUNT, account.moved);
                                                intent.putExtras(b);
                                                context.startActivity(intent);
                                            }

                                            @Override
                                            public void updateDrawState(@NonNull TextPaint ds) {
                                                super.updateDrawState(ds);
                                                ds.setUnderlineText(false);
                                                ds.setColor(linkColor);
                                            }
                                        },
                        startPosition, endPosition,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }


    public static Spannable convertEmoji(Activity activity, String text, Account account, WeakReference<View> viewWeakReference) {

        SpannableString initialContent;
        if (text == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            initialContent = new SpannableString(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        else
            initialContent = new SpannableString(Html.fromHtml(text));

        SpannableStringBuilder content = new SpannableStringBuilder(initialContent);
        List<Emoji> emojiList = account.emojis;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean animate = !sharedpreferences.getBoolean(activity.getString(R.string.SET_DISABLE_ANIMATED_EMOJI), false);
        if (emojiList != null && emojiList.size() > 0) {
            for (Emoji emoji : emojiList) {
                Matcher matcher = Pattern.compile(":" + emoji.shortcode + ":", Pattern.LITERAL)
                        .matcher(content);
                while (matcher.find()) {
                    CustomEmoji customEmoji = new CustomEmoji(new WeakReference<>(viewWeakReference.get()));
                    content.setSpan(customEmoji, matcher.start(), matcher.end(), 0);
                    if (Helper.isValidContextForGlide(activity)) {
                        Glide.with(viewWeakReference.get())
                                .asDrawable()
                                .load(animate ? emoji.url : emoji.static_url)
                                .into(customEmoji.getTarget(animate, null));
                    }
                }
            }
        }

        return trimSpannable(new SpannableStringBuilder(content));
    }
}
