package cn.zhangjh.zhiyue.pojo;

public class Book {
    private String id;
    private String title;
    private String author;
    private String size;
    private String format;
    private String coverUrl;
    private String description;

    public Book(String id, String title, String author, String size, String format, String coverUrl, String description) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.size = size;
        this.format = format;
        this.coverUrl = coverUrl;
        this.description = description;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getSize() { return size; }
    public String getFormat() { return format; }
    public String getCoverUrl() { return coverUrl; }
    public String getDescription() { return description; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setSize(String size) { this.size = size; }
    public void setFormat(String format) { this.format = format; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setDescription(String description) { this.description = description; }
} 