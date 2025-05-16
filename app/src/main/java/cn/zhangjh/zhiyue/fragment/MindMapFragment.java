package cn.zhangjh.zhiyue.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONException;
import org.json.JSONObject;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.mindmap.MindMapManager;
import cn.zhangjh.zhiyue.viewmodel.BookInfoViewModel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MindMapFragment extends Fragment {
    private static final String TAG = MindMapFragment.class.getName();
    private String userId;
    private WebView mindMapWebView;
    private ProgressBar loadingProgress;
    private TextView errorText;
    private MindMapManager mindMapManager;
    private String title, author, partsSummary;

    private static WebSocket webSocket;
    private static boolean isWebSocketInitialized = false;
    private final StringBuilder markdownData = new StringBuilder();

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mind_map, container, false);
        
        mindMapWebView = view.findViewById(R.id.mind_map_webview);
        loadingProgress = view.findViewById(R.id.loading_progress);
        errorText = view.findViewById(R.id.error_text);

        if(!isWebSocketInitialized) {
            initWebSocket();
            isWebSocketInitialized = true;
        }
        initMindMap();
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE);
        this.userId = prefs.getString("userId", "");
        // 观察数据变化
        BookInfoViewModel viewModel = new ViewModelProvider(requireActivity()).get(BookInfoViewModel.class);
        viewModel.getTitle().observe(this, title -> this.title = title);
        viewModel.getAuthor().observe(this, author -> this.author = author);
        viewModel.getPartsSummary().observe(this, summary -> this.partsSummary = summary);
    }

    private void initWebSocket() {
        if (webSocket != null) {
            return;
        }
        String wsUrl = "wss://tx.zhangjh.cn/socket/mindmap?userId=" + this.userId;

        Log.d(TAG, "Connecting to MindMapSocket: " + wsUrl);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .url(wsUrl)
            .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "MindMapWebSocket connection opened");
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "MindMapWs received message: " + text);
                try {
                    JSONObject message = new JSONObject(text);
                    String type = message.getString("type");
                    
                    if ("data".equals(type)) {
                        String data = message.getString("data");
                        markdownData.append(data);
                    } else if ("finish".equals(type)) {
                        if (getActivity() != null) {
                            Log.d(TAG, "markdownData: " + markdownData.toString());
                            getActivity().runOnUiThread(() -> mindMapManager.renderMarkdown(markdownData.toString()));
                        }
                        closeWebSocket();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "MindMapWs message parse error", e);
                    showMindMapError();
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                Log.e(TAG, "MindMapWs connection failed", t);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showMindMapError());
                }
            }
        });
    }

    private void showMindMapLoading() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
            if (errorText != null) {
                errorText.setVisibility(View.GONE);
            }
        }
    }

    private void hideMindMapLoading() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.GONE);
        }
    }

    private void showMindMapError() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.GONE);
        }
        if (errorText != null) {
            errorText.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initMindMap() {
        WebSettings webSettings = mindMapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        
        // 添加调试支持
        mindMapWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(TAG, "MindMap Console: " + consoleMessage.message());
                return true;
            }
        });

        // 修正回调方法
        mindMapManager = new MindMapManager(mindMapWebView, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::hideMindMapLoading);
            }
        });

        mindMapWebView.addJavascriptInterface(mindMapManager.new JsInterface(), "Android");
        mindMapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showMindMapLoading();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "开始加载map data");
                if(TextUtils.isEmpty(title) || TextUtils.isEmpty(author) || TextUtils.isEmpty(partsSummary)) {
                    Toast.makeText(getActivity(), "请等待AI总结完成", Toast.LENGTH_SHORT).show();
                    return;
                }
                loadMindMapData();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                showMindMapError();
            }
        });

        showMindMapLoading();
        mindMapWebView.loadUrl("file:///android_asset/mindmap.html");
    }

    private void loadMindMapData() {
        try {
            JSONObject request = new JSONObject();
            request.put("title", title);
            request.put("author", author);
            request.put("summary", partsSummary);
            webSocket.send(request.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error loading mind map data", e);
            showMindMapError();
        }
    }

    @Override
    public void onDestroyView() {
        if (mindMapWebView != null) {
            mindMapWebView.destroy();
            mindMapWebView = null;
        }
        super.onDestroyView();
    }

    public static void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Fragment closing");
            webSocket = null;
            isWebSocketInitialized = false;
        }
    }
}