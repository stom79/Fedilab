package app.fedilab.android.mastodon.client.entities.nitter;
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

import androidx.annotation.NonNull;

import org.jsoup.select.Elements;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.io.Serializable;
import java.net.IDN;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.mastodon.client.endpoints.MastodonTimelinesService;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

@Root(name = "rss", strict = false)
public class Nitter implements Serializable {


    public static HashMap<String, Nitter> accounts = new HashMap<>();

    @Element(name = "title")
    @Path("channel")
    public String title;
    @Element(name = "image")
    @Path("channel")
    public Image image;
    @ElementList(name = "item", inline = true)
    @Path("channel")
    public List<FeedItem> mFeedItems;

    public static MastodonTimelinesService initInstanceXMLOnly(Context context, String instance) {

        OkHttpClient okHttpClient = Helper.myOkHttpClient(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null))
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    public static Status convert(Context context, String instance, FeedItem feedItem) {
        Status status = new Status();
        Matcher matcherLink = Helper.nitterIDPattern.matcher(feedItem.link);
        if (matcherLink.find()) {
            status.id = matcherLink.group(1);
        } else {
            status.id = feedItem.pubDate;
        }
        status.content = feedItem.description;
        status.text = feedItem.title;
        status.content = status.content.replaceAll("<img [^>]*src=\"[^\"]+\"[^>]*>", "");
        status.visibility = "public";
        String dateFormat = "E', 'dd' 'MMM' 'yyyy' 'hh:m:s' GMT'";
        status.created_at = Helper.stringToDateWithFormat(context, feedItem.pubDate, dateFormat);
        status.uri = feedItem.guid;
        status.url = feedItem.link;
        if (!accounts.containsKey(feedItem.creator)) {
            MastodonTimelinesService mastodonTimelinesService = initInstanceXMLOnly(context, instance);
            Call<Nitter> accountCall = mastodonTimelinesService.getNitterAccount(feedItem.creator.replace("@", ""));
            if (accountCall != null) {
                try {
                    Response<Nitter> publicTlResponse = accountCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        Nitter nitterAccount = publicTlResponse.body();
                        accounts.put(feedItem.creator, nitterAccount);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Nitter nitterAccount = accounts.get(feedItem.creator);
        Account account = new Account();
        if (nitterAccount != null) {
            String[] names = nitterAccount.image.title.split("/");
            account.id = feedItem.guid;
            account.acct = names[1].replace("@", "");
            account.username = names[1].replace("@", "");
            account.display_name = names[0];
            account.avatar = nitterAccount.image.url;
            account.avatar_static = nitterAccount.image.url;
            account.url = nitterAccount.image.link;
        } else {
            account.id = feedItem.guid;
            account.acct = feedItem.creator.replace("@", "");
            account.username = feedItem.creator.replace("@", "");
            account.display_name = feedItem.creator.replace("@", "");
            account.avatar = "";
            account.avatar_static = "";
            account.url = feedItem.link;
        }
        status.account = account;

        if (feedItem.description != null) {
            Pattern imgPattern = Pattern.compile("<img [^>]*src=\"([^\"]+)\"[^>]*>");
            Matcher matcher = imgPattern.matcher(feedItem.description);
            String description = feedItem.description;
            ArrayList<Attachment> attachmentList = new ArrayList<>();
            while (matcher.find()) {
                description = description.replaceAll(Pattern.quote(matcher.group()), "");
                Attachment attachment = new Attachment();
                attachment.type = "image";
                attachment.url = matcher.group(1);
                attachment.preview_url = matcher.group(1);
                attachment.id = matcher.group(1);
                attachmentList.add(attachment);
            }
            status.media_attachments = attachmentList;
        }
        return status;
    }

    @Root(name = "image", strict = false)
    public static class Image implements Serializable {
        @Element(name = "title")
        public String title;
        @Element(name = "url")
        public String url;
        @Element(name = "link")
        public String link;
    }

    @Root(name = "item", strict = false)
    public static class FeedItem implements Serializable {
        @Namespace(prefix = "dc")
        @Element(name = "creator", required = false)
        public String creator;
        @Element(name = "title", required = false)
        public String title;
        @Element(name = "description", required = false)
        public String description;
        @Element(name = "pubDate", required = false)
        public String pubDate;
        @Element(name = "guid", required = false)
        public String guid;
        @Element(name = "link", required = false)
        public String link;

        @NonNull
        @Override
        public String toString() {
            return "creator: " + creator + "\r" + "title: " + title + "\r" + "description: "
                    + description + "\r" + "pubDate: " + pubDate + "\r"
                    + "guid: " + guid + "\r" + "link: " + link;
        }
    }


    public static Status nitterHTMLParser(Context context, org.jsoup.nodes.Element timelineItem, String nitterInstance) {

        if(timelineItem == null) {
            return null;
        }
        Status status = new Status();
        Account account = new Account();
        String fedilabInstance = "nitter.fedilab.app";


        org.jsoup.nodes.Element messageLink;
        if(timelineItem.select(".quote-text").html().isEmpty()) {
            status.content = timelineItem.select(".tweet-content").html();
            status.text = timelineItem.select(".tweet-content").text();
            status.url = "https://"+ nitterInstance +timelineItem.select(".tweet-link").attr("href");
            messageLink =  timelineItem.select(".tweet-link").first();
        } else {
            status.content = timelineItem.select(".quote-text").html();
            status.text = timelineItem.select(".quote-text").text();
            status.url = "https://"+ nitterInstance +timelineItem.select(".quote-link").attr("href");
            messageLink =  timelineItem.select(".quote-link").first();
        }
        status.uri = status.url;

        String status_id = String.valueOf(ThreadLocalRandom.current().nextLong(10,10000000));;
        if(messageLink != null){
            String[] splitLink = messageLink.attr("href").split("/");
            status_id = splitLink[splitLink.length-1];
        }
        String pubDate = timelineItem.select(".tweet-date").select("a").attr("title");
        org.jsoup.nodes.Element nameElement = timelineItem.select(".fullname").first();
        String name = nameElement!= null?nameElement.text():"";
        org.jsoup.nodes.Element userNameElement = timelineItem.select(".username").first();
        String userName = userNameElement!= null?userNameElement.text().replace("@",""):"";
        String avatar = "https://" + fedilabInstance + timelineItem.select(".avatar").attr("src");
        account.id = userName;
        account.acct = userName;
        if(timelineItem.select(".replying-to").html().isEmpty()) {
            account.username = userName;
            account.display_name = name;
        } else {
            account.display_name = timelineItem.select(".fullname").text() +"&nbsp;" +timelineItem.select(".replying-to").text();
        }

        account.avatar = avatar;
        account.avatar_static = avatar;
        account.url = "https://"+ nitterInstance +"/" + userName;
        status.id = status_id;
        status.account = account;



        Elements imageElements = timelineItem.select(".attachments").select("img");
        Elements videoElements = timelineItem.select(".attachments").select("video");
        ArrayList<Attachment> attachmentList = new ArrayList<>();
        for(org.jsoup.nodes.Element imageElement: imageElements) {
            Attachment attachment = new Attachment();
            attachment.type = "image";
            attachment.url = "https://"+fedilabInstance+imageElement.attr("src");
            attachment.preview_url = "https://"+fedilabInstance+imageElement.attr("src");
            attachment.id = imageElement.attr("src");
            attachmentList.add(attachment);
        }
        for(org.jsoup.nodes.Element videoElement: videoElements) {
            Attachment attachment = new Attachment();
            attachment.type = "video";
            attachment.url = "https://"+fedilabInstance+videoElement.child(0).attr("src");
            attachment.preview_url = "https://"+fedilabInstance+videoElement.attr("poster");
            attachment.id = videoElement.attr("poster");
            attachmentList.add(attachment);
        }
        status.visibility = "public";
        status.media_attachments = attachmentList;
        String dateFormat = "MMM d', 'yyyy' · 'h:m a' UTC'";
        status.created_at = Helper.stringToDateWithFormat(context, pubDate, dateFormat);
        return status;
    }


}
