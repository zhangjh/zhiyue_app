package cn.zhangjh.zhiyue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ReaderFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reader, container, false);
        // 在这里添加你的 "阅读器" 页面的内容
        TextView textView = view.findViewById(R.id.text_reader);
        textView.setText("这里是阅读器页面");

        return view;
    }
}