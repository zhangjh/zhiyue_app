package cn.zhangjh.zhiyue.model;

public class Annotation {
    private String bookId;
    private String cfiRange;
    private String type;    // highlight 或 underline
    private String color;
    private String text;
    private long timestamp;
    
    public Annotation(String bookId, String cfiRange, String type, String color, String text) {
        this.bookId = bookId;
        this.cfiRange = cfiRange;
        this.type = type;
        this.color = color;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getCfiRange() {
        return cfiRange;
    }

    public void setCfiRange(String cfiRange) {
        this.cfiRange = cfiRange;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


}