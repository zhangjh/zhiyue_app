package cn.zhangjh.zhiyue.model;

import com.google.gson.annotations.SerializedName;

public class ReadingHistory {
    @SerializedName("id")
    private String id;

    @SerializedName("create_time")
    private String startTime;

    @SerializedName("modify_time")
    private String lastReadTime;

    @SerializedName("title")
    private String bookTitle;

    @SerializedName("author")
    private String bookAuthor;

    @SerializedName("cover")
    private String coverUrl;

    @SerializedName("progress")
    private int progress;

    @SerializedName("file_id")
    private String hash;

    @SerializedName("summary")
    private String summary;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(String lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}