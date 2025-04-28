package cn.zhangjh.zhiyue.model;

import java.util.List;

public class BizListResponse<T> {
    private boolean success;
    private List<T> data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
} 