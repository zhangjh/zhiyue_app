package cn.zhangjh.zhiyue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.zhangjh.zhiyue.activity.MainActivity;

public class ReaderFragment extends Fragment {

    private String bookId;
    private boolean isNavigationVisible = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookId = getArguments().getString("book_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reader, container, false);
        // 在这里添加你的 "阅读器" 页面的内容
        TextView textView = view.findViewById(R.id.text_reader);
        textView.setText("这里是阅读器页面");

        // 设置点击监听，切换导航栏显示状态
        view.setOnClickListener(v -> toggleNavigationVisibility());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO: 使用bookId加载书籍内容
    }

    private void toggleNavigationVisibility() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            if (isNavigationVisible) {
                activity.hideBottomNavigation();
            } else {
                activity.showBottomNavigation();
            }
            isNavigationVisible = !isNavigationVisible;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 当阅读器页面销毁时，显示底部导航栏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBottomNavigation();
        }
    }
}