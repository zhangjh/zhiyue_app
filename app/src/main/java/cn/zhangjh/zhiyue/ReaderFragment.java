package cn.zhangjh.zhiyue;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.mindmap.MindMapManager;
import cn.zhangjh.zhiyue.model.DownloadResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderFragment extends Fragment {

    private static final String TAG = ReaderFragment.class.getName();
    private String bookUrl;
    private WebView webView;
    private boolean isNavigationVisible = false;
    private int currentProgress = 0;
    private View loadingView; // 新增加载视图
    // 将变量声明移到类开始处
    private WebView mindMapWebView;
    private View mindMapLoadingProgress;
    private TextView mindMapErrorText;
    private MindMapManager mindMapManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置返回键监听
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 返回到找书页面
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSearch();
                }
            }
        });
        if (getArguments() != null) {
            String bookId = getArguments().getString("book_id");
            String hashId = getArguments().getString("hash_id");
            Log.d("ReaderFragment", "Received bookId: " + bookId + ", hashId: " + hashId);
            // 获取书籍url
//            getEbookUrl(bookId, hashId);
            bookUrl = "https://s3.zhangjh.cn/三体 (刘慈欣) (Z-Library).epub";
        }
    }

    private void getEbookUrl(String bookId, String hashId) {
        ApiClient.getBookService().downloadBook(bookId, hashId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<DownloadResponse> call, @NonNull Response<DownloadResponse> response) {
                if (!response.isSuccessful()) {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Error response: " + errorBody);
                        showError("获取ebook url失败 (" + response.code() + "): " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error response", e);
                        showError("获取ebook url失败 (" + response.code() + ")");
                    }
                    return;
                }
                if (response.body() != null && response.body().isSuccess()) {
                    bookUrl = response.body().getData();
                    Log.d(TAG, "Ebook url: " + bookUrl);
                }
            }

            @Override
            public void onFailure(Call<DownloadResponse> call, Throwable t) {
                Log.e(TAG, "Search failed", t);
                String errorMessage = "网络错误: " + t.getClass().getSimpleName();
                if (t.getMessage() != null) {
                    errorMessage += " - " + t.getMessage();
                }
                showError(errorMessage);
            }
        });
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (loadingView != null) {
                    loadingView.setVisibility(View.VISIBLE);
                    // 更新加载提示文本
                    TextView loadingText = loadingView.findViewById(R.id.loading_text);
                    if (loadingText != null) {
                        loadingText.setText(message);
                    }
                }
            });
        }
    }

    private void hideLoading() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (loadingView != null) {
                    loadingView.setVisibility(View.GONE);
                }
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reader, container, false);

        webView = view.findViewById(R.id.webview_reader);
        loadingView = view.findViewById(R.id.loading_view); // 获取加载视图

        showLoading("正在加载阅读器...");

        // 配置WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // 添加JavaScript接口
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // 设置WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showLoading("正在初始化...");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (bookUrl != null) {
                    showLoading("正在加载电子书...");
                    webView.evaluateJavascript("loadBook('" + bookUrl + "')", null);
                }
            }
        });

        // 加载epub.js和电子书
        String readerHtml = "file:///android_asset/reader.html";
        webView.loadUrl(readerHtml);

        View aiReadingLayout = view.findViewById(R.id.ai_reading_layout);
        View mindMapLayout = view.findViewById(R.id.mind_map_layout);

        // 初始化TabLayout
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // 阅读器
                        webView.setVisibility(View.VISIBLE);
                        aiReadingLayout.setVisibility(View.GONE);
                        mindMapLayout.setVisibility(View.GONE);
                        break;
                    case 1: // AI伴读
                        webView.setVisibility(View.GONE);
                        aiReadingLayout.setVisibility(View.VISIBLE);
                        mindMapLayout.setVisibility(View.GONE);
                        break;
                    case 2: // 思维导图
                        // 初始化思维导图
                        initMindMap(view);
                        webView.setVisibility(View.GONE);
                        aiReadingLayout.setVisibility(View.GONE);
                        mindMapLayout.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroyView();
        // 当阅读器页面销毁时，显示底部导航栏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBottomNavigation();
        }
    }

    // JavaScript接口类
    private class WebAppInterface {
        @JavascriptInterface
        public void onProgressUpdate(int progress) {
            // if (getActivity() != null) {
            //     getActivity().runOnUiThread(() -> {
            //         currentProgress = progress;
            //         // 这里可以保存进度到本地存储
            //         // 也可以更新UI显示当前进度
            //         Toast.makeText(getContext(), "阅读进度: " + progress + "%", Toast.LENGTH_SHORT).show();
            //     });
            // }
        }

        @JavascriptInterface
        public void onBookLoaded() {
            hideLoading();
        }
    }


    // 重命名思维导图相关的方法，避免冲突
    private void showMindMapLoading() {
        if (mindMapLoadingProgress != null) {
            mindMapLoadingProgress.setVisibility(View.VISIBLE);
            if (mindMapErrorText != null) {
                mindMapErrorText.setVisibility(View.GONE);
            }
        }
    }

    private void hideMindMapLoading() {
        if (mindMapLoadingProgress != null) {
            mindMapLoadingProgress.setVisibility(View.GONE);
        }
    }

    private void showMindMapError() {
        if (mindMapLoadingProgress != null) {
            mindMapLoadingProgress.setVisibility(View.GONE);
        }
        if (mindMapErrorText != null) {
            mindMapErrorText.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initMindMap(View view) {
        mindMapWebView = view.findViewById(R.id.mind_map_webview);
        mindMapLoadingProgress = view.findViewById(R.id.loading_progress);
        mindMapErrorText = view.findViewById(R.id.error_text);

        WebSettings webSettings = mindMapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // 添加调试支持
        mindMapWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(TAG, "MindMap Console: " + consoleMessage.message());
                return true;
            }
        });

        // 允许加载网络资源
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

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
}