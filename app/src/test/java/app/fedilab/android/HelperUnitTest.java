package app.fedilab.android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.regex.Matcher;

import app.fedilab.android.mastodon.helper.Helper;

public class HelperUnitTest {

    @Test
    public void tests_wwwTwitter_pattern() {
        String twitterUrl = "https://www.twitter.com/foo";
        String result = matchTwitter(twitterUrl);
        assertEquals("https://nitter.net/foo", result);
    }

    public void tests_Twitter_pattern() {
        String twitterUrl = "https://twitter.com/foo";
        String result = matchTwitter(twitterUrl);
        assertEquals("https://nitter.net/foo", result);
    }

    @Test
    public void tests_wwwxcom_pattern() {
        String twitterUrl = "https://www.x.com/foo";
        String result = matchTwitter(twitterUrl);
        assertEquals("https://nitter.net/foo", result);
    }

    @Test
    public void tests_xcom_pattern() {
        String twitterUrl = "https://x.com/foo";
        String result = matchTwitter(twitterUrl);
        assertEquals("https://nitter.net/foo", result);
    }


    @Test
    public void tests_wwwwixdotcom_pattern() {
        String twitterUrl = "https://www.wix.com/foo";
        String result = matchTwitter(twitterUrl);
        assertEquals("https://www.wix.com/foo", result);
    }

    @Test
    public void tests_wixdotcom_pattern() {
        String twitterUrl = "https://wix.com/foo";
        String result = matchTwitter(twitterUrl);
        assertEquals("https://wix.com/foo", result);
    }

    private String matchTwitter(String url){
        Matcher matcher = Helper.nitterPattern.matcher(url);
        if (matcher.find()) {
            final String nitter_directory = matcher.group(3);
            String nitterHost =  "nitter.net";


            return "https://" + nitterHost + nitter_directory;
        }
        return url;
    }

}