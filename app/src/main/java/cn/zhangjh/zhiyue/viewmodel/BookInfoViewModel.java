package cn.zhangjh.zhiyue.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BookInfoViewModel extends ViewModel {
    private final MutableLiveData<String> title = new MutableLiveData<>();
    private final MutableLiveData<String> author = new MutableLiveData<>();
    // 分片总结过的内容，用来整理脑图使用
    private final MutableLiveData<String> partsSummary = new MutableLiveData<>();
    // 全书总结，用来AI总结和聊天使用
    private final MutableLiveData<String> summary = new MutableLiveData<>();

    public void setBookInfo(String title, String author, String summary, String partsSummary) {
        this.title.setValue(title);
        this.author.setValue(author);
        this.summary.setValue(summary);
        this.partsSummary.setValue(partsSummary);
    }

    public LiveData<String> getTitle() {
        return title;
    }

    public LiveData<String> getAuthor() {
        return author;
    }

    public LiveData<String> getSummary() {
        return summary;
    }

    public MutableLiveData<String> getPartsSummary() {
        return partsSummary;
    }

    public void clearBookInfo() {
        title.setValue("");
        author.setValue("");
        summary.setValue("");
        partsSummary.setValue("");
    }
}