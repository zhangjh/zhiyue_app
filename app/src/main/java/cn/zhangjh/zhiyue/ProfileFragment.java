package cn.zhangjh.zhiyue;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.HistoryResponse;
import cn.zhangjh.zhiyue.model.ReadingHistory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment implements ReadingHistoryAdapter.OnHistoryItemClickListener {
    private static final int PAGE_SIZE = 5;
    
    private ShapeableImageView userAvatar;
    private TextView userName;
    private RecyclerView readingHistoryRecyclerView;
    private ReadingHistoryAdapter adapter;
    private ProgressBar loadingProgressBar;
    private View emptyView;
    
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private int currentPage = 1;

    private static final String TAG = ProfileFragment.class.getName();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        loadUserInfo();
        loadReadingHistory(1);
    }

    private void initViews(View view) {
        userAvatar = view.findViewById(R.id.userAvatar);
        userName = view.findViewById(R.id.userName);
        readingHistoryRecyclerView = view.findViewById(R.id.readingHistoryRecyclerView);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyView = view.findViewById(R.id.emptyView);
    }

    private void setupRecyclerView() {
        adapter = new ReadingHistoryAdapter();
        adapter.setOnHistoryItemClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        readingHistoryRecyclerView.setLayoutManager(layoutManager);
        readingHistoryRecyclerView.setAdapter(adapter);

        // 添加滚动监听以实现加载更多
        readingHistoryRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (!hasMoreData || isLoading) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0) {
                    loadReadingHistory(currentPage + 1);
                }
            }
        });
    }

    private void loadUserInfo() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", MODE_PRIVATE);
        // 获取用户信息
        userName.setText(prefs.getString("name", "未登录"));

        // 获取并加载用户头像
        String avatarUrl = prefs.getString("avatar", "");
        if (!TextUtils.isEmpty(avatarUrl)) {
            // 使用Glide加载网络图片
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .circleCrop()
                .into(userAvatar);
        } else {
            userAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    private void loadReadingHistory(int page) {
        if (page == 1) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            readingHistoryRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }
        isLoading = true;
        currentPage = page;
        
        // 获取用户ID
        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        if (TextUtils.isEmpty(userId)) {
            // todo: 跳转登录
            loadingProgressBar.setVisibility(View.GONE);
            readingHistoryRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            isLoading = false;
            return;
        }
        // 调用API获取历史记录
        ApiClient.getBookService().getHistory(page, PAGE_SIZE, userId)
            .enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BizResponse<HistoryResponse>> call,
                                       @NonNull Response<BizResponse<HistoryResponse>> response) {
                    loadingProgressBar.setVisibility(View.GONE);
                    if (!response.isSuccessful()) {
                        Toast.makeText(requireContext(), "获取历史记录失败", Toast.LENGTH_SHORT).show();
                        isLoading = false;
                        return;
                    }
                    BizResponse<HistoryResponse> bizResponse = response.body();
                    if (bizResponse == null || !bizResponse.isSuccess()) {
                        String errorMsg = bizResponse != null ? bizResponse.getErrorMsg() : "Unknown error";
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                        isLoading = false;
                        return;
                    }
                    
                    HistoryResponse historyResponse = bizResponse.getData();
                    List<ReadingHistory> histories = historyResponse != null ? historyResponse.getResults() : new ArrayList<>();
                    
                    if (histories.isEmpty() && page == 1) {
                        readingHistoryRecyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        readingHistoryRecyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                        if (page == 1) {
                            adapter.setHistories(histories);
                        } else {
                            adapter.addHistories(histories);
                        }
                    }
                    isLoading = false;
                    hasMoreData = histories.size() >= PAGE_SIZE;
                }

                @Override
                public void onFailure(@NonNull Call<BizResponse<HistoryResponse>> call, @NonNull Throwable t) {
                    loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "网络请求失败", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Get reading history failed", t);
                    isLoading = false;
                }
            });
    }

    @Override
    public void onContinueReading(ReadingHistory history) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToReader(history.getId(), history.getFileId());
        }
    }

    @Override
    public void onDeleteHistory(ReadingHistory history) {
        // 获取用户ID
        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 调用删除接口
        ApiClient.getBookService().deleteHistory(userId, history.getFileId())
            .enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BizResponse<Void>> call,
                                       @NonNull Response<BizResponse<Void>> response) {
                    if (!response.isSuccessful()) {
                        Toast.makeText(requireContext(), "删除记录失败", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BizResponse<Void> bizResponse = response.body();
                    if (bizResponse != null && bizResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show();
                        // 重新加载第一页数据
                        loadReadingHistory(1);
                    } else {
                        String errorMsg = bizResponse != null ? bizResponse.getErrorMsg() : "未知错误";
                        Toast.makeText(requireContext(), "删除失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
                    Toast.makeText(requireContext(), "网络请求失败", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Delete history failed", t);
                }
            });
    }
}