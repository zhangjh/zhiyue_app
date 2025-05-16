package cn.zhangjh.zhiyue.model;

import com.google.gson.annotations.SerializedName;

public class ReadingRecord {

    private String userId;
    private String fileId;
    private String hashId;
    private String title;
    private String author;
    private int progress;
    private String cfi;
    private String summary;
    @SerializedName("contentSummary")
    private String partsSummary;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getHashId() {
        return hashId;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getCfi() {
        return cfi;
    }

    public void setCfi(String cfi) {
        this.cfi = cfi;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPartsSummary() {
        return partsSummary;
    }

    public void setPartsSummary(String partsSummary) {
        this.partsSummary = partsSummary;
    }
}
