package cn.zhangjh.zhiyue.fragment;

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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.model.Annotation;
import cn.zhangjh.zhiyue.model.BizListResponse;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.ReadingRecord;
import cn.zhangjh.zhiyue.viewmodel.BookInfoViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderFragment extends Fragment {

    private static final String TAG = ReaderFragment.class.getName();
    private String userId;
    private String fileId;
    private String hashId;
    private String cfi;
    private String bookUrl;
	private WebView webViewReader;
    private View loadingView;

    private final ReadingRecord readingRecord = new ReadingRecord();

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
            hashId = getArguments().getString("hash_id");
            fileId = getArguments().getString("file_id");
            cfi = getArguments().getString("cfi");
            // for test only
//            fileId = "一句顶一万句 (刘震云) (Z-Library).epub";
            if(fileId == null || fileId.isEmpty()) {
                // 获取书籍url, 阅读记录里已有则不重复下载
                 getEbookUrl(bookId, hashId);
            } else {
                bookUrl = getString(R.string.biz_domain) + fileId;
            }
        }
        BookInfoViewModel viewModel = new ViewModelProvider(requireActivity()).get(BookInfoViewModel.class);

        // 观察数据变化
        viewModel.getSummary().observe(this, summary -> {
            // 更新总结到记录
            readingRecord.setUserId(userId);
            readingRecord.setFileId(fileId);
            readingRecord.setSummary(summary);

            updateReadingRecord(readingRecord);
        });
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
                    fileId = bookUrl.substring(bookUrl.lastIndexOf('/') + 1);
                    Log.d(TAG, "fileId: " + fileId);
                    // 需要在这里触发加载
                    if (webViewReader != null) {
                        webViewReader.post(() -> {
                            String script = String.format("loadBook('%s', '%s')", bookUrl, cfi);
                            webViewReader.evaluateJavascript(script, null);
                        });
                    }
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reader, container, false);
        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        this.userId = prefs.getString("userId", "");

        // 删除思维导图相关初始化
        webViewReader = view.findViewById(R.id.webview_reader);
        configureWebView(webViewReader);
        loadingView = view.findViewById(R.id.loading_view);

        showLoading("正在加载阅读器...");

        // 添加以下配置阻止系统菜单
        webViewReader.setLongClickable(false);  // 禁用系统长按菜单
        webViewReader.setHapticFeedbackEnabled(true);  // 启用触觉反馈
        webViewReader.setOnLongClickListener(v -> {
            // 返回true表示我们已经处理了长按事件，阻止系统菜单
            return true;
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
                if (!TextUtils.isEmpty(bookUrl)) {
                    showLoading("正在加载电子书...");
                    String script = String.format("loadBook('%s', '%s')", bookUrl, cfi);
                    webViewReader.evaluateJavascript(script, null);
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
            // 确保底部导航栏隐藏
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).hideBottomNavigation();
            }
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
        public void onBookMetadata(String title, String author) {
            Log.d(TAG, "onBookMetaData update: " + title + ", author: " + author);
            // 更新记录，先与onBookLoaded执行
            readingRecord.setFileId(fileId);
            readingRecord.setUserId(userId);
            readingRecord.setTitle(title);
            readingRecord.setAuthor(author);
        }

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
        public void onProgressUpdate(int progress, String cfi) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // 这里可以保存进度到本地存储
                    readingRecord.setUserId(userId);
                    readingRecord.setFileId(fileId);
                    readingRecord.setProgress(progress);
                    readingRecord.setCfi(cfi);
                    updateReadingRecord(readingRecord);
                });
            }
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
        public void saveAnnotation(String cfi, String type, String color, String text) {
            if (getActivity() != null) {
                Annotation annotation = new Annotation(fileId, cfi, type, color, text);
                annotation.setUserId(userId);
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
            Log.d(TAG, "loadAnnotations called, fileId: " + fileId);
            if (getActivity() != null) {
                // 从服务器获取标注
                ApiClient.getBookService().getAnnotations(userId, fileId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<BizListResponse<Annotation>> call, @NonNull Response<BizListResponse<Annotation>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                // 将标注传递给前端
                                String annotations = new Gson().toJson(response.body().getData());
                                Log.d(TAG, "Annotations: " + annotations);
                                if(TextUtils.isEmpty(annotations)) {
                                    return;
                                }
                                // 确保JSON字符串正确转义
                                annotations = annotations.replace("\n", "\\n")
                                                       .replace("\r", "\\r")
                                                       .replace("\t", "\\t")
                                                       .replace("\\", "\\\\")
                                                       .replace("\"", "\\\"");
                                // 使用单引号包裹整个JSON字符串，避免双引号冲突
                                String script = String.format("loadAnnotations('%s')", annotations);
                                webViewReader.post(() -> webViewReader.evaluateJavascript(script, null));
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing annotations", e);
                                showError("处理标注数据失败: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<BizListResponse<Annotation>> call, @NonNull Throwable t) {
                        showError("加载标注失败: " + t.getMessage());
                    }
                });
            }
        }

        @JavascriptInterface
        public void deleteAnnotation(String annotationText) {

            ApiClient.getBookService().deleteAnnotation(annotationText)
                .enqueue(new Callback<>() {
	                @Override
	                public void onResponse(@NonNull Call<BizResponse<Void>> call, @NonNull Response<BizResponse<Void>> response) {
		                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
			                Log.d(TAG, "标注删除成功");
		                } else {
			                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show());
		                }
	                }

	                @Override
	                public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
		                Log.e(TAG, "标注删除请求失败", t);
		                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "网络错误，删除失败", Toast.LENGTH_SHORT).show());
	                }
                });
        }
    }

    // 添加保存阅读记录的方法
    private void saveReadingRecord() {
        if (getActivity() == null || fileId == null) {
            return;
        }
        if (TextUtils.isEmpty(userId)) {
            return;
        }

        // 调用保存记录接口
        ApiClient.getBookService().saveRecord(readingRecord)
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

    // 更新阅读记录
    private void updateReadingRecord(ReadingRecord readingRecord) {
        String userId = readingRecord.getUserId();
        if(TextUtils.isEmpty(userId)) {
            Log.d(TAG, "Update reading record failed: userId is empty");
            return;
        }
        ApiClient.getBookService().updateRecord(readingRecord)
            .enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BizResponse<Void>> call,
                                       @NonNull Response<BizResponse<Void>> response) {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Update reading record failed: " + response.code());
                        return;
                    }
                    BizResponse<Void> bizResponse = response.body();
                    if (bizResponse != null && bizResponse.isSuccess()) {
                        Log.d(TAG, "Update reading record success");
                    } else {
                        String errorMsg = bizResponse != null ? bizResponse.getErrorMsg() : "unknown error";
                        Log.e(TAG, "Update reading record failed: " + errorMsg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Update reading record failed", t);
                }
            });
    }
}