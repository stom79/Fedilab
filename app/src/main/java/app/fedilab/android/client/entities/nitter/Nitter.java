package app.fedilab.android.client.entities.nitter;


import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.client.endpoints.MastodonTimelinesService;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

@Root(name = "rss", strict = false)
public class Nitter implements Serializable {


    public static HashMap<String, NitterAccount> accounts = new HashMap<>();
    @Element(name = "channel")
    public Channel channel;

    public static MastodonTimelinesService initInstanceXMLOnly(Context context, String instance) {
        Gson gson = new GsonBuilder().setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").create();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(context))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    public static Status convert(Context context, String instance, FeedItem feedItem) {
        Status status = new Status();
        status.id = feedItem.pubDate.toString();
        status.content = feedItem.title;
        status.text = feedItem.title;
        status.visibility = "public";
        status.created_at = feedItem.pubDate;
        status.uri = feedItem.guid;
        status.url = feedItem.link;
        if (feedItem.creator != null && !accounts.containsValue(feedItem.creator)) {
            MastodonTimelinesService mastodonTimelinesService = initInstanceXMLOnly(context, instance);
            Call<NitterAccount> accountCall = mastodonTimelinesService.getNitterAccount(instance);
            if (accountCall != null) {
                try {
                    Response<NitterAccount> publicTlResponse = accountCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        NitterAccount nitterAccount = publicTlResponse.body();
                        accounts.put(feedItem.creator, nitterAccount);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        NitterAccount nitterAccount = accounts.get(feedItem.creator);
        if (nitterAccount != null) {
            app.fedilab.android.client.entities.api.Account account = new app.fedilab.android.client.entities.api.Account();
            String[] names = nitterAccount.channel.image.title.split("/");
            account.id = feedItem.guid;
            account.acct = names[1];
            account.username = names[1];
            account.display_name = names[0];
            account.avatar = nitterAccount.channel.image.url;
            account.avatar_static = nitterAccount.channel.image.url;
            account.url = nitterAccount.channel.image.link;
            status.account = account;
        }

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

        return status;
    }

    @Root(name = "channel", strict = false)
    public static class Channel implements Serializable {
        @ElementList(name = "item")
        public List<FeedItem> mFeedItems;
    }

    @Root(name = "item", strict = false)
    public static class FeedItem implements Serializable {
        @ElementList(name = "dc:creator", required = false)
        public String creator;
        @ElementList(name = "title")
        public String title;
        @ElementList(name = "description", required = false)
        public String description;
        @ElementList(name = "pubDate")
        public Date pubDate;
        @ElementList(name = "guid", required = false)
        public String guid;
        @ElementList(name = "link", required = false)
        public String link;
    }

}
