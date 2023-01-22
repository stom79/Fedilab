package app.fedilab.android.mastodon.interfaces;

public interface ProgressListener {
    void onProgress(long transferred, long total);
}