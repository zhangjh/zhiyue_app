package cn.zhangjh.zhiyue;

import android.os.Bundle;
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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

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
    private MaterialButton sendButton;
    private LinearLayout progressLayout;
    private TextView progressPercentage;
    private LinearProgressIndicator progressBar;
    private static WebSocket webSocket;
    private static boolean isWebSocketInitialized = false;
    private StringBuilder summaryContent = new StringBuilder();
    private static final String TAG = AIReadingFragment.class.getName();
    
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
            initWebSocket();
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
                            case "summaryProgress":
                                if(progressLayout.getVisibility() == View.GONE) {
                                    progressLayout.setVisibility(View.VISIBLE);
                                }
                                progressBar.setProgress((int)Double.parseDouble(data));
                                progressPercentage.setText(String.format("%s%%", data));
                                break;
                            case "data":
                                if(progressLayout.getVisibility() == View.VISIBLE) {
                                    progressLayout.setVisibility(View.GONE);
                                }
                                summaryContent.append(data);
                                if(aiSummaryText.getVisibility() == View.GONE) {
                                    aiSummaryText.setVisibility(View.VISIBLE);
                                }
                                Markwon markwon = Markwon.create(requireContext());
                                markwon.setMarkdown(aiSummaryText, summaryContent.toString());
                                break;
                            case "finish":
                                inputLayout.setVisibility(View.VISIBLE);
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