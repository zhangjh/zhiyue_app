package cn.zhangjh.zhiyue.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.util.CollectionUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.adapter.BookAdapter;
import cn.zhangjh.zhiyue.adapter.RecommendBookAdapter;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.billing.SubscriptionManager;
import cn.zhangjh.zhiyue.model.BizListResponse;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.Book;
import cn.zhangjh.zhiyue.model.BookDetail;
import cn.zhangjh.zhiyue.model.HistoryResponse;
import cn.zhangjh.zhiyue.model.ReadingHistory;
import cn.zhangjh.zhiyue.utils.BizUtils;
import cn.zhangjh.zhiyue.utils.LogUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BooksFragment extends Fragment implements BookAdapter.OnBookClickListener, RecommendBookAdapter.OnBookClickListener {
    private static final String TAG = "BooksFragment";
    private static final int DEFAULT_PAGE_SIZE = 5;

    private String currentUserId;
    private SubscriptionManager subscriptionManager;
    private TextInputEditText centerSearchEditText;
    private TextInputEditText topSearchEditText;
    private RecyclerView recyclerView;
    private RecyclerView recommendRecyclerView;
    private ProgressBar recommendProgressBar;
    private ImageButton refreshRecommendButton;
    private ConstraintLayout searchResultContainer;
    private ProgressBar progressBar;
    private View centerSearchContainer;
    private View topSearchContainer;
    private BookAdapter bookAdapter;
    private RecommendBookAdapter recommendBookAdapter;
    private String lastSearchQuery = "";
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private View emptyView;

    private int recommendCurrentPage;
    // 阅读记录bookId缓存
    private static Set<String> cachedBookIds = new HashSet<>();
    // 推荐书目缓存
    private static List<Book> cacheRecommendBooks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 获取用户ID
        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        // 获取阅读记录缓存
        String cachedBookIdsString = BizUtils.getCache(requireContext(), "reader", "cachedBookIds");
        if(!TextUtils.isEmpty(cachedBookIdsString)) {
            Type listType = new TypeToken<Set<String>>() {}.getType();
            cachedBookIds = new Gson().fromJson(cachedBookIdsString, listType);
        }

        initViews(view);
        setupRecyclerView();
        setupSearchViews();
        setupRecommendBooks();

        subscriptionManager = SubscriptionManager.getInstance(requireActivity());
        
        // 添加返回键处理
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (searchResultContainer.getVisibility() == View.VISIBLE) {
                        // 如果在搜索结果页，返回到默认搜索页
                        resetToDefaultSearchView();
                    } else {
                        // 如果在默认搜索页，移除回调并允许正常的返回行为
                        setEnabled(false);
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            });
    }

    // 添加重置视图的方法
    private void resetToDefaultSearchView() {
        searchResultContainer.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        centerSearchContainer.setVisibility(View.VISIBLE);
        topSearchContainer.setVisibility(View.GONE);
        
        // 清空搜索框
        centerSearchEditText.setText("");
        topSearchEditText.setText("");
        
        // 清空搜索结果
        bookAdapter.clearBooks();
        
        // 重置分页状态
        currentPage = 1;
        hasMoreData = true;
        lastSearchQuery = "";

        // 推荐分页
        recommendCurrentPage = 1;
    }

    private void initViews(View view) {
        centerSearchEditText = view.findViewById(R.id.centerSearchEditText);
        topSearchEditText = view.findViewById(R.id.topSearchEditText);
        recyclerView = view.findViewById(R.id.recyclerView);
        recommendRecyclerView = view.findViewById(R.id.recommendRecyclerView);
        recommendProgressBar = view.findViewById(R.id.recommendProgressBar);
        refreshRecommendButton = view.findViewById(R.id.refreshRecommendButton);
        progressBar = view.findViewById(R.id.progressBar);
        centerSearchContainer = view.findViewById(R.id.centerSearchContainer);
        topSearchContainer = view.findViewById(R.id.topSearchContainer);
        searchResultContainer = view.findViewById(R.id.searchResultContainer);
        emptyView = view.findViewById(R.id.emptyView);
    }

    private void setupRecyclerView() {
        // 设置搜索结果列表
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
        
        // 设置推荐书目列表为网格布局，两列
        recommendBookAdapter = new RecommendBookAdapter();
        recommendBookAdapter.setOnBookClickListener(this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        recommendRecyclerView.setLayoutManager(gridLayoutManager);
        recommendRecyclerView.setAdapter(recommendBookAdapter);
        
        // 设置推荐书目列表的高度，使其只显示2.5行
        ViewGroup.LayoutParams params = recommendRecyclerView.getLayoutParams();
        if (params != null) {
            // 计算高度：每个项目的高度约为150dp，显示2.5行
            float density = getResources().getDisplayMetrics().density;
            int itemHeight = (int) (150 * density);
            params.height = (int) (2.5f * itemHeight);
            recommendRecyclerView.setLayoutParams(params);
        }
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

    private void setupRecommendBooks() {
        recommendCurrentPage = 1;

        // 设置刷新按钮点击事件
        refreshRecommendButton.setOnClickListener(v -> {
            recommendCurrentPage++;
            loadRecommendBooks();
        });
        
        // 加载推荐书目
        loadRecommendBooks();
    }

    private void loadRecommendBooks() {
        // 将进度条显示在推荐书目列表的位置，而不是在底部
        recommendProgressBar.setVisibility(View.VISIBLE);
        
        // 确保推荐书目列表在加载时不可见，避免空白区域
        recommendRecyclerView.setVisibility(View.GONE);
        
        // 如果是第一页并且没有缓存的bookId，需要先获取阅读历史
        if (recommendCurrentPage == 1) {
            // 直接展示缓存书目，大部分场景下不会翻页
            if(!cacheRecommendBooks.isEmpty()) {
                showRecommendView(cacheRecommendBooks);
            }
            if(cachedBookIds.isEmpty()) {
                ApiClient.getBookService().getHistory(1, 10, currentUserId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<BizResponse<HistoryResponse>> call, @NonNull Response<BizResponse<HistoryResponse>> response) {
                        if (!isAdded()) return;

                        if (!response.isSuccessful()
                                || response.body() == null
                                || !Objects.requireNonNull(response.body()).isSuccess()) {
                            showNoHistoryView();
                            return;
                        }

                        HistoryResponse historyResponse = Objects.requireNonNull(response.body()).getData();
                        List<ReadingHistory> histories = historyResponse != null ? historyResponse.getResults() : new ArrayList<>();

                        if (histories.isEmpty()) {
                            showNoHistoryView();
                            return;
                        }

                        // 缓存书籍ID用于分页
                        cachedBookIds = histories.stream()
                                .map(ReadingHistory::getBookId)
                                .filter(item -> !TextUtils.isEmpty(item))
                                .collect(Collectors.toSet());
                        // 保存缓存
                        BizUtils.saveCache(requireActivity(), "reader", "cachedBookIds", new Gson().toJson(cachedBookIds));

                        // 获取第一页推荐
                        fetchRecommendBooks();
                    }

                    @Override
                    public void onFailure(@NonNull Call<BizResponse<HistoryResponse>> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        showNoHistoryView();
                    }
                });
            } else {
                fetchRecommendBooks();
            }
        } else {
            // 直接获取下一页推荐
            fetchRecommendBooks();
        }
    }

    private void fetchRecommendBooks() {
        ApiClient.getBookService().getRecommendBooks(cachedBookIds, recommendCurrentPage)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<BizListResponse<BookDetail>> call, @NonNull Response<BizListResponse<BookDetail>> response) {
                        if (!isAdded()) return;

                        if (!response.isSuccessful()) {
                            handleRecommendError(response);
                            return;
                        }
                        
                        if (response.body() != null && Objects.requireNonNull(response.body()).isSuccess()) {
                            List<Book> books = convertToBooks(Objects.requireNonNull(response.body()).getData());
                            LogUtil.d(TAG, "Recommend successful. Found " + books.size() + " books");
                            // 只缓存第一页
                            if(CollectionUtils.isEmpty(books)) {
                                showNoHistoryView();
                                return;
                            }
                            if(cacheRecommendBooks.isEmpty()) {
                                cacheRecommendBooks = books;
                            }
                            showRecommendView(books);
                        } else {
                            LogUtil.e(TAG, "Response body was null or not successful");
                            showError(getString(R.string.recommend_failed));
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<BizListResponse<BookDetail>> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        recommendProgressBar.setVisibility(View.GONE);
                        recommendRecyclerView.setVisibility(View.VISIBLE);

                        LogUtil.e(TAG, "Recommend failed", t);
                        String errorMessage = getString(R.string.error_network) + ":" + t.getClass().getSimpleName();
                        if (t.getMessage() != null) {
                            errorMessage += " - " + t.getMessage();
                        }
                        showError(errorMessage);
                    }
                });
    }

    private void handleRecommendError(Response<BizListResponse<BookDetail>> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            LogUtil.e(TAG, "Error response: " + errorBody);
            showError(getString(R.string.recommend_failed) + " (" + response.code() + "): " + errorBody);
        } catch (Exception e) {
            LogUtil.e(TAG, "Error reading error response", e);
            showError(getString(R.string.recommend_failed) + " (" + response.code() + ")");
        }
    }

    private void showNoHistoryView() {
        recommendProgressBar.setVisibility(View.GONE);
        recommendRecyclerView.setVisibility(View.VISIBLE);
        // 显示提示信息
        TextView noHistoryText = new TextView(requireContext());
        noHistoryText.setText(getString(R.string.reading_first_recommend));
        noHistoryText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ((ViewGroup) recommendRecyclerView.getParent()).addView(noHistoryText);
        recommendRecyclerView.setVisibility(View.GONE);
    }

    private void showRecommendView(List<Book> books) {
        recommendProgressBar.setVisibility(View.GONE);
        recommendRecyclerView.setVisibility(View.VISIBLE);

        // 重置推荐书目容器
        ViewGroup parent = (ViewGroup) recommendRecyclerView.getParent();
        parent.removeAllViews();
        parent.addView(recommendRecyclerView);
        parent.addView(recommendProgressBar);

        // 设置新数据
        recommendBookAdapter.setBooks(books);

        // 确保 RecyclerView 显示在顶部
        recommendRecyclerView.scrollToPosition(0);
    }

    private void performNewSearch(TextInputEditText searchEditText) {
        String keyword = Objects.requireNonNull(searchEditText.getText()).toString().trim();
        if (keyword.isEmpty()) {
            return;
        }

        // 隐藏输入法
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
    
        // 重置分页状态
        currentPage = 1;
        hasMoreData = true;
        lastSearchQuery = keyword;
        bookAdapter.clearBooks();
        
        // 重置视图状态
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        showLoading();
        performSearch(keyword, currentPage);
    }

    private void loadMoreBooks() {
        if (!isLoading && hasMoreData) {
            performSearch(lastSearchQuery, ++currentPage);
        }
    }

    private void performSearch(String keyword, int page) {
        isLoading = true;
        LogUtil.d(TAG, "Performing search: keyword=" + keyword + ", page=" + page + ", limit=" + BooksFragment.DEFAULT_PAGE_SIZE);

        ApiClient.getBookService().searchBooks(keyword, "epub", page, BooksFragment.DEFAULT_PAGE_SIZE).enqueue(new Callback<>() {
	        @Override
	        public void onResponse(@NonNull Call<BizListResponse<BookDetail>> call, @NonNull Response<BizListResponse<BookDetail>> response) {
		        if (!isAdded()) return;
		        isLoading = false;

		        LogUtil.d(TAG, "Search response received. Code: " + response.code());
		        if (!response.isSuccessful()) {
			        try {
				        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
				        LogUtil.e(TAG, "Error response: " + errorBody);
				        showError(getString(R.string.search_failed) + " (" + response.code() + "): " + errorBody);
			        } catch (Exception e) {
				        LogUtil.e(TAG, "Error reading error response", e);
				        showError(getString(R.string.search_failed) + " (" + response.code() + ")");
			        }
			        if (currentPage == 1) {
				        showResults(new ArrayList<>());
			        }
			        return;
		        }

		        if (response.body() != null && response.body().isSuccess()) {
			        List<Book> books = convertToBooks(response.body().getData());
			        LogUtil.d(TAG, "Search successful. Found " + books.size() + " books");

			        // 如果返回的数据少于请求的数量，说明没有更多数据了
			        hasMoreData = books.size() >= BooksFragment.DEFAULT_PAGE_SIZE;

			        if (currentPage == 1) {
				        showResults(books);
			        } else {
				        bookAdapter.addBooks(books);
			        }
		        } else {
			        LogUtil.e(TAG, "Response body was null or not successful");
			        showError(getString(R.string.search_failed));
			        if (currentPage == 1) {
				        showResults(new ArrayList<>());
			        }
		        }
	        }

	        @Override
	        public void onFailure(@NonNull Call<BizListResponse<BookDetail>> call, @NonNull Throwable t) {
		        if (!isAdded()) return;
		        isLoading = false;

		        LogUtil.e(TAG, "Search failed", t);
		        String errorMessage = getString(R.string.error_network) + ": " + t.getClass().getSimpleName();
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

    private List<Book> convertToBooks(List<BookDetail> bookDetails) {
        List<Book> books = new ArrayList<>();
        if(bookDetails == null) return books;
        for (BookDetail detail : bookDetails) {
            books.add(new Book(detail));
        }
        return books;
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        if (currentPage == 1) {
            searchResultContainer.setVisibility(View.GONE);
            centerSearchContainer.setVisibility(View.GONE);
            topSearchContainer.setVisibility(View.VISIBLE);
        }
    }

    private void showResults(List<Book> books) {
        progressBar.setVisibility(View.GONE);
        if (books.isEmpty() && currentPage == 1) {
            // 如果是第一页且没有结果，显示空状态视图
            searchResultContainer.setVisibility(View.VISIBLE);
            centerSearchContainer.setVisibility(View.GONE);
            topSearchContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            bookAdapter.clearBooks(); // 清空现有数据
        } else {
            // 如果有结果，显示结果列表
            centerSearchContainer.setVisibility(View.GONE);
            topSearchContainer.setVisibility(View.VISIBLE);
            searchResultContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            bookAdapter.setBooks(books);
        }
    }

    @Override
    public void onReadButtonClick(Book book) {
        // 检查订阅状态
        checkSubscriptionAndReadCount(book);
    }

    @Override
    public void onBookClick(Book book) {
        // 检查订阅状态
        checkSubscriptionAndReadCount(book);
    }

    private void checkSubscriptionAndReadCount(Book book) {
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(requireContext(), getString(R.string.please_login_first), Toast.LENGTH_SHORT).show();
            return;
        }
        // 显示加载进度
        progressBar.setVisibility(View.VISIBLE);
        
        // 1. 首先检查用户是否已订阅
        if (subscriptionManager.isSubscribed()) {
            // 已订阅用户直接阅读
            navigateToReader(book);
            progressBar.setVisibility(View.GONE);
            return;
        }
        
        // 2. 未订阅用户，检查阅读记录数量
        ApiClient.getBookService().getHistory(1, 100, currentUserId)
            .enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BizResponse<HistoryResponse>> call,
                                      @NonNull Response<BizResponse<HistoryResponse>> response) {
                    progressBar.setVisibility(View.GONE);
                    
                    if (!response.isSuccessful()) {
                        Toast.makeText(requireContext(), getString(R.string.get_reading_history_failed), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    BizResponse<HistoryResponse> bizResponse = response.body();
                    if (bizResponse == null || !bizResponse.isSuccess()) {
                        String errorMsg = bizResponse != null ? bizResponse.getErrorMsg() : getString(R.string.unknown_error);
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    HistoryResponse historyResponse = bizResponse.getData();
                    List<ReadingHistory> histories = historyResponse != null ? historyResponse.getResults() : new ArrayList<>();
                    
                    if (histories.isEmpty()) {
                        // 没有阅读记录，可以试用一次
                        Toast.makeText(requireContext(), getString(R.string.first_trial), Toast.LENGTH_SHORT).show();
                        navigateToReader(book);
                    } else {
                        // 需要订阅
                        showSubscriptionDialog(book);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BizResponse<HistoryResponse>> call, @NonNull Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    LogUtil.e(TAG, "Get reading history failed", t);
                }
            });
    }
    
    private void showSubscriptionDialog(Book book) {
        // 创建自定义对话框
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_subscription, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        
        // 获取对话框中的视图
        TextView titleTextView = dialogView.findViewById(R.id.dialogTitle);
        TextView messageTextView = dialogView.findViewById(R.id.dialogMessage);
        Button subscribeButton = dialogView.findViewById(R.id.subscribeButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        
        // 设置对话框内容
        titleTextView.setText(getString(R.string.need_subscription));
        messageTextView.setText(getString(R.string.need_subscription_tips));
        
        // 创建对话框
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // 设置按钮点击事件
        subscribeButton.setOnClickListener(v -> {
            dialog.dismiss();
            // 在当前页面进行订阅
            subscription(book);
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }

    private void subscription(Book book) {
        // 显示加载进度
        ProgressBar progressBar = new ProgressBar(requireContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(progressBar);
        AlertDialog loadingDialog = builder.create();
        loadingDialog.show();
        
        // 使用实际的订阅方法
        subscriptionManager.subscribe(info -> {
            loadingDialog.dismiss();
            if(info != null) {
                Toast.makeText(requireContext(), getString(R.string.subscription_success), Toast.LENGTH_SHORT).show();
                navigateToReader(book);
            }
        });
    }
    
    private void navigateToReader(Book book) {
        if (getActivity() instanceof MainActivity) {
            // 只显示加载进度条，保持其他视图状态不变
            progressBar.setVisibility(View.VISIBLE);
            // 跳转到阅读器后将当前书籍ID保存到缓存中，超过10个淘汰最旧的
            cachedBookIds.add(book.getId());
            if(cachedBookIds.size() > 10) {
                cachedBookIds.remove(0);
            }
            // 持久化到本地
            BizUtils.saveCache(requireActivity(), "reader", "cachedBookIds", new Gson().toJson(cachedBookIds));
            ((MainActivity) getActivity()).navigateToReader(book.getId(), book.getHash(), "", "");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        subscriptionManager.destroy();
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