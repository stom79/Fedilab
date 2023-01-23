package app.fedilab.android.peertube.client.entities;


import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class File implements Serializable {

    @SerializedName("fileDownloadUrl")
    private String fileDownloadUrl;
    @SerializedName("fileUrl")
    private String fileUrl;
    @SerializedName("fps")
    private int fps;
    @SerializedName("magnetUri")
    private String magnetUri;
    @SerializedName("metadataUrl")
    private String metadataUrl;
    @SerializedName("resolution")
    private Item resolutions;
    @SerializedName("size")
    private long size;
    @SerializedName("torrentDownloadUrl")
    private String torrentDownloadUrl;
    @SerializedName("torrentUrl")
    private String torrentUrl;

    public File() {
    }

    public String getFileDownloadUrl() {
        return fileDownloadUrl;
    }

    public void setFileDownloadUrl(String fileDownloadUrl) {
        this.fileDownloadUrl = fileDownloadUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public String getMagnetUri() {
        return magnetUri;
    }

    public void setMagnetUri(String magnetUri) {
        this.magnetUri = magnetUri;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public Item getResolutions() {
        return resolutions;
    }

    public void setResolutions(Item resolutions) {
        this.resolutions = resolutions;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getTorrentDownloadUrl() {
        return torrentDownloadUrl;
    }

    public void setTorrentDownloadUrl(String torrentDownloadUrl) {
        this.torrentDownloadUrl = torrentDownloadUrl;
    }

    public String getTorrentUrl() {
        return torrentUrl;
    }

    public void setTorrentUrl(String torrentUrl) {
        this.torrentUrl = torrentUrl;
    }

}
