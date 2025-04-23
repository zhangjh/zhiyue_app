package cn.zhangjh.zhiyue;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.zhangjh.zhiyue.activity.MainActivity;

public class ReaderFragment extends Fragment {

    private String bookUrl;
    private WebView webView;
    private ProgressBar progressBar;
    private boolean isNavigationVisible = false;
    private int currentProgress = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookUrl = getArguments().getString("book_url");
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reader, container, false);
        
        webView = view.findViewById(R.id.webview_reader);
        progressBar = view.findViewById(R.id.progress_bar);

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
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完成后，调用JavaScript方法加载电子书
                if (bookUrl != null) {
                    webView.evaluateJavascript("loadBook('" + bookUrl + "')", null);
                }
            }
        });

        // 设置WebChromeClient来处理进度
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }
            }
        });

        // 加载epub.js和电子书
        String readerHtml = "file:///android_asset/reader.html";
        webView.loadUrl(readerHtml);

        // 设置点击监听，切换导航栏显示状态
        view.setOnClickListener(v -> toggleNavigationVisibility());

        return view;
    }

    private void toggleNavigationVisibility() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            if (isNavigationVisible) {
                activity.hideBottomNavigation();
            } else {
                activity.showBottomNavigation();
            }
            isNavigationVisible = !isNavigationVisible;
        }
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
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    currentProgress = progress;
                    // 这里可以保存进度到本地存储
                    // 也可以更新UI显示当前进度
                    Toast.makeText(getContext(), "阅读进度: " + progress + "%", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}