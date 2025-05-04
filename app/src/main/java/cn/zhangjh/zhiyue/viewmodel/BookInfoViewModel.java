package cn.zhangjh.zhiyue.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BookInfoViewModel extends ViewModel {
    private final MutableLiveData<String> title = new MutableLiveData<>();
    private final MutableLiveData<String> author = new MutableLiveData<>();
    private final MutableLiveData<String> summary = new MutableLiveData<>();

    public void setBookInfo(String title, String author, String summary) {
        this.title.setValue(title);
        this.author.setValue(author);
        this.summary.setValue(summary);
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
}