package cn.zhangjh.zhiyue;

import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.Toast;

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

import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.model.SearchResponse;
import cn.zhangjh.zhiyue.pojo.Book;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BooksFragment extends Fragment implements BookAdapter.OnBookClickListener {
    private static final String TAG = "BooksFragment";
    private static final int DEFAULT_PAGE_SIZE = 5;

    private TextInputEditText centerSearchEditText;
    private TextInputEditText topSearchEditText;
    private RecyclerView recyclerView;
    private ConstraintLayout searchResultContainer;
    private ProgressBar progressBar;
    private View centerSearchContainer;
    private View topSearchContainer;
    private BookAdapter bookAdapter;
    private String lastSearchQuery = "";
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

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

        view.findViewById(R.id.centerSearchButton).setOnClickListener(v -> performNewSearch(centerSearchEditText));
        view.findViewById(R.id.topSearchButton).setOnClickListener(v -> performNewSearch(topSearchEditText));
    }

    private void setupRecyclerView() {
        bookAdapter = new BookAdapter();
        bookAdapter.setOnBookClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(bookAdapter);

        // 添加滚动监听以实现加载更多
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (!hasMoreData || isLoading) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0) {
                    loadMoreBooks();
                }
            }
        });
    }

    private void setupSearchViews() {
        // 设置中心搜索框的动作
        centerSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performNewSearch(centerSearchEditText);
                return true;
            }
            return false;
        });

        // 设置顶部搜索框的动作
        topSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performNewSearch(topSearchEditText);
                return true;
            }
            return false;
        });

        // 同步两个搜索框的文本
        centerSearchEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!Objects.requireNonNull(topSearchEditText.getText()).toString().equals(s.toString())) {
                    topSearchEditText.setText(s.toString());
                }
            }
        });

        topSearchEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!Objects.requireNonNull(centerSearchEditText.getText()).toString().equals(s.toString())) {
                    centerSearchEditText.setText(s.toString());
                }
            }
        });
    }

    private void performNewSearch(TextInputEditText searchEditText) {
        String query = Objects.requireNonNull(searchEditText.getText()).toString().trim();
        if (query.isEmpty()) {
            return;
        }

        // 重置分页状态
        currentPage = 1;
        hasMoreData = true;
        lastSearchQuery = query;
        bookAdapter.clearBooks();
        
        showLoading();
        performSearch(query, currentPage);
    }

    private void loadMoreBooks() {
        if (!isLoading && hasMoreData) {
            performSearch(lastSearchQuery, ++currentPage);
        }
    }

    private void performSearch(String query, int page) {
        isLoading = true;
        Log.d(TAG, "Performing search: query=" + query + ", page=" + page + ", limit=" + BooksFragment.DEFAULT_PAGE_SIZE);
        
        ApiClient.getBookService().searchBooks(query, page, BooksFragment.DEFAULT_PAGE_SIZE).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (!isAdded()) return;
                isLoading = false;

                Log.d(TAG, "Search response received. Code: " + response.code());
                if (!response.isSuccessful()) {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Error response: " + errorBody);
                        showError("搜索失败 (" + response.code() + "): " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error response", e);
                        showError("搜索失败 (" + response.code() + ")");
                    }
                    if (currentPage == 1) {
                        showResults(new ArrayList<>());
                    }
                    return;
                }

                if (response.body() != null && response.body().isSuccess()) {
                    List<Book> books = convertToBooks(response.body().getData());
                    Log.d(TAG, "Search successful. Found " + books.size() + " books");
                    
                    // 如果返回的数据少于请求的数量，说明没有更多数据了
                    hasMoreData = books.size() >= BooksFragment.DEFAULT_PAGE_SIZE;
                    
                    if (currentPage == 1) {
                        showResults(books);
                    } else {
                        bookAdapter.addBooks(books);
                    }
                } else {
                    Log.e(TAG, "Response body was null or not successful");
                    showError("搜索失败：返回数据无效");
                    if (currentPage == 1) {
                        showResults(new ArrayList<>());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                isLoading = false;
                
                Log.e(TAG, "Search failed", t);
                String errorMessage = "网络错误: " + t.getClass().getSimpleName();
                if (t.getMessage() != null) {
                    errorMessage += " - " + t.getMessage();
                }
                showError(errorMessage);
                if (currentPage == 1) {
                    showResults(new ArrayList<>());
                }
            }
        });
    }

    private List<Book> convertToBooks(List<SearchResponse.BookDetail> bookDetails) {
        List<Book> books = new ArrayList<>();
        for (SearchResponse.BookDetail detail : bookDetails) {
            books.add(new Book(
                    detail.getId(),
                    detail.getTitle(),
                    detail.getAuthor(),
                    detail.getFilesizeString(),
                    detail.getExtension(),
                    detail.getCover(),
                    detail.getDescription()
            ));
        }
        return books;
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading() {
        if (currentPage == 1) {
            progressBar.setVisibility(View.VISIBLE);
            searchResultContainer.setVisibility(View.GONE);
            centerSearchContainer.setVisibility(View.GONE);
            topSearchContainer.setVisibility(View.VISIBLE);
        }
    }

    private void showResults(List<Book> books) {
        progressBar.setVisibility(View.GONE);
        if (books.isEmpty() && currentPage == 1) {
            // 如果是第一页且没有结果，显示中心搜索框
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