package cn.zhangjh.zhiyue;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.model.Annotation;
import cn.zhangjh.zhiyue.model.BizListResponse;
import cn.zhangjh.zhiyue.model.BizResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderFragment extends Fragment {

    private static final String TAG = ReaderFragment.class.getName();
    private String fileId;
    private String bookUrl;
    private String bookId;
    private WebView webViewReader;
    private boolean isNavigationVisible = false;
    private int currentProgress = 0;
    private View loadingView;

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
            bookId = getArguments().getString("book_id");  // 保存bookId
            String hashId = getArguments().getString("hash_id");
            Log.d("ReaderFragment", "Received bookId: " + bookId + ", hashId: " + hashId);
            // 获取书籍url
            // todo: 阅读记录里已有则不重复下载
//            showError("该书籍已下载请从阅读记录继续阅读");
//            getEbookUrl(bookId, hashId);

            // for test only
            fileId = "一句顶一万句 (刘震云) (Z-Library).epub";
            bookUrl = getString(R.string.biz_domain) + fileId;
        }
    }

    private void getEbookUrl(String bookId, String hashId) {
        ApiClient.getBookService().downloadBook(bookId, hashId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BizResponse<String>> call, @NonNull retrofit2.Response<BizResponse<String>> response) {
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
            public void onFailure(@NonNull Call<BizResponse<String>> call, @NonNull Throwable t) {
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

    // 新增配置方法
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 通用安全配置
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        webView.setLongClickable(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reader, container, false);

        // 删除思维导图相关初始化
        webViewReader = view.findViewById(R.id.webview_reader);
        configureWebView(webViewReader);
        loadingView = view.findViewById(R.id.loading_view);

        showLoading("正在加载阅读器...");

        // 添加以下配置阻止系统菜单
        webViewReader.setLongClickable(false);  // 禁用系统长按菜单
        webViewReader.setOnLongClickListener(v -> {
            // 允许选择但阻止系统菜单
            return false;
        });

        // 添加JavaScript接口
        webViewReader.addJavascriptInterface(new WebAppInterface(), "Android");

        // 设置WebViewClient
        webViewReader.setWebViewClient(new WebViewClient() {
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
                    webViewReader.evaluateJavascript("loadBook('" + bookUrl + "')", null);
                }
            }
        });
        webViewReader.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(TAG, "WebView Console: " + consoleMessage.message());
                return true;
            }
        });

        // 加载epub.js和电子书
        String readerHtml = "file:///android_asset/reader.html";
        webViewReader.loadUrl(readerHtml);

        View aiReadingLayout = view.findViewById(R.id.ai_reading_layout);

        // 初始化TabLayout
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // 阅读器
                        webViewReader.setVisibility(View.VISIBLE);
                        aiReadingLayout.setVisibility(View.GONE);
                        tabLayout.setVisibility(View.VISIBLE); // 显示一级Tab
                        break;
                    case 1: // AI伴读
                        webViewReader.setVisibility(View.GONE);
                        aiReadingLayout.setVisibility(View.VISIBLE);
                        tabLayout.setVisibility(View.GONE); // 隐藏一级Tab
                        // 复用Fragment实例
                        FragmentManager fm = getChildFragmentManager();
                        Fragment fragment = fm.findFragmentByTag("AIReadingFragment");
                        FragmentTransaction ft = fm.beginTransaction();
                        if (fragment == null) {
                            fragment = new AIReadingFragment(fileId);
                            ft.add(R.id.ai_reading_layout, fragment, "AIReadingFragment");
                        } else {
                            ft.show(fragment);
                        }
                        ft.commit();
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 设置全屏模式
        if (getActivity() != null) {
            getActivity().getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }

    @Override
    public void onDestroyView() {
        if (webViewReader != null) {
            webViewReader.destroy();
            webViewReader = null;
        }
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBottomNavigation();
        }
    }

    // JavaScript接口类
    private class WebAppInterface {
        @JavascriptInterface
        public void onBookLoaded() {
            hideLoading();
            // 保存阅读记录
            saveReadingRecord();
        }

        @JavascriptInterface
        public void copyText(String text) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("selected_text", text);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getActivity(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                });
            }
        }

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
        public void onLoadError(String error) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
                // 隐藏加载提示
                hideLoading();
            });
        }

        @JavascriptInterface
        public void saveAnnotation(String cfiRange, String type, String color, String text) {
            if (getActivity() != null) {
                Annotation annotation = new Annotation(bookId, cfiRange, type, color, text);
                Log.d(TAG, "Saving annotation: " + new Gson().toJson(annotation));
                // 调用API保存标注
                ApiClient.getBookService().saveAnnotation(annotation).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<BizResponse<Void>> call, @NonNull retrofit2.Response<BizResponse<Void>> response) {
                        if (!response.isSuccessful()) {
                            showError("保存标注失败");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
                        showError("保存标注失败: " + t.getMessage());
                    }
                });
            }
        }

        @JavascriptInterface
        public void loadAnnotations() {
            Log.d(TAG, "loadAnnotations called, bookId: " + bookId);
            if (getActivity() != null) {
                // 从服务器获取标注
                ApiClient.getBookService().getAnnotations(bookId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<BizListResponse<Annotation>> call, @NonNull Response<BizListResponse<Annotation>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // 将标注传递给前端
                            String annotations = new Gson().toJson(response.body());
                            // mock data
//			                String annotations = new Gson().toJson("[{\"bookId\":\"1\",\"cfiRange\":\"epubcfi(/6/8!/4/10,/1:0,/1:36)\",\"color\":\"rgba(255,255,0,0.3)\",\"text\":\"在《三体》电子书与读者见面之际，再次感谢广大读者的关注和支持，谢谢大家！\",\"timestamp\":1745821203806,\"type\":\"highlight\"}]");
                            webViewReader.post(() -> webViewReader.evaluateJavascript(
                                    "window.loadAnnotations(" + annotations + ")",
                                    null
                            ));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<BizListResponse<Annotation>> call, @NonNull Throwable t) {
                        showError("加载标注失败: " + t.getMessage());
                    }
                });
            }
        }
    }

    // 添加保存阅读记录的方法
    private void saveReadingRecord() {
        if (getActivity() == null || fileId == null) {
            return;
        }

        // 获取用户ID
        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        if (TextUtils.isEmpty(userId)) {
            return;
        }

        // 调用保存记录接口
        ApiClient.getBookService().saveRecord(userId, fileId)
            .enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BizResponse<Void>> call,
                                       @NonNull Response<BizResponse<Void>> response) {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Save reading record failed: " + response.code());
                        return;
                    }
                    BizResponse<Void> bizResponse = response.body();
                    if (bizResponse != null && bizResponse.isSuccess()) {
                        Log.d(TAG, "Save reading record success");
                    } else {
                        String errorMsg = bizResponse != null ? bizResponse.getErrorMsg() : "unknown error";
                        Log.e(TAG, "Save reading record failed: " + errorMsg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Save reading record failed", t);
                }
            });
    }
}