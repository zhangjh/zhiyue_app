package cn.zhangjh.zhiyue.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.adapter.ReadingHistoryAdapter;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.billing.BillingManager;
import cn.zhangjh.zhiyue.billing.SubscriptionInfo;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.HistoryResponse;
import cn.zhangjh.zhiyue.model.ReadingHistory;
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
    
    private BillingManager billingManager;
    private boolean isUserSubscribed = false;

    private static final String TAG = ProfileFragment.class.getName();

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
        
        // 初始化订阅管理
        initBillingManager();

        // 修改订阅按钮点击事件
        subscribeButton.setOnClickListener(v -> {
            // 在测试阶段使用模拟订阅
            mockSubscription();

            // 正式环境下使用实际订阅
            // if (billingManager != null) {
            //     billingManager.subscribeMonthly();
            // }
        });
        
        manageSubscriptionButton.setOnClickListener(v -> {
            // 打开订阅管理页面
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/account/subscriptions"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "无法打开订阅管理页面", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mockSubscription() {
        // 模拟订阅成功
        isUserSubscribed = true;
        updateSubscriptionUI(true);

        // 模拟订阅详情
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date expireDate = new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);

        SubscriptionInfo mockInfo = new SubscriptionInfo(
                true,
                "包月服务（测试）",
                expireDate,
                "smart_reader_monthly_subscription"
        );

        updateSubscriptionDetails(mockInfo);

        Toast.makeText(requireContext(), "测试模式：订阅成功", Toast.LENGTH_SHORT).show();
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
        
        // 默认隐藏订阅详情
        subscriptionInfoLayout.setVisibility(View.GONE);
    }
    
    private void initBillingManager() {
        billingManager = new BillingManager(requireActivity(), new BillingManager.BillingCallback() {
            @Override
            public void onBillingSetupFinished() {
                // 查询订阅状态
                billingManager.querySubscriptionStatus();
            }

            @Override
            public void onSubscriptionStatusChecked(boolean isSubscribed) {
                isUserSubscribed = isSubscribed;
                
                // 在UI线程更新订阅状态
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateSubscriptionUI(isSubscribed));
                }
                
                // 如果已订阅，获取详细信息
                if (isSubscribed && billingManager != null) {
                    billingManager.getSubscriptionDetails(subscriptionInfo -> {
                        if (getActivity() != null && subscriptionInfo != null) {
                            getActivity().runOnUiThread(() -> updateSubscriptionDetails(subscriptionInfo));
                        }
                    });
                }
            }

            @Override
            public void onPurchaseSuccess() {
                // 购买成功后重新查询订阅状态
                if (billingManager != null) {
                    billingManager.querySubscriptionStatus();
                }
                Toast.makeText(requireContext(), "订阅成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPurchaseFailure(int responseCode, String message) {
                Toast.makeText(requireContext(), "订阅失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateSubscriptionUI(boolean isSubscribed) {
        if (isSubscribed) {
            // 已订阅状态
            subscriptionStatus.setText("已订阅");
            subscriptionStatus.setTextColor(getResources().getColor(R.color.primary, null));
            
            // 显示订阅类型和到期时间
            subscriptionInfoLayout.setVisibility(View.VISIBLE);
            
            // 显示管理订阅按钮，隐藏订阅按钮
            subscribeButton.setVisibility(View.GONE);
            manageSubscriptionButton.setVisibility(View.VISIBLE);
            
            // 保存订阅状态到SharedPreferences
            SharedPreferences prefs = requireActivity().getSharedPreferences("subscription", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("isSubscribed", true).apply();
        } else {
            // 未订阅状态
            subscriptionStatus.setText("未订阅");
            subscriptionStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
            
            // 隐藏订阅类型和到期时间
            subscriptionInfoLayout.setVisibility(View.GONE);
            
            // 显示订阅按钮，隐藏管理订阅按钮
            subscribeButton.setVisibility(View.VISIBLE);
            manageSubscriptionButton.setVisibility(View.GONE);
            
            // 保存订阅状态到SharedPreferences
            SharedPreferences prefs = requireActivity().getSharedPreferences("subscription", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("isSubscribed", false).apply();
        }
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

        if (TextUtils.isEmpty(currentUserId)) {
            loadingProgressBar.setVisibility(View.GONE);
            readingHistoryRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            isLoading = false;
            return;
        }
        // 调用API获取历史记录
        ApiClient.getBookService().getHistory(page, PAGE_SIZE, currentUserId)
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
            ((MainActivity) getActivity()).navigateToReader(history.getId(), "", history.getFileId(), history.getCfi());
        }
    }

    @Override
    public void onDeleteHistory(ReadingHistory history) {
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 调用删除接口
        ApiClient.getBookService().deleteHistory(currentUserId, history.getFileId())
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (billingManager != null) {
            billingManager.destroy();
        }
    }
}