package app.fedilab.android.mastodon.helper;
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


import static app.fedilab.android.BaseMainActivity.currentNightMode;

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
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
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
import app.fedilab.android.MySuperGrammerLocator;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.PopupLinksBinding;
import app.fedilab.android.mastodon.activities.ContextActivity;
import app.fedilab.android.mastodon.activities.HashTagActivity;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Announcement;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Emoji;
import app.fedilab.android.mastodon.client.entities.api.Filter;
import app.fedilab.android.mastodon.client.entities.api.Mention;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.MarkdownConverter;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.FiltersVM;
import es.dmoral.toasty.Toasty;
import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.SoftBreakAddsNewLinePlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.inlineparser.HtmlInlineProcessor;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;
import io.noties.markwon.syntax.Prism4jThemeDefault;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;

public class SpannableHelper {

    public static final String CLICKABLE_SPAN = "CLICKABLE_SPAN";
    private static int linkColor;
    private static boolean underlineLinks;

    public static Spannable convert(Context context, String text,
                                    Status status, Account account, Announcement announcement,
                                    WeakReference<View> viewWeakReference, Status.Callback callback, boolean convertHtml, boolean convertMarkdown) {
        return convert(context, text, status, account, announcement, false, viewWeakReference, callback, convertHtml, convertMarkdown);
    }

    public static Spannable convert(Context context, String text,
                                    Status status, Account account, Announcement announcement, boolean checkRemotely,
                                    WeakReference<View> viewWeakReference, Status.Callback callback, boolean convertHtml, boolean convertMarkdown) {
        if (text == null) {
            return null;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean customLight = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_LIGHT_COLORS), false);
        boolean customDark = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_DARK_COLORS), false);
        underlineLinks = sharedpreferences.getBoolean(context.getString(R.string.SET_UNDERLINE_CLICKABLE), false);
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
            linkColor = -1;
        }
        if (linkColor == 0) {
            linkColor = -1;
        }
        if (status != null && status.underlined) {
            linkColor = -1;
        }

        List<Mention> mentions = new ArrayList<>();
        if (status != null && status.mentions != null) {
            mentions.addAll(status.mentions);
        }
        if(!convertMarkdown) {
            text = text.replaceAll("((<\\s?p\\s?>|<\\s?br\\s?/?>)&gt;(((?!(<\\s?br\\s?/?>|<\\s?/s?p\\s?>)).)*))", "$2<blockquote>$3</blockquote>");
        }
        text = text.trim().replaceAll("\\s{3}", "&nbsp;&nbsp;&nbsp;");
        text = text.trim().replaceAll("\\s{2}", "&nbsp;&nbsp;");
        SpannableString initialContent;
        if (convertHtml) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                initialContent = new SpannableString(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
            else
                initialContent = new SpannableString(Html.fromHtml(text));
        } else {
            initialContent = new SpannableString(text);
        }
        boolean markdownSupport = sharedpreferences.getBoolean(context.getString(R.string.SET_MARKDOWN_SUPPORT), false);
        //Get all links
        SpannableStringBuilder content;
        if (markdownSupport && convertMarkdown) {
            MarkdownConverter markdownConverter = new MarkdownConverter();
            markdownConverter.markdownItems = new ArrayList<>();
            int next;
            int position = 0;
            for (int i = 0; i < initialContent.length(); i = next) {
                // find the next span transition
                next = initialContent.nextSpanTransition(i, initialContent.length(), URLSpan.class);
                MarkdownConverter.MarkdownItem markdownItem = new MarkdownConverter.MarkdownItem();
                markdownItem.code = initialContent.subSequence(i, next).toString();

                markdownItem.position = position;
                // get all spans in this range
                URLSpan[] spans = initialContent.getSpans(i, next, URLSpan.class);
                if (spans != null && spans.length > 0) {
                    markdownItem.urlSpan = spans[0];
                }

                if (!markdownItem.code.trim().isEmpty()) {
                    markdownConverter.markdownItems.add(markdownItem);
                    position++;
                }
            }

            final Markwon markwon = Markwon.builder(context)
                    .usePlugin(TablePlugin.create(context))
                    .usePlugin(SoftBreakAddsNewLinePlugin.create())
                    .usePlugin(SyntaxHighlightPlugin.create(new Prism4j(new MySuperGrammerLocator()), Prism4jThemeDefault.create()))
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    .usePlugin(new AbstractMarkwonPlugin() {
                        @Override
                        public void configure(@NonNull Registry registry) {
                            registry.require(MarkwonInlineParserPlugin.class, plugin -> plugin.factoryBuilder()
                                    .excludeInlineProcessor(HtmlInlineProcessor.class));
                        }
                    })
                    .build();

            final Spanned markdown = markwon.toMarkdown(initialContent.toString());
            content = new SpannableStringBuilder(markdown);
            position = 0;

            for (MarkdownConverter.MarkdownItem markdownItem : markdownConverter.markdownItems) {

                String sb = Pattern.compile("\\A[\\p{L}0-9_]").matcher(markdownItem.code).find() ? "\\b" : "";
                String eb = Pattern.compile("[\\p{L}0-9_]\\z").matcher(markdownItem.code).find() ? "\\b" : "\\B";
                Pattern p = Pattern.compile(sb + "(" + Pattern.quote(markdownItem.code) + ")" + eb, Pattern.UNICODE_CASE);
                Matcher m = p.matcher(content);
                int fetchPosition = 1;
                while (m.find()) {
                    int regexPosition = markdownItem.regexPosition(markdownConverter.markdownItems);
                    if (regexPosition == fetchPosition) {
                        content.setSpan(markdownItem.urlSpan, m.start(), m.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                    fetchPosition++;
                }
                position++;
            }
        } else {
            content = new SpannableStringBuilder(initialContent);
        }

        URLSpan[] urls = content.getSpans(0, (content.length() - 1), URLSpan.class);
        //Loop through links
        for (URLSpan span : urls) {
            String url = span.getURL();
            int start = content.getSpanStart(span);
            int end = content.getSpanEnd(span);
            if (start < 0 || end > content.length()) {
                continue;
            }
            content.removeSpan(span);
            //Get the matching word associated to the URL
            String word = content.subSequence(start, end).toString();
            if (word.startsWith("@") || word.startsWith("#")) {
                content.setSpan(new LongClickableSpan() {
                    @Override
                    public void onLongClick(View textView) {
                        textView.setTag(CLICKABLE_SPAN);
                        if (word.startsWith("#") && BaseMainActivity.filterFetched && MainActivity.mainFilters != null) {
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
                        Intent intent;
                        Bundle args;
                        if (word.startsWith("#")) {
                            intent = new Intent(context, HashTagActivity.class);
                            args = new Bundle();
                            args.putString(Helper.ARG_SEARCH_KEYWORD, word.trim());
                            new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                                Bundle bundle = new Bundle();
                                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                intent.putExtras(bundle);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            });

                        } else if (word.startsWith("@")) {
                            intent = new Intent(context, ProfileActivity.class);
                            args = new Bundle();
                            Mention targetedMention = null;
                            String acct = null;

                            for (Mention mention : mentions) {
                                if (word.compareToIgnoreCase("@" + mention.username) == 0) {
                                    if (!checkRemotely) {
                                        targetedMention = mention;
                                    } else {
                                        acct = mention.acct;
                                    }
                                    break;
                                }
                            }

                            if (targetedMention != null) {
                                args.putString(Helper.ARG_USER_ID, targetedMention.id);
                            } else if (acct != null) {
                                args.putString(Helper.ARG_MENTION, acct);
                            } else {
                                args.putString(Helper.ARG_MENTION, word);
                            }
                            new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                                Bundle bundle = new Bundle();
                                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                intent.putExtras(bundle);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            });
                        }
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        if (!underlineLinks) {
                            ds.setUnderlineText(status != null && status.underlined);
                        }
                        if (linkColor != -1) {
                            ds.setColor(linkColor);
                        }
                    }

                }, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                makeLinks(context, status, content, url, start, end);
            }
            replaceQuoteSpans(context, content);
            emails(context, content, status);
        }

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

        View view = viewWeakReference.get();
        List<Emoji> emojiList = null;
        if (status != null) {
            emojiList = status.emojis;
        } else if (account != null) {
            emojiList = account.emojis;
        } else if (announcement != null) {
            emojiList = announcement.emojis;
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


    private static void makeLinks(Context context, Status status, SpannableStringBuilder content, String url, int start, int end) {
        String newUrl = url;
        boolean validUrl = URLUtil.isValidUrl(url) && url.length() == (end - start);
        if (validUrl) {
            newUrl = Helper.transformURL(context, url);
        }


        //If URL has been transformed
        if (validUrl && newUrl.compareTo(url) != 0) {
            content.replace(start, end, newUrl);
            end = start + newUrl.length();
            url = newUrl;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean truncate = sharedpreferences.getBoolean(context.getString(R.string.SET_TRUNCATE_LINKS), true);
        if (truncate) {
            int truncateValue = sharedpreferences.getInt(context.getString(R.string.SET_TRUNCATE_LINKS_MAX), 30);
            if (url.length() > truncateValue && (validUrl || url.startsWith("gimini://"))) {
                newUrl = url.substring(0, truncateValue);
                newUrl += "…";
                content.replace(start, end, newUrl);
            }
        }
        int matchEnd = validUrl ? start + newUrl.length() : end;

        String finalUrl = url;
        if (content.length() < matchEnd) {
            matchEnd = content.length();
        }
        content.setSpan(new LongClickableSpan() {
            @Override
            public void onLongClick(View view) {
                Context mContext = view.getContext();
                AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(mContext);
                PopupLinksBinding popupLinksBinding = PopupLinksBinding.inflate(LayoutInflater.from(context));
                dialogBuilder.setView(popupLinksBinding.getRoot());
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
                popupLinksBinding.displayFullLink.setOnClickListener(v -> {
                    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(mContext);
                    builder.setMessage(finalUrl);
                    builder.setTitle(context.getString(R.string.display_full_link));
                    builder.setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                            .show();
                    alertDialog.dismiss();
                });
                popupLinksBinding.shareLink.setOnClickListener(v -> {
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.shared_via));
                    sendIntent.putExtra(Intent.EXTRA_TEXT, finalUrl);
                    sendIntent.setType("text/plain");
                    sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Intent intentChooser = Intent.createChooser(sendIntent, context.getString(R.string.share_with));
                    intentChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intentChooser);
                    alertDialog.dismiss();
                });

                popupLinksBinding.openOtherApp.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(finalUrl));
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
                    ClipData clip = ClipData.newPlainText(Helper.CLIP_BOARD, finalUrl);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toasty.info(context, context.getString(R.string.clipboard_url), Toast.LENGTH_LONG).show();
                    }
                    alertDialog.dismiss();
                });

                popupLinksBinding.checkRedirect.setOnClickListener(v -> {
                    try {

                        URL finalUrlCheck = new URL(finalUrl);
                        new Thread(() -> {
                            try {
                                String redirect = null;
                                if (finalUrl.startsWith("http://")) {
                                    HttpURLConnection httpURLConnection = (HttpURLConnection) finalUrlCheck.openConnection();
                                    httpURLConnection.setConnectTimeout(10 * 1000);
                                    httpURLConnection.setRequestProperty("http.keepAlive", "false");
                                    //httpsURLConnection.setRequestProperty("User-Agent", USER_AGENT);
                                    httpURLConnection.setRequestMethod("HEAD");
                                    httpURLConnection.setInstanceFollowRedirects(false);
                                    if (httpURLConnection.getResponseCode() == 301 || httpURLConnection.getResponseCode() == 302) {
                                        Map<String, List<String>> map = httpURLConnection.getHeaderFields();
                                        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                                            if (entry.toString().toLowerCase().startsWith("location")) {
                                                Matcher matcher = Patterns.WEB_URL.matcher(entry.toString());
                                                if (matcher.find()) {
                                                    redirect = matcher.group(1);
                                                }
                                            }
                                        }
                                    }
                                    httpURLConnection.getInputStream().close();
                                    if (redirect != null && redirect.compareTo(finalUrl) != 0) {
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
                                        AlertDialog.Builder builder1 = new MaterialAlertDialogBuilder(view.getContext());
                                        if (finalRedirect != null) {
                                            builder1.setMessage(context.getString(R.string.redirect_detected, finalUrl, finalRedirect));
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
                                                sendIntent1.putExtra(Intent.EXTRA_TEXT, finalUrl);
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
                                } else {
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
                                    if (redirect != null && redirect.compareTo(finalUrl) != 0) {
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
                                        AlertDialog.Builder builder1 = new MaterialAlertDialogBuilder(view.getContext());
                                        if (finalRedirect != null) {
                                            builder1.setMessage(context.getString(R.string.redirect_detected, finalUrl, finalRedirect));
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
                                                sendIntent1.putExtra(Intent.EXTRA_TEXT, finalUrl);
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
                                }

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

                textView.setTag(CLICKABLE_SPAN);
                linkClickAction(context, finalUrl);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                if (!underlineLinks) {
                    ds.setUnderlineText(status != null && status.underlined);
                }
                if (linkColor != -1) {
                    ds.setColor(linkColor);
                }
            }
        }, start, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    public static void linkClickAction(Context context, String finalUrl) {
        Pattern link = Pattern.compile("https?://([\\da-z.-]+\\.[a-z.]{2,10})/(@[\\w._-]*[0-9]*)(/[0-9]+)?$");
        Matcher matcherLink = link.matcher(finalUrl);
        Pattern linkLong = Pattern.compile("https?://([\\da-z.-]+\\.[a-z.]{2,10})/(@[\\w_.-]+@[a-zA-Z0-9][a-zA-Z0-9.-]{1,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+)(/[0-9]+)?$");
        Matcher matcherLinkLong = linkLong.matcher(finalUrl);
        Pattern userWithoutAt = Pattern.compile("https?://([\\da-z.-]+\\.[a-z.]{2,10})/(users/([\\w._-]*[0-9]*))/statuses/([0-9]+)");
        Matcher matcherUserWithoutAt = userWithoutAt.matcher(finalUrl);
        if (matcherLink.find() && !finalUrl.contains("medium.com")) {
            if (matcherLink.group(3) != null && Objects.requireNonNull(matcherLink.group(3)).length() > 0) { //It's a toot
                CrossActionHelper.fetchRemoteStatus(context, Helper.getCurrentAccount(context), finalUrl, new CrossActionHelper.Callback() {
                    @Override
                    public void federatedStatus(Status status) {
                        Intent intent = new Intent(context, ContextActivity.class);
                        Bundle args = new Bundle();
                        args.putSerializable(Helper.ARG_STATUS, status);
                        new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                            Bundle bundle = new Bundle();
                            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                            intent.putExtras(bundle);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    }

                    @Override
                    public void federatedAccount(Account account) {
                    }
                });
            } else {//It's an account
                CrossActionHelper.fetchRemoteAccount(context, Helper.getCurrentAccount(context), matcherLink.group(2) + "@" + matcherLink.group(1), new CrossActionHelper.Callback() {
                    @Override
                    public void federatedStatus(Status status) {
                    }

                    @Override
                    public void federatedAccount(Account account) {
                        Intent intent = new Intent(context, ProfileActivity.class);
                        Bundle args = new Bundle();
                        args.putSerializable(Helper.ARG_ACCOUNT, account);
                        new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                            Bundle bundle = new Bundle();
                            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                            intent.putExtras(bundle);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    }
                });
            }
        } else if (matcherLinkLong.find() && !finalUrl.contains("medium.com")) {
            if (matcherLinkLong.group(3) != null && Objects.requireNonNull(matcherLinkLong.group(3)).length() > 0) { //It's a toot
                CrossActionHelper.fetchRemoteStatus(context, Helper.getCurrentAccount(context), finalUrl, new CrossActionHelper.Callback() {
                    @Override
                    public void federatedStatus(Status status) {
                        Intent intent = new Intent(context, ContextActivity.class);
                        Bundle args = new Bundle();
                        args.putSerializable(Helper.ARG_STATUS, status);
                        new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                            Bundle bundle = new Bundle();
                            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                            intent.putExtras(bundle);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    }

                    @Override
                    public void federatedAccount(Account account) {
                    }
                });
            } else if (matcherLinkLong.group(2) != null) {//It's an account
                CrossActionHelper.fetchRemoteAccount(context, Helper.getCurrentAccount(context), matcherLinkLong.group(2), new CrossActionHelper.Callback() {
                    @Override
                    public void federatedStatus(Status status) {
                    }

                    @Override
                    public void federatedAccount(Account account) {
                        Intent intent = new Intent(context, ProfileActivity.class);
                        Bundle args = new Bundle();
                        args.putSerializable(Helper.ARG_ACCOUNT, account);
                        new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                            Bundle bundle = new Bundle();
                            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                            intent.putExtras(bundle);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    }
                });
            }
        } else if (matcherUserWithoutAt.find() && !finalUrl.contains("medium.com")) {
            if (matcherUserWithoutAt.group(4) != null && Objects.requireNonNull(matcherUserWithoutAt.group(4)).length() > 0) { //It's a toot
                CrossActionHelper.fetchRemoteStatus(context, Helper.getCurrentAccount(context), finalUrl, new CrossActionHelper.Callback() {
                    @Override
                    public void federatedStatus(Status status) {
                        Intent intent = new Intent(context, ContextActivity.class);
                        Bundle args = new Bundle();
                        args.putSerializable(Helper.ARG_STATUS, status);
                        new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                            Bundle bundle = new Bundle();
                            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                            intent.putExtras(bundle);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    }

                    @Override
                    public void federatedAccount(Account account) {
                    }
                });
            } else {//It's an account
                CrossActionHelper.fetchRemoteAccount(context, Helper.getCurrentAccount(context), matcherUserWithoutAt.group(3) + "@" + matcherUserWithoutAt.group(1), new CrossActionHelper.Callback() {
                    @Override
                    public void federatedStatus(Status status) {
                    }

                    @Override
                    public void federatedAccount(Account account) {
                        Intent intent = new Intent(context, ProfileActivity.class);
                        Bundle args = new Bundle();
                        args.putSerializable(Helper.ARG_ACCOUNT, account);
                        new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                            Bundle bundle = new Bundle();
                            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                            intent.putExtras(bundle);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    }
                });
            }
        } else {
            Helper.openBrowser(context, finalUrl);
        }
    }

    private static void emails(Context context, Spannable content, Status status) {
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
                        if (!underlineLinks) {
                            ds.setUnderlineText(status != null && status.underlined);
                        }
                        if (linkColor != -1) {
                            ds.setColor(linkColor);
                        }
                    }
                }, matchStart, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static void addTagToFilter(Context context, String tag, Status status, Filter filter) {
        for (Filter.KeywordsAttributes keywords : filter.keywords) {
            if (keywords.keyword.equalsIgnoreCase(tag)) {
                return;
            }
        }
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
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
                    StatusAdapter.sendAction(context, Helper.ARG_TIMELINE_REFRESH_ALL, null, null);
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
                                                Bundle args = new Bundle();
                                                args.putSerializable(Helper.ARG_ACCOUNT, account.moved);
                                                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                                                    Bundle bundle = new Bundle();
                                                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                                    intent.putExtras(bundle);
                                                    context.startActivity(intent);
                                                });
                                            }

                                            @Override
                                            public void updateDrawState(@NonNull TextPaint ds) {
                                                super.updateDrawState(ds);
                                                if (!underlineLinks) {
                                                    ds.setUnderlineText(false);
                                                }
                                                if (linkColor != -1) {
                                                    ds.setColor(linkColor);
                                                }
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
