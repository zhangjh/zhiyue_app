package cn.zhangjh.zhiyue.fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.adapter.AIReadingPagerAdapter;


public class AIReadingFragment extends Fragment {
    private String fileId;
    private TabLayout tabLayout;
    private View rootView;

    public AIReadingFragment(String fileId) {
        this.fileId = fileId;
    }

    public AIReadingFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ai_reading, container, false);

        ViewPager2 viewPager = rootView.findViewById(R.id.view_pager);
        tabLayout = rootView.findViewById(R.id.tab_layout);

        // 设置适配器
        AIReadingPagerAdapter pagerAdapter = new AIReadingPagerAdapter(requireActivity(), fileId);
        viewPager.setAdapter(pagerAdapter);

        String[] tabTitles = new String[]{
                requireActivity().getString(R.string.ai_summary),
                requireActivity().getString(R.string.ai_chat),
                requireActivity().getString(R.string.ai_mind_map)
        };

        // 连接 TabLayout 和 ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        // 应用主题
        applyTheme();

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void applyTheme() {
        if (tabLayout != null && rootView != null) {
            int backgroundColor = ContextCompat.getColor(requireContext(), R.color.background);
            int textColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
            int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
            
            tabLayout.setBackgroundColor(backgroundColor);
            tabLayout.setTabTextColors(textColor, primaryColor);
            tabLayout.setSelectedTabIndicatorColor(primaryColor);
            rootView.setBackgroundColor(backgroundColor);
        }
    }

    public void updateTheme() {
        applyTheme();
        // 通知子Fragment更新主题
        FragmentManager fm = getChildFragmentManager();
        for (Fragment fragment : fm.getFragments()) {
            if (fragment instanceof MindMapFragment) {
                ((MindMapFragment) fragment).updateTheme();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTheme();
    }
}
