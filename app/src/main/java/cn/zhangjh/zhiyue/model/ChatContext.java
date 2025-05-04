package cn.zhangjh.zhiyue.model;

public class ChatContext {
    private final String role;
    private final String content;

    public ChatContext(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}