package cn.zhangjh.zhiyue;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import io.noties.markwon.Markwon;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;


public class AIReadingFragment extends Fragment {
    private TextView aiSummaryText;
    private RecyclerView chatRecyclerView;
    private FrameLayout inputLayout;
    private TextInputEditText inputEditText;
    private MaterialButton sendButton;
    private LinearLayout progressLayout;
    private TextView progressPercentage;
    private LinearProgressIndicator progressBar;
    private static WebSocket webSocket;
    private static boolean isWebSocketInitialized = false;
    private StringBuilder summaryContent = new StringBuilder();
    // 保存Fragment的弱引用，保证切换Tab回来，消息处理的进度可以实时更新UI
    private static WeakReference<AIReadingFragment> currentActiveFragment = new WeakReference<>(null);
    private static final String TAG = AIReadingFragment.class.getName();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_ai_reading, container, false);
        
        initView(view);
        
        // 初始化 StringBuilder
        summaryContent = new StringBuilder();

        currentActiveFragment = new WeakReference<>(this);
        
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "AIReading Fragment onCreate called");
        // 只在第一次创建时初始化WebSocket
        if (!isWebSocketInitialized) {
            initWebSocket();
            isWebSocketInitialized = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initView(requireView());
        currentActiveFragment = new WeakReference<>(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // 只有当前实例才清空
        if (currentActiveFragment.get() == this) {
            currentActiveFragment = new WeakReference<>(null);
        }
    }

    private void initView(View view) {
        // 初始化进度相关视图
        progressLayout = view.findViewById(R.id.progress_layout);
        progressBar = view.findViewById(R.id.progress_bar);
        progressPercentage = view.findViewById(R.id.progress_percentage);

        aiSummaryText = view.findViewById(R.id.ai_summary_text);
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        inputLayout = view.findViewById(R.id.input_layout);
        inputEditText = view.findViewById(R.id.input_edit_text);
        sendButton = view.findViewById(R.id.send_button);
    }

    private void initWebSocket() {
        if (webSocket != null) {
            return;
        }
        String fileId = "一句顶一万句 (刘震云) (Z-Library).epub";
        String userId = "123456";
        String wsUrl = String.format("wss://tx.zhangjh.cn/socket/summary?userId=%s", userId);
        
        Log.d(TAG, "Connecting to WebSocket: " + wsUrl);
    
        Request request = new Request.Builder()
                .url(wsUrl)
                .build();
                
        OkHttpClient client = new OkHttpClient();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "WebSocket connection opened");
                // 连接建立后发送数据
                String message = String.format("{\"fileId\": \"%s\", \"userId\": \"%s\"}", fileId, userId);
                webSocket.send(message);
                Log.d(TAG, "Sent message: " + message);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "WebSocket received message: " + text);
                AIReadingFragment fragment = currentActiveFragment.get();
                if (fragment == null || !fragment.isAdded() || fragment.getView() == null) {
                    return;
                }
                fragment.requireActivity().runOnUiThread(() -> {
                    try {
                        JSONObject message = new JSONObject(text);
                        String type = message.getString("type");
                        
                        switch(type) {
                            case "summaryProgress":
                                String progressData = message.getString("data");
                                fragment.progressLayout.setVisibility(View.VISIBLE);
                                fragment.progressBar.setProgress((int)Double.parseDouble(progressData));
                                fragment.progressPercentage.setText(String.format("%s%%", progressData));
                                break;
                            case "data":
                                String contentData = message.getString("data");
                                // 先设置内容
                                fragment.summaryContent.append(contentData);
                                fragment.aiSummaryText.setVisibility(View.VISIBLE);
                                Markwon markwon = Markwon.create(fragment.requireContext());
                                markwon.setMarkdown(fragment.aiSummaryText, fragment.summaryContent.toString());
                                // 最后再隐藏进度条
                                fragment.progressLayout.post(() -> {
                                    fragment.progressLayout.setVisibility(View.GONE);
                                    Log.d(TAG, "Progress layout visibility set to GONE");
                                });
                                break;
                            case "finish":
                                // finish消息没有data字段，直接处理
                                // 总结完成后显示输入框
                                fragment.progressLayout.setVisibility(View.GONE);
                                fragment.inputLayout.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Summary finished, showing input layout");
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
                
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "连接失败: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        summaryContent = new StringBuilder(); // 清空累积的内容
        super.onDestroyView();
    }

    // 添加一个在应用退出时调用的方法
    public static void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Application closing");
            webSocket = null;
            isWebSocketInitialized = false;
        }
    }
}