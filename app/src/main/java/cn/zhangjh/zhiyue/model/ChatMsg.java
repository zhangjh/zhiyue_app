package cn.zhangjh.zhiyue.model;

public class ChatMsg {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;

    private String content;
    private final int type;
    private boolean isComplete;

    public ChatMsg(String content, int type) {
        this.content = content;
        this.type = type;
        this.isComplete = true;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }
}