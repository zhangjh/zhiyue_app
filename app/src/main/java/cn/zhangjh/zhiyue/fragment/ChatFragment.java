package cn.zhangjh.zhiyue.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import cn.zhangjh.zhiyue.R;

public class ChatFragment extends Fragment {
    private RecyclerView chatRecyclerView;
    private EditText inputEditText;
    private MaterialButton sendButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        inputEditText = view.findViewById(R.id.input_edit_text);
        sendButton = view.findViewById(R.id.send_button);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // TODO: 实现聊天功能
        
        return view;
    }
}