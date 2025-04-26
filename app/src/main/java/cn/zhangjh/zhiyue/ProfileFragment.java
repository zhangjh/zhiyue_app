package cn.zhangjh.zhiyue;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.model.ReadingHistory;

public class ProfileFragment extends Fragment implements ReadingHistoryAdapter.OnHistoryItemClickListener {
    private static final int PAGE_SIZE = 5;
    
    private ShapeableImageView userAvatar;
    private TextView userName;
    private TextView userStatus;
    private RecyclerView readingHistoryRecyclerView;
    private ReadingHistoryAdapter adapter;
    private ProgressBar loadingProgressBar;
    private View emptyView;
    
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private int currentPage = 1;

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
        userStatus = view.findViewById(R.id.userStatus);
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
        // 模拟用户数据
        userName.setText("测试用户");
        userStatus.setText("普通会员");
        // 设置默认头像
        userAvatar.setImageResource(R.drawable.default_avatar);
    }

    private void loadReadingHistory(int page) {
        if (page == 1) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            readingHistoryRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }
        
        isLoading = true;
        currentPage = page;
        
        // 模拟网络请求延迟
        new Handler().postDelayed(() -> {
            List<ReadingHistory> histories = new ArrayList<>();
            for (int i = 0; i < PAGE_SIZE; i++) {
                ReadingHistory history = new ReadingHistory();
                history.setBookId("book_" + ((page - 1) * PAGE_SIZE + i));
                history.setBookTitle("测试书籍 " + ((page - 1) * PAGE_SIZE + i));
                history.setBookAuthor("作者 " + i);
                history.setStartTime("2024-01-" + (10 + i));
                history.setLastReadTime("2024-01-" + (20 + i));
                history.setProgress(i * 10);
                history.setHash("hash_" + i);
                histories.add(history);
            }
            ReadingHistory history = new ReadingHistory();
            history.setBookTitle("11111");
            history.setBookAuthor("22222");
            history.setStartTime("2024-01-01");
            history.setLastReadTime("2024-04-01");
            history.setProgress(80);
            history.setHash("hash_55555");
            histories.add(history);

            loadingProgressBar.setVisibility(View.GONE);
            
            if (histories.isEmpty() && page == 1) {
                // 第一页且没有数据时显示空状态
                readingHistoryRecyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                // 有数据时显示列表
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
        }, 1000); // 1秒延迟模拟网络请求
    }

    @Override
    public void onContinueReading(ReadingHistory history) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToReader(history.getBookId(), history.getHash());
        }
    }

    @Override
    public void onDeleteHistory(ReadingHistory history) {
        // TODO: 实现删除阅读记录的逻辑
    }
}