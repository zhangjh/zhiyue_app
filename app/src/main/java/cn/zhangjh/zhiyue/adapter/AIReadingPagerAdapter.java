package cn.zhangjh.zhiyue.adapter;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import cn.zhangjh.zhiyue.fragment.AISummaryFragment;
import cn.zhangjh.zhiyue.fragment.ChatFragment;
import cn.zhangjh.zhiyue.fragment.MindMapFragment;

public class AIReadingPagerAdapter extends FragmentStateAdapter {
    private final SparseArray<Fragment> fragments = new SparseArray<>();

    private final String fileId;

    public AIReadingPagerAdapter(@NonNull FragmentActivity fragmentActivity, @NonNull String fileId) {
        super(fragmentActivity);
        this.fileId = fileId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new AISummaryFragment(fileId);
                break;
            case 1:
                fragment = new ChatFragment();
                break;
            case 2:
                fragment = new MindMapFragment();
                break;
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
        fragments.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}