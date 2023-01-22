package app.fedilab.android.peertube.client.entities;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class File implements Parcelable {

    public static final Creator<File> CREATOR = new Creator<File>() {
        @Override
        public File createFromParcel(Parcel source) {
            return new File(source);
        }

        @Override
        public File[] newArray(int size) {
            return new File[size];
        }
    };
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

    protected File(Parcel in) {
        this.fileDownloadUrl = in.readString();
        this.fileUrl = in.readString();
        this.fps = in.readInt();
        this.magnetUri = in.readString();
        this.metadataUrl = in.readString();
        this.resolutions = in.readParcelable(Item.class.getClassLoader());
        this.size = in.readLong();
        this.torrentDownloadUrl = in.readString();
        this.torrentUrl = in.readString();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fileDownloadUrl);
        dest.writeString(this.fileUrl);
        dest.writeInt(this.fps);
        dest.writeString(this.magnetUri);
        dest.writeString(this.metadataUrl);
        dest.writeParcelable(this.resolutions, flags);
        dest.writeLong(this.size);
        dest.writeString(this.torrentDownloadUrl);
        dest.writeString(this.torrentUrl);
    }
}
