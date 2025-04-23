package cn.zhangjh.zhiyue;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.zhangjh.zhiyue.pojo.Book;

public class BooksFragment extends Fragment implements BookAdapter.OnBookClickListener {
    private TextInputEditText centerSearchEditText;
    private TextInputEditText topSearchEditText;
    private RecyclerView recyclerView;
    private ConstraintLayout searchResultContainer;
    private ProgressBar progressBar;
    private View centerSearchContainer;
    private View topSearchContainer;
    private BookAdapter bookAdapter;

	@Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupSearchViews();
    }

    private void initViews(View view) {
        centerSearchEditText = view.findViewById(R.id.centerSearchEditText);
        topSearchEditText = view.findViewById(R.id.topSearchEditText);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        centerSearchContainer = view.findViewById(R.id.centerSearchContainer);
        topSearchContainer = view.findViewById(R.id.topSearchContainer);
        searchResultContainer = view.findViewById(R.id.searchResultContainer);

        view.findViewById(R.id.centerSearchButton).setOnClickListener(v -> performSearch(centerSearchEditText));
        view.findViewById(R.id.topSearchButton).setOnClickListener(v -> performSearch(topSearchEditText));
    }

    private void setupRecyclerView() {
        bookAdapter = new BookAdapter();
        bookAdapter.setOnBookClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(bookAdapter);
    }

    private void setupSearchViews() {
        // 设置中心搜索框的动作
        centerSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(centerSearchEditText);
                return true;
            }
            return false;
        });

        // 设置顶部搜索框的动作
        topSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(topSearchEditText);
                return true;
            }
            return false;
        });

        // 同步两个搜索框的文本
        centerSearchEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!topSearchEditText.getText().toString().equals(s.toString())) {
                    topSearchEditText.setText(s.toString());
                }
            }
        });

        topSearchEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!centerSearchEditText.getText().toString().equals(s.toString())) {
                    centerSearchEditText.setText(s.toString());
                }
            }
        });
    }

    private void performSearch(TextInputEditText searchEditText) {
        String query = Objects.requireNonNull(searchEditText.getText()).toString().trim();
        if (query.isEmpty()) {
            return;
        }

	    showLoading();
        // TODO: Implement actual search logic here
        // For now, we'll just simulate a search with dummy data
        simulateSearch();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        searchResultContainer.setVisibility(View.GONE);
        centerSearchContainer.setVisibility(View.GONE);
        topSearchContainer.setVisibility(View.VISIBLE);
    }

    private void showResults(List<Book> books) {
        progressBar.setVisibility(View.GONE);
        if (books.isEmpty()) {
            // 如果没有结果，显示中心搜索框
            searchResultContainer.setVisibility(View.GONE);
            centerSearchContainer.setVisibility(View.VISIBLE);
            topSearchContainer.setVisibility(View.GONE);
        } else {
            // 如果有结果，显示顶部搜索框和结果列表
            centerSearchContainer.setVisibility(View.GONE);
            topSearchContainer.setVisibility(View.VISIBLE);
            searchResultContainer.setVisibility(View.VISIBLE);
            bookAdapter.setBooks(books);
        }
    }

    // Temporary method to simulate search results
    private void simulateSearch() {
        // Simulate network delay
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                if (getActivity() == null) return;

                // Create dummy data
                List<Book> dummyBooks = new ArrayList<>();
                dummyBooks.add(new Book("1", "Android开发艺术探索", "任玉刚", "23.5MB", "PDF",
                        "https://example.com/cover1.jpg", "本书是一本Android进阶类书籍，采用理论、源码和实践相结合的方式来阐述高水准的Android应用开发要点。"));
                dummyBooks.add(new Book("2", "深入理解Java虚拟机", "周志明", "18.2MB", "EPUB",
                        "https://example.com/cover2.jpg", "本书第3版在第2版的基础上做了重大更新，内容更丰富、实战性更强。"));
                dummyBooks.add(new Book("3", "Android开发艺术探索", "任玉刚", "23.5MB", "PDF",
                        "https://example.com/cover1.jpg", "本书是一本Android进阶类书籍，采用理论、源码和实践相结合的方式来阐述高水准的Android应用开发要点。"));
                dummyBooks.add(new Book("4", "深入理解Java虚拟机", "周志明", "18.2MB", "EPUB",
                        "https://example.com/cover2.jpg", "本书第3版在第2版的基础上做了重大更新，内容更丰富、实战性更强。"));
                dummyBooks.add(new Book("5", "Android开发艺术探索", "任玉刚", "23.5MB", "PDF",
                        "https://example.com/cover1.jpg", "本书是一本Android进阶类书籍，采用理论、源码和实践相结合的方式来阐述高水准的Android应用开发要点。"));

                getActivity().runOnUiThread(() -> showResults(dummyBooks));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onReadButtonClick(Book book) {
        // TODO: Implement navigation to reader activity
        // For now, just print to console
        System.out.println("Navigate to reader for book: " + book.getTitle());
    }

    // 简单的TextWatcher实现，只需要afterTextChanged方法
    private static abstract class SimpleTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}