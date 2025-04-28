package cn.zhangjh.zhiyue;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import io.noties.markwon.Markwon;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;


public class AIReadingFragment extends Fragment {
    private TextView aiSummaryText;
    private RecyclerView chatRecyclerView;
    private TextInputEditText inputEditText;
    private MaterialButton sendButton;
    private View progressLayout;
    private TextView progressText;
    private LinearProgressIndicator progressBar;
    private WebSocket webSocket;

    private static final String TAG = AIReadingFragment.class.getName();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_ai_reading, container, false);
        
        // 初始化进度相关视图
        progressLayout = view.findViewById(R.id.progress_layout);
        progressText = view.findViewById(R.id.progress_text);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // 初始化视图
        aiSummaryText = view.findViewById(R.id.ai_summary_text);
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        inputEditText = view.findViewById(R.id.input_edit_text);
        sendButton = view.findViewById(R.id.send_button);
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 在Fragment可见时初始化WebSocket
        initWebSocket();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 在Fragment不可见时关闭WebSocket
        if (webSocket != null) {
            webSocket.close(1000, "Fragment paused");
            webSocket = null;
        }
    }
    private void initWebSocket() {
        String fileId = "三体 (刘慈欣) (Z-Library).epub";
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
                if(getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject message = new JSONObject(text);
                        String type = message.getString("type");
                        String data = message.getString("data");
                        
                        switch(type) {
                            case "summaryProgress":
                                double progress = Double.parseDouble(data);
                                progressBar.setProgress((int)progress);
                                progressText.setText(String.format("正在总结中，请稍等... %.1f%%", progress));
                                break;
                            case "contentSummary":
                                progressLayout.setVisibility(View.GONE);
                                Markwon markwon = Markwon.create(requireContext());
                                markwon.setMarkdown(aiSummaryText, data);
                                break;
                            case "finish":
                                progressLayout.setVisibility(View.GONE);
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
            
            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket closing: code=" + code + ", reason=" + reason);
            }
            
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket closed: code=" + code + ", reason=" + reason);
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
        if (webSocket != null) {
            webSocket.close(1000, "Fragment destroyed");  // 1000 表示正常关闭
        }
        super.onDestroyView();
    }
}