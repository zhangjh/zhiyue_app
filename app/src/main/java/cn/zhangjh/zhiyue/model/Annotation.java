package cn.zhangjh.zhiyue.model;

public class Annotation {
    private String id;
    private String userId;
    private String fileId;
    private String cfi;
    private String type;    // highlight 或 underline
    private String color;
    private String text;

    public Annotation(String fileId, String cfi, String type, String color, String text) {
        this.fileId = fileId;
        this.cfi = cfi;
        this.type = type;
        this.color = color;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getCfi() {
        return cfi;
    }

    public void setCfi(String cfi) {
        this.cfi = cfi;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}