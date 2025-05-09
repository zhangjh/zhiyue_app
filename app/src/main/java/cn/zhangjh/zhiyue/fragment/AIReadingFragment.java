package cn.zhangjh.zhiyue.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.adapter.AIReadingPagerAdapter;


public class AIReadingFragment extends Fragment {
    private static final String TAG = AIReadingFragment.class.getName();
    private final String fileId;

    private final String[] tabTitles = new String[]{"AI总结", "AI问答", "思维导图"};

    public AIReadingFragment(String fileId) {
        this.fileId = fileId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_reading, container, false);

        ViewPager2 viewPager = view.findViewById(R.id.view_pager);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        // 设置适配器
        AIReadingPagerAdapter pagerAdapter = new AIReadingPagerAdapter(requireActivity(), fileId);
        viewPager.setAdapter(pagerAdapter);

        // 连接 TabLayout 和 ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        return view;
    }
}
