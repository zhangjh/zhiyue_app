package cn.zhangjh.zhiyue.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.adapter.ChatAdapter;
import cn.zhangjh.zhiyue.model.ChatContext;
import cn.zhangjh.zhiyue.model.ChatMsg;
import cn.zhangjh.zhiyue.viewmodel.BookInfoViewModel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatFragment extends Fragment {
    private static final String TAG = ChatFragment.class.getName();
    private String userId;
    private RecyclerView chatRecyclerView;
    private TextInputEditText inputEditText;
    private ChatAdapter chatAdapter;
    private final List<ChatMsg> chatMessages = new ArrayList<>();
    private final StringBuilder currentResponse = new StringBuilder();
    private static WebSocket chatSocket;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        SharedPreferences prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE);
        this.userId = prefs.getString("userId", "");

        initViews(view);
        initWebSocket();
        
        // 设置软键盘不遮挡输入框
        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        
        return view;
    }

    private void initViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        inputEditText = view.findViewById(R.id.input_edit_text);
        MaterialButton sendButton = view.findViewById(R.id.send_button);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatAdapter = new ChatAdapter(chatMessages, requireContext());
        chatRecyclerView.setAdapter(chatAdapter);

        sendButton.setOnClickListener(v -> {
            String question = Objects.requireNonNull(inputEditText.getText()).toString().trim();
            if (!question.isEmpty()) {
                sendQuestion(question);
                inputEditText.setText("");
            }
        });
    }

    private void initWebSocket() {
        String wsUrl = "wss://tx.zhangjh.cn/socket/chat?userId=" + this.userId;
        Request request = new Request.Builder().url(wsUrl).build();
        
        OkHttpClient client = new OkHttpClient();
        chatSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "ChatWebSocket connection opened");
                // 每隔10s发送一个ping消息保活
                handler.postDelayed(() -> webSocket.send("{\"type\":\"ping\"}"), 10000);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "ChatWs Received: " + text);
                try {
                    JSONObject message = new JSONObject(text);
                    String type = message.optString("type");
                    String data = message.optString("data");
                    
                    handler.post(() -> {
                        switch (type) {
                            case "data":
                                currentResponse.append(data);
                                updateLastAIMessage(currentResponse.toString(), false);
                                break;
                            case "finish":
                                updateLastAIMessage(currentResponse.toString(), true);
                                // 一轮问答结束清空缓冲
                                currentResponse.setLength(0);
                                break;
                        }
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error", e);
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                Log.e(TAG, "ChatWs failure", t);
                handler.post(() -> Toast.makeText(getContext(), "Connection failed: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String title;
    private String author;
    private String summary;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BookInfoViewModel viewModel = new ViewModelProvider(requireActivity()).get(BookInfoViewModel.class);

        // 观察数据变化
        viewModel.getTitle().observe(this, title -> this.title = title);
        viewModel.getAuthor().observe(this, author -> this.author = author);
        viewModel.getSummary().observe(this, summary -> this.summary = summary);
    }

    private static final int MAX_CONTEXT_SIZE = 10;
    private final List<ChatContext> chatContexts = new ArrayList<>();

    private void sendQuestion(String question) {
        if(TextUtils.isEmpty(title) || TextUtils.isEmpty(author) || TextUtils.isEmpty(summary)) {
            Toast.makeText(getActivity(), getString(R.string.please_wait_ai_summary), Toast.LENGTH_SHORT).show();
            return;
        }
        // 隐藏软键盘
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // 添加用户问题到消息列表
        chatMessages.add(new ChatMsg(question, ChatMsg.TYPE_USER));
        chatMessages.add(new ChatMsg("", ChatMsg.TYPE_AI));
        // 添加用户问题到上下文
        chatContexts.add(new ChatContext("user", question));
        
        chatAdapter.notifyItemRangeInserted(chatMessages.size() - 2, 2);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("language", MODE_PRIVATE);
            String language = prefs.getString("language", "en");
            JSONObject message = new JSONObject();
            message.put("question", question);
            message.put("title", title);
            message.put("author", author);
            message.put("summary", summary);
            message.put("language", language);
            
            // 构建上下文
            JSONObject contextObj = getContextObj();
            message.put("context", contextObj);

            Log.d(TAG, "Sending message: " + message);
            chatSocket.send(message.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating message", e);
        }
    }

    @NonNull
    private JSONObject getContextObj() throws JSONException {
        JSONObject contextObj = new JSONObject();
        JSONArray messagesArray = new JSONArray();

        // 获取最近的对话记录（最多10轮问答）
        int startIndex = Math.max(0, chatContexts.size() - MAX_CONTEXT_SIZE * 2);
        for (int i = startIndex; i < chatContexts.size(); i++) {
            JSONObject contextMessage = new JSONObject();
            ChatContext context = chatContexts.get(i);
            contextMessage.put("role", context.getRole());
            contextMessage.put("content", context.getContent());
            messagesArray.put(contextMessage);
        }

        contextObj.put("messages", messagesArray);
        return contextObj;
    }

    private void updateLastAIMessage(String content, boolean isComplete) {
        if (!chatMessages.isEmpty()) {
            ChatMsg lastMessage = chatMessages.get(chatMessages.size() - 1);
            if (lastMessage.getType() == ChatMsg.TYPE_AI) {
                // 保存旧内容长度，用于判断是否需要滚动
                int oldContentLength = lastMessage.getContent().length();
                
                // 更新消息内容
                lastMessage.setContent(content);
                lastMessage.setComplete(isComplete);
                
                // 使用更精确的局部更新方式
                int position = chatMessages.size() - 1;
                chatAdapter.notifyItemChanged(position, "content_update");
                
                // 如果内容增加了，确保滚动到底部
                if (content.length() > oldContentLength) {
                    // 使用smoothScrollToPosition而不是scrollToPosition，使滚动更平滑
                    chatRecyclerView.postDelayed(() -> chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1), 100); // 短暂延迟确保布局已更新
                }
                
                // 当AI回复完成时，将回复添加到上下文
                if (isComplete) {
                    chatContexts.add(new ChatContext("assistant", content));
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 恢复软键盘模式
        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
    }

    public static void closeWebSocket() {
        if (chatSocket != null) {
            chatSocket.close(1000, "Fragment closing");
            chatSocket = null;
        }
    }
}