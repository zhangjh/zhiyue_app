package cn.zhangjh.zhiyue.model;

public class Book {
    private String id;
    private String hash;
    private String title;
    private String author;
    private String size;
    private String format;
    private String coverUrl;
    private String description;

    public Book(BookDetail bookDetail) {
        this.id = bookDetail.getId();
        this.title = bookDetail.getTitle();
        this.author = bookDetail.getAuthor();
        this.size = bookDetail.getFilesizeString();
        this.format = bookDetail.getExtension();
        this.coverUrl = bookDetail.getCover();
        this.description = bookDetail.getDescription();
        this.hash = bookDetail.getHash();
    }

    // Getters
    public String getId() { return id; }
    public String getHash() { return hash; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getSize() { return size; }
    public String getFormat() { return format; }
    public String getCoverUrl() { return coverUrl; }
    public String getDescription() { return description; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setHash(String hash) { this.hash = hash; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setSize(String size) { this.size = size; }
    public void setFormat(String format) { this.format = format; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setDescription(String description) { this.description = description; }
}