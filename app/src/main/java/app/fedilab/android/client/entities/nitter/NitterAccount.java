package app.fedilab.android.client.entities.nitter;


import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.Serializable;
import java.util.HashMap;

@Root(name = "rss", strict = false)
public class NitterAccount implements Serializable {


    public static HashMap<String, NitterAccount> accounts = new HashMap<>();
    @Element(name = "channel")
    public Channel channel;

    @Root(name = "channel", strict = false)
    public static class Channel implements Serializable {
        @Element(name = "image")
        public Image image;

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


}
