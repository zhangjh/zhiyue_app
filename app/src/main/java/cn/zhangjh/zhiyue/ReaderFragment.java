package cn.zhangjh.zhiyue;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.model.DownloadResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderFragment extends Fragment {

    private static final String TAG = ReaderFragment.class.getName();
    private String bookUrl;
    private WebView webView;
    private ProgressBar progressBar;
    private boolean isNavigationVisible = false;
    private int currentProgress = 0;
    private View loadingView; // 新增加载视图

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                if(!response.isSuccessful()) {
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
        progressBar = view.findViewById(R.id.progress_bar);
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

        // 设置WebChromeClient来处理进度
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        // 加载epub.js和电子书
        String readerHtml = "file:///android_asset/reader.html";
        webView.loadUrl(readerHtml);

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
}