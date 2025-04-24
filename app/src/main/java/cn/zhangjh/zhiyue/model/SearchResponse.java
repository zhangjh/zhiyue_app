package cn.zhangjh.zhiyue.model;

import java.util.List;

public class SearchResponse {
    private boolean success;
    private List<BookDetail> data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<BookDetail> getData() {
        return data;
    }

    public void setData(List<BookDetail> data) {
        this.data = data;
    }

    public static class BookDetail {
        private String id;
        private String title;
        private String author;
        private String cover;
        private String filesize;
        private String filesizeString;
        private String extension;
        private String description;
        private String hash;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        public String getCover() {
            return cover;
        }

        public void setCover(String cover) {
            this.cover = cover;
        }

        public String getFilesize() {
            return filesize;
        }

        public void setFilesize(String filesize) {
            this.filesize = filesize;
        }

        public String getFilesizeString() {
            return filesizeString;
        }

        public void setFilesizeString(String filesizeString) {
            this.filesizeString = filesizeString;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String url) {
            this.hash = hash;
        }
    }
} 