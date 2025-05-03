package cn.zhangjh.zhiyue;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.zhangjh.zhiyue.model.ChatMsg;
import io.noties.markwon.Markwon;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;


public class AIReadingFragment extends Fragment {
    private TextView aiSummaryText;
    private RecyclerView chatRecyclerView;
    private LinearLayout inputLayout;
    private TextInputEditText inputEditText;
    private LinearLayout progressLayout;
    private TextView progressPercentage;
    private LinearProgressIndicator progressBar;
    private static WebSocket summarySocket;
    private static WebSocket chatSocket;
    private String title;
    private String author;
    private String contentSummary;
    private String currentQuestion = "";
    private static final List<ChatContext> chatContexts = new ArrayList<>();
    private static boolean isWebSocketInitialized = false;
    private StringBuilder summaryContent = new StringBuilder();
    private final Handler pingHandler = new Handler(Looper.getMainLooper());
    private static final String TAG = AIReadingFragment.class.getName();
    
    // 添加聊天适配器和聊天消息列表
    private ChatAdapter chatAdapter;
    private final List<ChatMsg> chatMessages = new ArrayList<>();
    private StringBuilder currentResponse = new StringBuilder();
    private static final int MAX_CONTEXT_SIZE = 10;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_ai_reading, container, false);
        
        initView(view);
        
        // 初始化 StringBuilder
        summaryContent = new StringBuilder();

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "AIReading Fragment onCreate called");
        // 只在第一次创建时初始化WebSocket
        if (!isWebSocketInitialized) {
            initSummarySocket();
            isWebSocketInitialized = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initView(requireView());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void initView(View view) {
        // 初始化进度相关视图
        progressLayout = view.findViewById(R.id.progress_layout);
        progressBar = view.findViewById(R.id.progress_bar);
        progressPercentage = view.findViewById(R.id.progress_percentage);

        aiSummaryText = view.findViewById(R.id.ai_summary_text);
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        // 设置布局管理器
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // 初始化聊天适配器
        chatAdapter = new ChatAdapter(chatMessages, requireContext());
        chatRecyclerView.setAdapter(chatAdapter);
        inputLayout = view.findViewById(R.id.input_layout);
        inputEditText = view.findViewById(R.id.input_edit_text);
        MaterialButton sendButton = view.findViewById(R.id.send_button);
        
        // 设置发送按钮点击事件
        sendButton.setOnClickListener(v -> {
            String question = Objects.requireNonNull(inputEditText.getText()).toString().trim();
            if (!question.isEmpty()) {
                sendQuestion(question);
                inputEditText.setText("");
            }
        });
    }

    private void initSummarySocket() {
        if (summarySocket != null) {
            return;
        }
        String fileId = "一句顶一万句 (刘震云) (Z-Library).epub";
        String userId = "123456";
        String wsUrl = String.format("wss://tx.zhangjh.cn/socket/summary?userId=%s", userId);
        
        Log.d(TAG, "Connecting to SummarySocket: " + wsUrl);
    
        Request request = new Request.Builder()
                .url(wsUrl)
                .build();
                
        OkHttpClient client = new OkHttpClient();
        summarySocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "SummarySocket connection opened");
                // 连接建立后发送数据
                String message = String.format("{\"fileId\": \"%s\", \"userId\": \"%s\"}", fileId, userId);
                webSocket.send(message);
                Log.d(TAG, "Sent message: " + message);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "SummarySocket received message: " + text);
                FragmentActivity activity = getActivity();

                if (activity == null) {
                    return;
                }
                activity.runOnUiThread(() -> {
                    try {
                        JSONObject message = new JSONObject(text);
                        String type = message.optString("type");
                        String data = message.optString("data");
                        switch(type) {
                            case "title":
                                title = data;
                                break;
                            case "author":
                                author = data;
                                break;
                            case "contentSummary":
                                contentSummary = data;
                                break;
                            case "summaryProgress":
                                if(progressLayout.getVisibility() == View.GONE) {
                                    progressLayout.setVisibility(View.VISIBLE);
                                }
                                progressBar.setProgress((int)Double.parseDouble(data));
                                progressPercentage.setText(String.format("%s%%", data));
                                break;
                            case "data":
//                                if(progressLayout.getVisibility() == View.VISIBLE) {
                                    progressLayout.setVisibility(View.GONE);
//                                }
                                summaryContent.append(data);
                                if(aiSummaryText.getVisibility() == View.GONE) {
                                    aiSummaryText.setVisibility(View.VISIBLE);
                                }
                                Markwon markwon = Markwon.create(requireContext());
                                markwon.setMarkdown(aiSummaryText, summaryContent.toString());
                                break;
                            case "finish":
                                inputLayout.setVisibility(View.VISIBLE);
                                // 建立对话ws连接
                                initChatSocket(userId);
                                break;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON解析错误: " + e.getMessage());
                    }
                });
            }
            
            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, 
                    @Nullable Response response) {
                Log.e(TAG, "WebSocket connection failed", t);
                if(getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "连接失败: " + t.getMessage(),
                    Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void initChatSocket(String userId) {
        if(chatSocket != null) {
            return;
        }
        String wsUrl = String.format("wss://tx.zhangjh.cn/socket/chat?userId=%s", userId);
        Log.d(TAG, "Connecting to ChatSocket: " + wsUrl);

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();
        OkHttpClient client = new OkHttpClient();
        chatSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "ChatSocket connection opened");
                // 连接建立后建立心跳，每隔30s发送一次ping消息
                Runnable pingRunnable = new Runnable() {
                    @Override
                    public void run() {
                        String message = "{\"type\": \"ping\"}";
                        if (webSocket.send(message)) {
                            Log.d(TAG, "Sent ping");
                        }
                        pingHandler.postDelayed(this, 30000);
                    }
                };
                pingHandler.post(pingRunnable);
            }
            
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "ChatSocket received message: " + text);
                try {
                    JSONObject message = new JSONObject(text);
                    String type = message.optString("type");
                    String data = message.optString("data");
                    
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        try {
                            if ("data".equals(type)) {
                                // 增量更新回答内容
                                currentResponse.append(data);
                                updateLatestResponse(currentResponse.toString());
                            } else if ("finish".equals(type)) {
                                // 回答完成，添加到上下文
                                // 添加用户问题到上下文（从当前会话状态获取）
                                ChatContext userContext = new ChatContext();
                                userContext.setRole("user");
                                userContext.setContent(currentQuestion); // 需要添加一个成员变量来存储当前问题
                                chatContexts.add(userContext);

                                // 添加AI回答到上下文
                                ChatContext assistContext = new ChatContext();
                                assistContext.setRole("assist");
                                assistContext.setContent(currentResponse.toString());
                                chatContexts.add(assistContext);

                                // 保持上下文最多10条
                                while (chatContexts.size() > MAX_CONTEXT_SIZE) {
                                    chatContexts.remove(0);
                                }

                                // 重置当前回答
                                currentResponse = new StringBuilder();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "处理聊天消息失败", e);
                        }
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "JSON解析错误: " + e.getMessage());
                }
            }
            
            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t,
                    @Nullable Response response) {
                Log.e(TAG, "ChatSocket connection failed", t);
                if(getActivity() == null) return;

                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "连接失败: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    // 发送问题到服务器
    private void sendQuestion(String question) {
        if (chatSocket == null) {
            Toast.makeText(getContext(), "聊天连接未建立", Toast.LENGTH_SHORT).show();
            return;
        }
        currentQuestion = question;
        // 确保聊天记录列表可见
        chatRecyclerView.setVisibility(View.VISIBLE);
        
        // 添加问题到聊天记录
        ChatMsg chatMsg = new ChatMsg();
        chatMsg.setQuestion(question);
        chatMsg.setAnswer("正在思考中...");
        chatMessages.add(chatMsg);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        
        // 构建请求参数
        JSONObject requestData = new JSONObject();
        try {
            requestData.put("question", question);
            requestData.put("title", title);
            requestData.put("author", author);
            requestData.put("contentSummary", contentSummary);
            
            // 添加上下文
            JSONObject contextObj = new JSONObject();
            JSONArray contextArray = new JSONArray();
            for (ChatContext context : chatContexts) {
                JSONObject contextItem = new JSONObject();
                contextItem.put("role", context.getRole());
                contextItem.put("content", context.getContent());
                contextArray.put(contextItem);
            }
            contextObj.put("messages", contextArray);
            requestData.put("context", contextObj);
            
            // 发送请求
            chatSocket.send(requestData.toString());
        } catch (JSONException e) {
            Log.e(TAG, "构建请求参数失败", e);
        }
    }
    
    // 更新最新回答
    private void updateLatestResponse(String response) {
        if (!chatMessages.isEmpty()) {
            ChatMsg latestMsg = chatMessages.get(chatMessages.size() - 1);
            latestMsg.setAnswer(response);
            chatAdapter.notifyItemChanged(chatMessages.size() - 1);
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }

    @Override
    public void onDestroyView() {
        summaryContent = new StringBuilder(); // 清空累积的内容
        pingHandler.removeCallbacksAndMessages(null); // 移除所有回调
        super.onDestroyView();
    }

    // 添加一个在应用退出时调用的方法
    public static void closeWebSocket() {
        if (summarySocket != null) {
            summarySocket.close(1000, "Application closing");
            summarySocket = null;
            isWebSocketInitialized = false;
        }
    }
}

class ChatContext {
    private String role;
    private String content;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

// 添加聊天适配器类
class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<ChatMsg> chatMessages;
    private final Markwon markwon;
    
    public ChatAdapter(List<ChatMsg> chatMessages, Context context) {
        this.chatMessages = chatMessages;
        this.markwon = Markwon.create(context);
    }
    
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMsg chatMsg = chatMessages.get(position);
        holder.questionTextView.setText(chatMsg.getQuestion());
        markwon.setMarkdown(holder.answerTextView, chatMsg.getAnswer());
    }
    
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
    
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView questionTextView;
        TextView answerTextView;
        
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            questionTextView = itemView.findViewById(R.id.question_text_view);
            answerTextView = itemView.findViewById(R.id.answer_text_view);
        }
    }
}