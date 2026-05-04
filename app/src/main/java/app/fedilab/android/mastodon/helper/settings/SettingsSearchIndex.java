package app.fedilab.android.mastodon.helper.settings;
/* Copyright 2026 Thomas Schneider
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.fedilab.android.R;

public class SettingsSearchIndex {

    private static List<SettingsSearchEntry> entries;

    public static List<SettingsSearchEntry> getIndex() {
        if (entries == null) {
            entries = buildIndex();
        }
        return entries;
    }

    public static List<SettingsSearchEntry> search(Context context, String query) {
        List<SettingsSearchEntry> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        for (SettingsSearchEntry entry : getIndex()) {
            String title = context.getString(entry.getTitleResId()).toLowerCase(Locale.ROOT);
            String keywords = entry.getKeywordsResId() != 0 ? context.getString(entry.getKeywordsResId()).toLowerCase(Locale.ROOT) : "";
            String category = context.getString(entry.getCategoryTitleResId()).toLowerCase(Locale.ROOT);
            if (title.contains(normalizedQuery) || keywords.contains(normalizedQuery) || category.contains(normalizedQuery)) {
                results.add(entry);
            }
        }
        return results;
    }

    private static List<SettingsSearchEntry> buildIndex() {
        List<SettingsSearchEntry> list = new ArrayList<>();

        // Timelines
        int catTimelines = R.string.settings_category_label_timelines;
        int navTimelines = R.id.categories_to_timelines;

        list.add(new SettingsSearchEntry(R.string.remember_position, R.string.kw_remember_position, catTimelines, navTimelines, "SET_REMEMBER_POSITION"));
        list.add(new SettingsSearchEntry(R.string.auto_fetch_missing, R.string.kw_auto_fetch_missing, catTimelines, navTimelines, "SET_AUTO_FETCH_MISSING_MESSAGES"));
        list.add(new SettingsSearchEntry(R.string.group_reblogs, R.string.kw_group_reblogs, catTimelines, navTimelines, "SET_GROUP_REBLOGS"));
        list.add(new SettingsSearchEntry(R.string.chat_timeline_for_direct, R.string.kw_chat_direct, catTimelines, navTimelines, "SET_CHAT_FOR_CONVERSATION"));
        list.add(new SettingsSearchEntry(R.string.boost_original_date, R.string.kw_boost_date, catTimelines, navTimelines, "SET_BOOST_ORIGINAL_DATE"));
        list.add(new SettingsSearchEntry(R.string.timeline_scrollbar, R.string.kw_scrollbar, catTimelines, navTimelines, "SET_TIMELINE_SCROLLBAR"));
        list.add(new SettingsSearchEntry(R.string.reverse_timeline, R.string.kw_reverse_timeline, catTimelines, navTimelines, "SET_REVERSE_TIMELINE"));
        list.add(new SettingsSearchEntry(R.string.highlight_new_comments, R.string.kw_highlight_comments, catTimelines, navTimelines, "SET_HIGHLIGHT_NEW_COMMENTS"));
        list.add(new SettingsSearchEntry(R.string.max_indentation_thread, R.string.kw_max_indentation, catTimelines, navTimelines, "SET_MAX_INDENTATION"));
        list.add(new SettingsSearchEntry(R.string.set_share_validation, R.string.kw_confirm_boost, catTimelines, navTimelines, "SET_NOTIF_VALIDATION"));
        list.add(new SettingsSearchEntry(R.string.warn_boost_no_media_description, R.string.kw_warn_boost_alt, catTimelines, navTimelines, "SET_MANDATORY_ALT_TEXT_FOR_BOOSTS"));
        list.add(new SettingsSearchEntry(R.string.set_share_validation_fav, R.string.kw_confirm_fav, catTimelines, navTimelines, "SET_NOTIF_VALIDATION_FAV"));
        list.add(new SettingsSearchEntry(R.string.set_display_counter, R.string.kw_display_counter, catTimelines, navTimelines, "SET_DISPLAY_COUNTER_FAV_BOOST"));
        list.add(new SettingsSearchEntry(R.string.set_display_bookmark_indication, R.string.kw_bookmark, catTimelines, navTimelines, "SET_DISPLAY_BOOKMARK"));
        list.add(new SettingsSearchEntry(R.string.set_display_quote_indication, R.string.kw_quote_button, catTimelines, navTimelines, "SET_QUOTE_BUTTON"));
        list.add(new SettingsSearchEntry(R.string.set_toots_page, R.string.kw_toots_per_page, catTimelines, navTimelines, "SET_STATUSES_PER_CALL"));
        list.add(new SettingsSearchEntry(R.string.set_accounts_page, R.string.kw_accounts_per_page, catTimelines, navTimelines, "SET_ACCOUNTS_PER_CALL"));

        // Content
        int catContent = R.string.settings_category_label_content;
        int navContent = R.id.categories_to_content;

        list.add(new SettingsSearchEntry(R.string.expand_cw, R.string.kw_expand_cw, catContent, navContent, "SET_EXPAND_CW"));
        list.add(new SettingsSearchEntry(R.string.truncate_long_toots, R.string.kw_truncate_toots, catContent, navContent, "SET_TRUNCATE_TOOTS_SIZE"));
        list.add(new SettingsSearchEntry(R.string.truncate_links, R.string.kw_truncate_links, catContent, navContent, "SET_TRUNCATE_LINKS"));
        list.add(new SettingsSearchEntry(R.string.truncate_links_max, R.string.kw_truncate_links_max, catContent, navContent, "SET_TRUNCATE_LINKS_MAX"));
        list.add(new SettingsSearchEntry(R.string.underline_links, R.string.kw_underline_links, catContent, navContent, "SET_UNDERLINE_CLICKABLE"));
        list.add(new SettingsSearchEntry(R.string.underline_bottom_hashtags, R.string.kw_underline_hashtags, catContent, navContent, "SET_UNDERLINE_BOTTOM_HASHTAGS"));
        list.add(new SettingsSearchEntry(R.string.markdown_support, R.string.kw_markdown, catContent, navContent, "SET_MARKDOWN_SUPPORT"));
        list.add(new SettingsSearchEntry(R.string.pronouns_support, R.string.kw_pronouns, catContent, navContent, "SET_PRONOUNS_SUPPORT"));
        list.add(new SettingsSearchEntry(R.string.set_display_relative_date, R.string.kw_relative_date, catContent, navContent, "SET_DISPLAY_RELATIVE_DATE"));
        list.add(new SettingsSearchEntry(R.string.hide_single_media_with_card, R.string.kw_hide_media_card, catContent, navContent, "SET_HIDE_SINGLE_MEDIA_WITH_CARD"));

        // Media
        int catMedia = R.string.settings_category_label_media;
        int navMedia = R.id.categories_to_media;

        list.add(new SettingsSearchEntry(R.string.expand_image, R.string.kw_expand_media, catMedia, navMedia, "SET_EXPAND_MEDIA"));
        list.add(new SettingsSearchEntry(R.string.fetch_remote_media, R.string.kw_fetch_remote_media, catMedia, navMedia, "SET_FETCH_REMOTE_MEDIA"));
        list.add(new SettingsSearchEntry(R.string.set_fit_preview, R.string.kw_fit_preview, catMedia, navMedia, "SET_FULL_PREVIEW"));
        list.add(new SettingsSearchEntry(R.string.set_autoplay_gif, R.string.kw_autoplay_gif, catMedia, navMedia, "SET_AUTO_PLAY_GIG_MEDIA"));
        list.add(new SettingsSearchEntry(R.string.set_disable_gif, R.string.kw_disable_gif, catMedia, navMedia, "SET_DISABLE_GIF"));
        list.add(new SettingsSearchEntry(R.string.set_disable_animated_emoji, R.string.kw_disable_animated_emoji, catMedia, navMedia, "SET_DISABLE_ANIMATED_EMOJI"));
        list.add(new SettingsSearchEntry(R.string.set_link_previews, R.string.kw_link_previews, catMedia, navMedia, "SET_LINK_PREVIEWS"));
        list.add(new SettingsSearchEntry(R.string.load_media_type_title, R.string.kw_load_media_type, catMedia, navMedia, "SET_LOAD_MEDIA_TYPE"));
        list.add(new SettingsSearchEntry(R.string.set_long_press_media, R.string.kw_long_press_media, catMedia, navMedia, "SET_LONG_PRESS_STORE_MEDIA"));
        list.add(new SettingsSearchEntry(R.string.display_media_notification, R.string.kw_media_notification, catMedia, navMedia, "SET_DISPLAY_MEDIA_NOTIFICATION"));
        list.add(new SettingsSearchEntry(R.string.set_sensitive_indicator, R.string.kw_sensitive_indicator, catMedia, navMedia, "SET_SENSITIVE_INDICATOR"));
        list.add(new SettingsSearchEntry(R.string.set_media_description_indicator, R.string.kw_media_description_indicator, catMedia, navMedia, "SET_MEDIA_DESCRIPTION_INDICATOR"));
        list.add(new SettingsSearchEntry(R.string.set_pixelfed_presentation, R.string.kw_pixelfed_presentation, catMedia, navMedia, "SET_PIXELFED_PRESENTATION"));
        list.add(new SettingsSearchEntry(R.string.set_pixelfed_full_media, R.string.kw_pixelfed_full_media, catMedia, navMedia, "SET_PIXELFED_FULL_MEDIA"));
        list.add(new SettingsSearchEntry(R.string.set_video_cache, R.string.kw_video_cache, catMedia, navMedia, "SET_VIDEO_CACHE"));
        list.add(new SettingsSearchEntry(R.string.set_nsfw_timeout, R.string.kw_nsfw_timeout, catMedia, navMedia, "SET_NSFW_TIMEOUT"));
        list.add(new SettingsSearchEntry(R.string.set_med_desc_timeout, R.string.kw_media_desc_timeout, catMedia, navMedia, "SET_MED_DESC_TIMEOUT"));

        // Translation
        int catTranslation = R.string.settings_category_label_translation;
        int navTranslation = R.id.categories_to_translation;

        list.add(new SettingsSearchEntry(R.string.set_live_translate_title, R.string.kw_live_translate, catTranslation, navTranslation, "SET_LIVE_TRANSLATE_MULTIPLE"));
        list.add(new SettingsSearchEntry(R.string.set_translator, R.string.kw_translator, catTranslation, navTranslation, "SET_TRANSLATOR"));
        list.add(new SettingsSearchEntry(R.string.api_key, R.string.kw_translator_api_key, catTranslation, navTranslation, "SET_TRANSLATOR_API_KEY"));
        list.add(new SettingsSearchEntry(R.string.translator_domain, R.string.kw_translator_domain, catTranslation, navTranslation, "SET_TRANSLATOR_DOMAIN"));
        list.add(new SettingsSearchEntry(R.string.set_translator_version, R.string.kw_translator_version, catTranslation, navTranslation, "SET_TRANSLATOR_VERSION"));
        list.add(new SettingsSearchEntry(R.string.set_translate_button, R.string.kw_translate_button, catTranslation, navTranslation, "SET_TRANSLATE_BUTTON"));

        // Interface
        int catInterface = R.string.settings_category_label_interface;
        int navInterface = R.id.categories_to_interface;

        list.add(new SettingsSearchEntry(R.string.set_single_topbar_title, R.string.kw_single_topbar, catInterface, navInterface, "SET_USE_SINGLE_TOPBAR"));
        list.add(new SettingsSearchEntry(R.string.set_disable_topbar_scrolling_title, R.string.kw_topbar_scrolling, catInterface, navInterface, "SET_DISABLE_TOPBAR_SCROLLING"));
        list.add(new SettingsSearchEntry(R.string.set_audo_hide_compose_title, R.string.kw_auto_hide_compose, catInterface, navInterface, "SET_AUTO_HIDE_COMPOSE"));
        list.add(new SettingsSearchEntry(R.string.set_timelines_in_a_list_title, R.string.kw_timelines_list, catInterface, navInterface, "SET_TIMELINES_IN_A_LIST"));
        list.add(new SettingsSearchEntry(R.string.set_remove_left_margin_title, R.string.kw_left_margin, catInterface, navInterface, "SET_REMOVE_LEFT_MARGIN"));
        list.add(new SettingsSearchEntry(R.string.set_remote_conversation_title, R.string.kw_remote_conversation, catInterface, navInterface, "SET_CONVERSATION_REMOTELY"));
        list.add(new SettingsSearchEntry(R.string.set_remote_profile_title, R.string.kw_remote_profile, catInterface, navInterface, "SET_PROFILE_REMOTELY"));
        list.add(new SettingsSearchEntry(R.string.set_profile_image_shape, R.string.kw_profile_image_shape, catInterface, navInterface, "SET_PROFILE_IMAGE_SHAPE"));
        list.add(new SettingsSearchEntry(R.string.set_unfollow_validation_title, R.string.kw_unfollow_confirm, catInterface, navInterface, "SET_UNFOLLOW_VALIDATION"));
        list.add(new SettingsSearchEntry(R.string.set_display_counters, R.string.kw_display_counters, catInterface, navInterface, "SET_DISPLAY_COUNTERS"));
        list.add(new SettingsSearchEntry(R.string.set_display_compact_buttons, R.string.kw_compact_buttons, catInterface, navInterface, "SET_DISPLAY_COMPACT_ACTION_BUTTON"));
        list.add(new SettingsSearchEntry(R.string.set_use_cache, R.string.kw_use_cache, catInterface, navInterface, "SET_USE_CACHE"));
        list.add(new SettingsSearchEntry(R.string.custom_tabs, R.string.kw_custom_tabs, catInterface, navInterface, "SET_CUSTOM_TABS"));
        list.add(new SettingsSearchEntry(R.string.set_clear_cache_exit, R.string.kw_clear_cache, catInterface, navInterface, "SET_CLEAR_CACHE_EXIT"));
        list.add(new SettingsSearchEntry(R.string.set_enable_crash_report, R.string.kw_crash_report, catInterface, navInterface, "SET_SEND_CRASH_REPORTS"));
        list.add(new SettingsSearchEntry(R.string.set_disable_release_notes, R.string.kw_release_notes, catInterface, navInterface, "SET_DISABLE_RELEASE_NOTES_ALERT"));
        list.add(new SettingsSearchEntry(R.string.text_size, R.string.kw_text_size, catInterface, navInterface, "SET_FONT_SCALE_INT"));
        list.add(new SettingsSearchEntry(R.string.icon_size, R.string.kw_icon_size, catInterface, navInterface, "SET_FONT_SCALE_ICON_INT"));
        list.add(new SettingsSearchEntry(R.string.change_logo, R.string.kw_logo, catInterface, navInterface, "SET_LOGO_LAUNCHER"));

        // Compose
        int catCompose = R.string.compose;
        int navCompose = R.id.categories_to_compose;

        list.add(new SettingsSearchEntry(R.string.set_capitalize, R.string.kw_capitalize, catCompose, navCompose, "SET_CAPITALIZE"));
        list.add(new SettingsSearchEntry(R.string.thread_long_message, R.string.kw_thread_long_message, catCompose, navCompose, "SET_THREAD_MESSAGE"));
        list.add(new SettingsSearchEntry(R.string.set_mention_at_top, R.string.kw_mention_top, catCompose, navCompose, "SET_MENTIONS_AT_TOP"));
        list.add(new SettingsSearchEntry(R.string.set_mention_booster, R.string.kw_mention_booster, catCompose, navCompose, "SET_MENTION_BOOSTER"));
        list.add(new SettingsSearchEntry(R.string.set_resize_picture, R.string.kw_resize_picture, catCompose, navCompose, "SET_PICTURE_COMPRESSED"));
        list.add(new SettingsSearchEntry(R.string.set_unlisted_replies, R.string.kw_unlisted_replies, catCompose, navCompose, "SET_UNLISTED_REPLIES"));
        list.add(new SettingsSearchEntry(R.string.set_colorize_visibility, R.string.kw_colorize_visibility, catCompose, navCompose, "SET_COLORIZE_FOR_VISIBILITY"));
        list.add(new SettingsSearchEntry(R.string.set_language_picker_title, R.string.kw_language_picker, catCompose, navCompose, "SET_SELECTED_LANGUAGE"));
        list.add(new SettingsSearchEntry(R.string.set_watermark, R.string.kw_watermark, catCompose, navCompose, "SET_WATERMARK"));
        list.add(new SettingsSearchEntry(R.string.set_alt_text_mandatory, R.string.kw_alt_text, catCompose, navCompose, "SET_MANDATORY_ALT_TEXT"));
        list.add(new SettingsSearchEntry(R.string.set_alt_text_mandatory_warn, R.string.kw_alt_text_warn, catCompose, navCompose, "SET_MANDATORY_ALT_TEXT_WARN"));
        list.add(new SettingsSearchEntry(R.string.set_maths_support, R.string.kw_maths, catCompose, navCompose, "SET_MATHS_COMPOSER"));
        list.add(new SettingsSearchEntry(R.string.set_display_emoji, R.string.kw_emoji_one, catCompose, navCompose, "SET_DISPLAY_EMOJI"));
        list.add(new SettingsSearchEntry(R.string.set_share_details, R.string.kw_share_details, catCompose, navCompose, "SET_SHARE_DETAILS"));
        list.add(new SettingsSearchEntry(R.string.set_retrieve_metadata_share_from_extras, R.string.kw_share_metadata, catCompose, navCompose, "SET_RETRIEVE_METADATA_IF_URL_FROM_EXTERAL"));
        list.add(new SettingsSearchEntry(R.string.settings_title_custom_sharing, R.string.kw_custom_sharing, catCompose, navCompose, "SET_CUSTOM_SHARING"));
        list.add(new SettingsSearchEntry(R.string.set_forward_tags, R.string.kw_forward_tags, catCompose, navCompose, "SET_FORWARD_TAGS_IN_REPLY"));

        // Notifications
        int catNotifications = R.string.notifications;
        int navNotifications = R.id.categories_to_notifications;

        list.add(new SettingsSearchEntry(R.string.aggregate_notifications, R.string.kw_aggregate_notif, catNotifications, navNotifications, "SET_AGGREGATE_NOTIFICATION"));
        list.add(new SettingsSearchEntry(R.string.set_notifications_page, R.string.kw_notifications_per_page, catNotifications, navNotifications, "SET_NOTIFICATIONS_PER_CALL"));
        list.add(new SettingsSearchEntry(R.string.type_of_notifications_title, R.string.kw_notification_type, catNotifications, navNotifications, "SET_NOTIFICATION_TYPE"));
        list.add(new SettingsSearchEntry(R.string.type_of_notifications_delay_title, R.string.kw_notification_delay, catNotifications, navNotifications, "SET_NOTIFICATION_DELAY_VALUE"));
        list.add(new SettingsSearchEntry(R.string.push_distributors, R.string.kw_push_distributor, catNotifications, navNotifications, "SET_PUSH_DISTRIBUTOR"));
        list.add(new SettingsSearchEntry(R.string.set_remove_battery, R.string.kw_battery, catNotifications, navNotifications, "SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS"));
        list.add(new SettingsSearchEntry(R.string.set_notif_follow, R.string.kw_notif_follow, catNotifications, navNotifications, "SET_NOTIF_FOLLOW"));
        list.add(new SettingsSearchEntry(R.string.set_notif_follow_mention, R.string.kw_notif_mention, catNotifications, navNotifications, "SET_NOTIF_MENTION"));
        list.add(new SettingsSearchEntry(R.string.set_notif_follow_add, R.string.kw_notif_fav, catNotifications, navNotifications, "SET_NOTIF_FAVOURITE"));
        list.add(new SettingsSearchEntry(R.string.set_notif_follow_share, R.string.kw_notif_boost, catNotifications, navNotifications, "SET_NOTIF_SHARE"));
        list.add(new SettingsSearchEntry(R.string.set_notif_follow_poll, R.string.kw_notif_poll, catNotifications, navNotifications, "SET_NOTIF_POLL"));
        list.add(new SettingsSearchEntry(R.string.set_notif_status, R.string.kw_notif_status, catNotifications, navNotifications, "SET_NOTIF_STATUS"));
        list.add(new SettingsSearchEntry(R.string.set_notif_update, R.string.kw_notif_update, catNotifications, navNotifications, "SET_NOTIF_UPDATE"));
        list.add(new SettingsSearchEntry(R.string.set_notif_user_sign_up, R.string.kw_notif_signup, catNotifications, navNotifications, "SET_NOTIF_ADMIN_SIGNUP"));
        list.add(new SettingsSearchEntry(R.string.set_notif_admin_report, R.string.kw_notif_report, catNotifications, navNotifications, "SET_NOTIF_ADMIN_REPORT"));
        list.add(new SettingsSearchEntry(R.string.set_notif_silent, R.string.kw_silent_notif, catNotifications, navNotifications, "SET_NOTIF_SILENT"));
        list.add(new SettingsSearchEntry(R.string.set_led_colour, R.string.kw_led_colour, catNotifications, navNotifications, "SET_LED_COLOUR_VAL_N"));
        list.add(new SettingsSearchEntry(R.string.set_enable_time_slot, R.string.kw_time_slot, catNotifications, navNotifications, "SET_ENABLE_TIME_SLOT"));

        // Theming
        int catTheming = R.string.theming;
        int navTheming = R.id.categories_to_theming;

        list.add(new SettingsSearchEntry(R.string.type_of_theme, R.string.kw_theme_mode, catTheming, navTheming, "SET_THEME_BASE"));
        list.add(new SettingsSearchEntry(R.string.set_dynamic_color, R.string.kw_dynamic_color, catTheming, navTheming, "SET_DYNAMICCOLOR"));
        list.add(new SettingsSearchEntry(R.string.set_custom_accent, R.string.kw_accent_color, catTheming, navTheming, "SET_CUSTOM_ACCENT"));
        list.add(new SettingsSearchEntry(R.string.type_default_theme_light, R.string.kw_theme_light, catTheming, navTheming, "SET_THEME_DEFAULT_LIGHT"));
        list.add(new SettingsSearchEntry(R.string.type_default_theme_dark, R.string.kw_theme_dark, catTheming, navTheming, "SET_THEME_DEFAULT_DARK"));
        list.add(new SettingsSearchEntry(R.string.set_cardview, R.string.kw_cardview, catTheming, navTheming, "SET_CARDVIEW"));
        list.add(new SettingsSearchEntry(R.string.set_customize_light, R.string.kw_customize_light, catTheming, navTheming, "SET_CUSTOMIZE_LIGHT_COLORS"));
        list.add(new SettingsSearchEntry(R.string.set_customize_dark, R.string.kw_customize_dark, catTheming, navTheming, "SET_CUSTOMIZE_DARK_COLORS"));

        // Privacy
        int catPrivacy = R.string.action_privacy;
        int navPrivacy = R.id.categories_to_privacy;

        list.add(new SettingsSearchEntry(R.string.replace_youtube, R.string.kw_youtube, catPrivacy, navPrivacy, "SET_INVIDIOUS"));
        list.add(new SettingsSearchEntry(R.string.replace_twitter, R.string.kw_twitter, catPrivacy, navPrivacy, "SET_NITTER"));
        list.add(new SettingsSearchEntry(R.string.replace_instagram, R.string.kw_instagram, catPrivacy, navPrivacy, "SET_BIBLIOGRAM"));
        list.add(new SettingsSearchEntry(R.string.replace_reddit, R.string.kw_reddit, catPrivacy, navPrivacy, "SET_LIBREDDIT"));
        list.add(new SettingsSearchEntry(R.string.replace_medium, R.string.kw_medium, catPrivacy, navPrivacy, "REPLACE_MEDIUM"));
        list.add(new SettingsSearchEntry(R.string.set_tracking_parameters, R.string.kw_tracking, catPrivacy, navPrivacy, "SET_FILTER_TRACKING"));

        // Home cache
        int catHomeCache = R.string.home_cache;
        int navHomeCache = R.id.categories_to_home_cache;

        list.add(new SettingsSearchEntry(R.string.set_fetch_home, R.string.kw_fetch_home, catHomeCache, navHomeCache, "SET_FETCH_HOME"));
        list.add(new SettingsSearchEntry(R.string.type_of_home_delay_title, R.string.kw_fetch_home_delay, catHomeCache, navHomeCache, "SET_FETCH_HOME_DELAY_VALUE"));

        // Language
        int catLanguage = R.string.languages;
        int navLanguage = R.id.categories_to_language;

        list.add(new SettingsSearchEntry(R.string.set_change_locale, R.string.kw_change_locale, catLanguage, navLanguage, "SET_DEFAULT_LOCALE_NEW"));

        // Network
        int catNetwork = R.string.network;
        int navNetwork = R.id.categories_to_network;

        list.add(new SettingsSearchEntry(R.string.proxy_set, R.string.kw_proxy, catNetwork, navNetwork, "pref_key_proxy"));

        // Extra features
        int catExtra = R.string.set_extand_extra_features_title;
        int navExtra = R.id.categories_to_extra_features;

        list.add(new SettingsSearchEntry(R.string.set_extand_extra_features_title, R.string.kw_extra_features, catExtra, navExtra, "SET_EXTAND_EXTRA_FEATURES"));
        list.add(new SettingsSearchEntry(R.string.set_display_translate_indication, R.string.kw_display_translate, catExtra, navExtra, "SET_DISPLAY_TRANSLATE"));
        list.add(new SettingsSearchEntry(R.string.set_display_reaction_indication, R.string.kw_reactions, catExtra, navExtra, "SET_DISPLAY_REACTIONS"));
        list.add(new SettingsSearchEntry(R.string.set_display_local_only, R.string.kw_local_only, catExtra, navExtra, "SET_DISPLAY_LOCAL_ONLY"));
        list.add(new SettingsSearchEntry(R.string.set_post_format, R.string.kw_post_format, catExtra, navExtra, "SET_POST_FORMAT"));

        return list;
    }
}
