package cn.zhangjh.zhiyue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
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
    private TextInputEditText searchEditText;
    private RecyclerView recyclerView;
    private ConstraintLayout searchResultContainer;
    private ProgressBar progressBar;

    private LinearLayout searchContainer;
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
        setupSearchView();
    }

    private void initViews(View view) {
        searchEditText = view.findViewById(R.id.searchEditText);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        searchContainer = view.findViewById(R.id.searchContainer);
        searchResultContainer = view.findViewById(R.id.searchResultContainer);
        View searchBtn = view.findViewById(R.id.searchButton);
        searchBtn.setOnClickListener(v -> performSearch());
    }

    private void setupRecyclerView() {
        bookAdapter = new BookAdapter();
        bookAdapter.setOnBookClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(bookAdapter);
    }

    private void setupSearchView() {
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
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
    }

    private void showResults(List<Book> books) {
        progressBar.setVisibility(View.GONE);
        if (books.isEmpty()) {
            searchResultContainer.setVisibility(View.GONE);
            searchContainer.setVisibility(View.VISIBLE);
        } else {
            searchContainer.setVisibility(View.GONE);
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
                dummyBooks.add(new Book("1", "Android开发艺术探索", "任玉刚", "23.5MB", "PDF",
                        "https://example.com/cover1.jpg", "本书是一本Android进阶类书籍，采用理论、源码和实践相结合的方式来阐述高水准的Android应用开发要点。"));
                dummyBooks.add(new Book("2", "深入理解Java虚拟机", "周志明", "18.2MB", "EPUB",
                        "https://example.com/cover2.jpg", "本书第3版在第2版的基础上做了重大更新，内容更丰富、实战性更强。"));
                dummyBooks.add(new Book("1", "Android开发艺术探索", "任玉刚", "23.5MB", "PDF",
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
}