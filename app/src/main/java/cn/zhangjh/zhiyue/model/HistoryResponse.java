package cn.zhangjh.zhiyue.model;

import java.util.List;

public class HistoryResponse {
    private List<ReadingHistory> results;
    private int total;

    public List<ReadingHistory> getResults() {
        return results;
    }

    public void setResults(List<ReadingHistory> results) {
        this.results = results;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}