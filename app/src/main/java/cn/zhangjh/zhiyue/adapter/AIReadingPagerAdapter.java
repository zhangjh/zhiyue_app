package cn.zhangjh.zhiyue.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import cn.zhangjh.zhiyue.fragment.AISummaryFragment;
import cn.zhangjh.zhiyue.fragment.ChatFragment;
import cn.zhangjh.zhiyue.fragment.MindMapFragment;

public class AIReadingPagerAdapter extends FragmentStateAdapter {

    public AIReadingPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AISummaryFragment();
            case 1:
                return new ChatFragment();
            case 2:
                return new MindMapFragment();
            default:
                return new AISummaryFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}