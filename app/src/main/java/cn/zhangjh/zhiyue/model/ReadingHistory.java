package cn.zhangjh.zhiyue.model;

public class ReadingHistory {
    private String bookId;
    private String bookTitle;
    private String bookAuthor;
    private String startTime;
    private String lastReadTime;
    private int progress;
    private String hash;

    // Getters and Setters
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    
    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getLastReadTime() { return lastReadTime; }
    public void setLastReadTime(String lastReadTime) { this.lastReadTime = lastReadTime; }
    
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}