package cn.zhangjh.zhiyue.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.adapter.ReadingHistoryAdapter;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.billing.SubscriptionInfo;
import cn.zhangjh.zhiyue.billing.SubscriptionManager;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.HistoryResponse;
import cn.zhangjh.zhiyue.model.ReadingHistory;
import cn.zhangjh.zhiyue.utils.BizUtils;
import cn.zhangjh.zhiyue.utils.LogUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment implements ReadingHistoryAdapter.OnHistoryItemClickListener {
    private static final int PAGE_SIZE = 5;
    private String currentUserId;
    
    private ShapeableImageView userAvatar;
    private TextView userName;
    private RecyclerView readingHistoryRecyclerView;
    private ReadingHistoryAdapter adapter;
    private ProgressBar loadingProgressBar;
    private View emptyView;
    private SubscriptionManager subscriptionManager;
    // 订阅相关视图
    private TextView subscriptionStatus;
    private TextView subscriptionType;
    private TextView subscriptionExpireDate;
    private LinearLayout subscriptionInfoLayout;
    private Button subscribeButton;
    private Button manageSubscriptionButton;
    
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private int currentPage = 1;

    private static final String TAG = ProfileFragment.class.getName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subscriptionManager = SubscriptionManager.getInstance(requireActivity());
        subscriptionManager.querySubscriptionInfo(info -> {
            if(info != null) {
                SharedPreferences prefs = requireActivity().getSharedPreferences("subscription", Context.MODE_PRIVATE);
                prefs.edit().putString("subscriptionInfo", new Gson().toJson(info)).apply();
                prefs.edit().putBoolean("isSubscribed", true).apply();
            }
            return null;
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        initViews(view);
        setupRecyclerView();
        loadUserInfo();
        loadReadingHistory(1);

        checkSubscriptionStatus();

        // 修改订阅按钮点击事件
        subscribeButton.setOnClickListener(v -> {
            LogUtil.d(TAG, "subscribe button clicked");
            // 显示加载进度
            ProgressBar progressBar = new ProgressBar(requireContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setView(progressBar);
            AlertDialog loadingDialog = builder.create();
            loadingDialog.show();
    
            // 使用实际订阅方法
            subscriptionManager.subscribe(info -> {
                loadingDialog.dismiss();
                if (info != null) {
                    // 更新订阅状态UI
                    updateSubscriptionUI();
                    // 更新订阅详情
                    updateSubscriptionDetails(info);
                }
            });
        });
        
        manageSubscriptionButton.setOnClickListener(v -> {
            // 打开订阅管理页面
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/account/subscriptions"));
            startActivity(intent);
        });
    }

    private void initViews(View view) {
        userAvatar = view.findViewById(R.id.userAvatar);
        userName = view.findViewById(R.id.userName);
        readingHistoryRecyclerView = view.findViewById(R.id.readingHistoryRecyclerView);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyView = view.findViewById(R.id.emptyView);
        
        // 初始化订阅相关视图
        subscriptionStatus = view.findViewById(R.id.subscriptionStatus);
        subscriptionType = view.findViewById(R.id.subscriptionType);
        subscriptionExpireDate = view.findViewById(R.id.subscriptionExpireDate);
        subscriptionInfoLayout = view.findViewById(R.id.subscriptionInfoLayout);
        subscribeButton = view.findViewById(R.id.subscribeButton);
        manageSubscriptionButton = view.findViewById(R.id.manageSubscriptionButton);
        View subscriptionManageContent = view.findViewById(R.id.subscriptionManageContent);
	    TextView subscriptionManageTitle = view.findViewById(R.id.subscriptionManageTitle);
        // 默认隐藏订阅详情
        subscriptionInfoLayout.setVisibility(View.GONE);
        // 订阅管理内容默认折叠，点击标题展开/收起
        subscriptionManageTitle.setOnClickListener(v -> {
            if (subscriptionManageContent.getVisibility() == View.VISIBLE) {
                subscriptionManageContent.setVisibility(View.GONE);
            } else {
                subscriptionManageContent.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkSubscriptionStatus() {
        // 检查是否已订阅
        if (subscriptionManager.isSubscribed()) {
            updateSubscriptionUI();
            // 查询订阅
            SharedPreferences prefs = requireActivity().getSharedPreferences("subscription", MODE_PRIVATE);
            String subscriptionInfo = prefs.getString("subscriptionInfo", "");
            if(!TextUtils.isEmpty(subscriptionInfo)) {
                SubscriptionInfo info = new Gson().fromJson(subscriptionInfo, SubscriptionInfo.class);
                updateSubscriptionDetails(info);
            }
        }
    }

    private void updateSubscriptionUI() {
        // 已订阅状态
        subscriptionStatus.setText(getString(R.string.subscription_already));
        subscriptionStatus.setTextColor(getResources().getColor(R.color.primary, null));

        // 显示订阅类型和到期时间
        subscriptionInfoLayout.setVisibility(View.VISIBLE);

        // 显示管理订阅按钮，隐藏订阅按钮
        subscribeButton.setVisibility(View.GONE);
        manageSubscriptionButton.setVisibility(View.VISIBLE);
    }
    
    private void updateSubscriptionDetails(SubscriptionInfo info) {
        if (info != null) {
            // 格式化到期时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String expireDate = sdf.format(info.getExpireDate());
            
            // 更新UI
            subscriptionType.setText(info.getSubscriptionName());
            subscriptionExpireDate.setText(expireDate);
        }
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
        userName.setText(prefs.getString("name", ""));

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

        if (TextUtils.isEmpty(currentUserId)) {
            loadingProgressBar.setVisibility(View.GONE);
            readingHistoryRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            isLoading = false;
            return;
        }
        // 调用API获取历史记录
        ApiClient.getBookService().getHistory(page, PAGE_SIZE, currentUserId, "modify_time", "desc")
            .enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BizResponse<HistoryResponse>> call,
                                       @NonNull Response<BizResponse<HistoryResponse>> response) {
                    loadingProgressBar.setVisibility(View.GONE);
                    if (!response.isSuccessful()) {
                        Toast.makeText(requireContext(), getString(R.string.reading_history_failed), Toast.LENGTH_SHORT).show();
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
                    // 缓存
                    List<String> cachedBookIds = histories.stream()
                            .map(ReadingHistory::getBookId)
                            .filter(item -> !TextUtils.isEmpty(item))
                            .collect(Collectors.toList());
                    BizUtils.saveCache(requireActivity(), "reader", "cachedBookIds", new Gson().toJson(cachedBookIds));
                    isLoading = false;
                    hasMoreData = histories.size() >= PAGE_SIZE;
                }

                @Override
                public void onFailure(@NonNull Call<BizResponse<HistoryResponse>> call, @NonNull Throwable t) {
                    loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    LogUtil.e(TAG, "Get reading history failed", t);
                    isLoading = false;
                }
            });
    }

    @Override
    public void onContinueReading(ReadingHistory history) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToReader(
                    history.getBookId(),
                    history.getHashId(),
                    history.getFileId(),
                    history.getCfi());
        }
    }

    @Override
    public void onDeleteHistory(ReadingHistory history) {
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(requireContext(), getString(R.string.please_login_first), Toast.LENGTH_SHORT).show();
            return;
        }

        // 调用删除接口
        ApiClient.getBookService().deleteHistory(currentUserId, history.getFileId())
            .enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BizResponse<Void>> call,
                                       @NonNull Response<BizResponse<Void>> response) {
                    if (!response.isSuccessful()) {
                        Toast.makeText(requireContext(), getString(R.string.delete_record_failed), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BizResponse<Void> bizResponse = response.body();
                    if (bizResponse != null && bizResponse.isSuccess()) {
                        Toast.makeText(requireContext(), getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                        // 重新加载第一页数据
                        loadReadingHistory(1);
                    } else {
                        String errorMsg = bizResponse != null ? bizResponse.getErrorMsg() : getString(R.string.unknown_error);
                        Toast.makeText(requireContext(), getString(R.string.delete_history_failed) + ": " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
                    Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    LogUtil.e(TAG, "Delete history failed", t);
                }
            });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        subscriptionManager.destroy();
    }
}