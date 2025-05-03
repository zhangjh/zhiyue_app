package cn.zhangjh.zhiyue.fragment;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.mindmap.MindMapManager;

public class MindMapFragment extends Fragment {
    private static final String TAG = MindMapFragment.class.getName();
    private WebView mindMapWebView;
    private ProgressBar loadingProgress;
    private TextView errorText;
    private MindMapManager mindMapManager;

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mind_map, container, false);
        
        mindMapWebView = view.findViewById(R.id.mind_map_webview);
        loadingProgress = view.findViewById(R.id.loading_progress);
        errorText = view.findViewById(R.id.error_text);

        initMindMap();
        
        return view;
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
            String testMarkdown = "# 三体\n" +
                    "## 第一部：地球往事\n" +
                    "### 文化大革命\n" +
                    "### 红岸基地\n" +
                    "### 三体文明\n" +
                    "## 第二部：黑暗森林\n" +
                    "### 面壁计划\n" +
                    "### 黑暗森林理论\n" +
                    "## 第三部：死神永生\n" +
                    "### 二向箔\n" +
                    "### 降维打击";
            mindMapManager.renderMarkdown(testMarkdown);
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
}