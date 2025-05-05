package cn.zhangjh.zhiyue.fragment;

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
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONException;
import org.json.JSONObject;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.viewmodel.BookInfoViewModel;
import io.noties.markwon.Markwon;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class AISummaryFragment extends Fragment {
    private static final String TAG = AISummaryFragment.class.getName();
    private TextView summaryText;
    private View progressLayout;
    private TextView progressPercentage;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressBar;
    private static WebSocket webSocket;
    private static boolean isWebSocketInitialized = false;
    private StringBuilder summaryContent = new StringBuilder();
    private String title, author;
    private final StringBuilder summary = new StringBuilder();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_summary, container, false);
        
        summaryText = view.findViewById(R.id.ai_summary_text);
        progressLayout = view.findViewById(R.id.progress_layout);
        progressBar = view.findViewById(R.id.progress_bar);
        progressPercentage = view.findViewById(R.id.progress_percentage);

        if (!isWebSocketInitialized) {
            initWebSocket();
            isWebSocketInitialized = true;
        }
        
        return view;
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
                Log.d(TAG, "SummaryWebSocket connection opened");
                String message = String.format("{\"fileId\": \"%s\", \"userId\": \"%s\"}", fileId, userId);
                webSocket.send(message);
                Log.d(TAG, "Sent message: " + message);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "SummaryWs received message: " + text);
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
                                summary.append(data);
                                break;
                            case "summaryProgress":
                                progressLayout.setVisibility(View.VISIBLE);
                                progressBar.setProgress((int)Double.parseDouble(data));
                                progressPercentage.setText(String.format("%s%%", data));
                                break;
                            case "data":
                                progressLayout.setVisibility(View.GONE);
                                summaryText.setVisibility(View.VISIBLE);
                                summaryContent.append(data);
                                Markwon markwon = Markwon.create(requireContext());
                                markwon.setMarkdown(summaryText, summaryContent.toString());
                                break;
                            case "finish":
                                progressLayout.setVisibility(View.GONE);
                                updateBookInfo(title, author, summary.toString());
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
                Log.e(TAG, "SummaryWs connection failed", t);
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
        summaryContent = new StringBuilder();
        super.onDestroyView();
    }

    public static void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Fragment closing");
            webSocket = null;
            isWebSocketInitialized = false;
        }
    }

    private BookInfoViewModel viewModel;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BookInfoViewModel.class);
    }

    private void updateBookInfo(String title, String author, String summary) {
        viewModel.setBookInfo(title, author, summary);
    }

}