package cn.zhangjh.zhiyue.fragment;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import cn.zhangjh.zhiyue.BuildConfig;
import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.activity.MainActivity;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.model.Annotation;
import cn.zhangjh.zhiyue.model.BizListResponse;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.ReadingRecord;
import cn.zhangjh.zhiyue.utils.BizUtils;
import cn.zhangjh.zhiyue.utils.LogUtil;
import cn.zhangjh.zhiyue.utils.SystemUIUtils;
import cn.zhangjh.zhiyue.viewmodel.BookInfoViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderFragment extends Fragment {

    private static final String TAG = ReaderFragment.class.getName();
    private String languageRes;
    private String userId;
    private String fileId;
    private String bookId;
    private String hashId;
	private String cfi;
	private WebView webViewReader;
    private View loadingView;
    private TabLayout tabLayout;
    private boolean isBookLoading = true;

    // 防止同一个书籍重复下载
    private static boolean downloading = false;
    
    private final ReadingRecord readingRecord = new ReadingRecord();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        String language = getResources().getConfiguration().getLocales().get(0).getLanguage();
        if(!TextUtils.equals(language, "en") && !TextUtils.equals(language, "zh")) {
            language = "en";
        }
        // 读取本地i18n下资源文件，返回string内容
        String fileName = "i18n/" + language + ".json";
        languageRes = BizUtils.readAssetFile(requireContext(), fileName);
        languageRes = BizUtils.escapeJson(languageRes);

        BookInfoViewModel viewModel = new ViewModelProvider(requireActivity()).get(BookInfoViewModel.class);

        // 设置返回键监听
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                viewModel.clearBookInfo();

                // 检查当前是否在AI伴读页面
                if (tabLayout != null && tabLayout.getSelectedTabPosition() == 1) {
                    // 如果在AI伴读页面，返回到阅读器页面
                    tabLayout.selectTab(tabLayout.getTabAt(0));
                } else {
                    // 否则返回到找书页面
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToSearch();
                    }
                }
            }
        });


        // 观察数据变化
        viewModel.getSummary().observe(this, summary -> {
            // 更新总结到记录
            if(!TextUtils.isEmpty(summary)) {
                readingRecord.setUserId(userId);
                readingRecord.setFileId(fileId);
                readingRecord.setSummary(summary);
                updateReadingRecord(readingRecord);
            }
        });
        viewModel.getPartsSummary().observe(this, partsSummary -> {
            // 更新部分总结到记录
            if(!TextUtils.isEmpty(partsSummary)) {
                readingRecord.setUserId(userId);
                readingRecord.setFileId(fileId);
                readingRecord.setPartsSummary(partsSummary);
                updateReadingRecord(readingRecord);
            }
        });
    }

    private void initWebView(View view) {
        webViewReader = view.findViewById(R.id.webview_reader);
        configureWebView(webViewReader);
        loadingView = view.findViewById(R.id.loading_view);
        showLoading(getString(R.string.reader_loading));

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
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showLoading(getString(R.string.view_init));
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完成后检查是否有缓存书籍需要加载
                if (getArguments() != null) {
                    bookId = getArguments().getString("book_id");
                    hashId = getArguments().getString("hash_id");
                    fileId = getArguments().getString("file_id");
                    cfi = getArguments().getString("cfi");

                    // 清除之前的数据
                    BookInfoViewModel viewModel = new ViewModelProvider(requireActivity()).get(BookInfoViewModel.class);
                    viewModel.clearBookInfo();

                    getBookFromCache();
                }
            }
        });
        webViewReader.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                LogUtil.d(TAG, "WebView Console: " + consoleMessage.message());
                return true;
            }
        });

        // 加载epub.js和电子书
        String readerHtml = "file:///android_asset/reader.html";
        webViewReader.loadUrl(readerHtml);
    }

    private void getEbookUrl(String bookId, String hashId, Consumer<String> callback) {
        if(downloading) {
            return;
        }
        downloading = true;
        showLoading(getString(R.string.please_wait_book_loading));
        ApiClient.getBookService().downloadBook(bookId, hashId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BizResponse<String>> call, @NonNull retrofit2.Response<BizResponse<String>> response) {
                downloading = false;
                if (!response.isSuccessful()) {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        LogUtil.e(TAG, "Error response: " + errorBody);
                        showError(getString(R.string.get_ebook_url_failed) + " (" + response.code() + "): " + errorBody);
                    } catch (Exception e) {
                        LogUtil.e(TAG, "Error reading error response", e);
                        showError(getString(R.string.get_ebook_url_failed) + " (" + response.code() + ")");
                    }
                    callback.accept(null);
                    return;
                }
                if (response.body() != null && response.body().isSuccess()) {
                    String bookUrl = response.body().getData();
                    LogUtil.d(TAG, "Ebook url: " + bookUrl);
                    if (bookUrl != null) {
                        fileId = bookUrl.substring(bookUrl.lastIndexOf('/') + 1);
                        LogUtil.d(TAG, "fileId: " + fileId);
                    }
                    callback.accept(bookUrl);
                } else {
                    showError(getString(R.string.get_ebook_url_failed) + ": " + (response.body() != null ? response.body().getErrorMsg() : "Response body is null"));
                    hideLoading();
                    isBookLoading = false;
                    callback.accept(null);
                }
            }
    
            @Override
            public void onFailure(@NonNull Call<BizResponse<String>> call, @NonNull Throwable t) {
                downloading = false;
                isBookLoading = false;
                LogUtil.e(TAG, "Search failed", t);
                String errorMessage = "网络错误: " + t.getClass().getSimpleName();
                if (t.getMessage() != null) {
                    errorMessage += " - " + t.getMessage();
                }
                showError(errorMessage);
                hideLoading();
            }
        });
    }

    private void loadFromCacheBook(File cacheBook) {
        String bookPath = "file://" + cacheBook.getAbsolutePath();
        String setLangScript = String.format("setLanguageResource('%s')", languageRes);

        if (webViewReader != null) {
            webViewReader.post(() -> {
                if (webViewReader == null) return;
                webViewReader.evaluateJavascript(setLangScript, value -> {
                    if (webViewReader == null) return;
                    // 设置主题模式
                    boolean isDarkMode = isDarkModeEnabled();
                    String themeScript = String.format("setThemeMode(%s)", isDarkMode);
                    webViewReader.evaluateJavascript(themeScript, themeValue -> {
                        if (webViewReader == null) return;
                        // 语言资源和主题设置完毕后再加载书籍
                        String script = String.format("loadBook('%s', '%s')", bookPath, cfi);
                        webViewReader.evaluateJavascript(script, null);
                    });
                });
            });
        }
    }

    // 优先从cache获取书籍，没有则先下载再缓存
    private void getBookFromCache() {
        showLoading(getString(R.string.ebook_loading));

        File cacheDir = requireContext().getFilesDir();
        AtomicReference<File> cacheBook = new AtomicReference<>(new File(cacheDir, bookId + "_" + hashId + ".epub"));
        // 可以直接使用缓存的场景：缓存书籍存在且fileId不为空，否则需要重新下载
        if(cacheBook.get().exists() && !TextUtils.isEmpty(fileId)) {
            LogUtil.d(TAG, "Found book in cache: " + cacheBook.get().getAbsolutePath());
            // 传递给epub加载
            loadFromCacheBook(cacheBook.get());
        } else {
            // 下载并保存
            getEbookUrl(bookId, hashId, fileUrl -> {
                if(!TextUtils.isEmpty(fileUrl)) {
                    // 写入本地缓存
                    cacheBook.set(new File(cacheDir, bookId + "_" + hashId + ".epub"));
                    // 从fileUrl下载并缓存到cacheBook
                    new Thread(() -> {
                        try {
                            URL url = new URL(fileUrl);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            try (InputStream inputStream = connection.getInputStream()) {
                                try (FileOutputStream outputStream = new FileOutputStream(cacheBook.get())) {
                                    byte[] buffer = new byte[1024];
                                    int bytesRead;
                                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                                        outputStream.write(buffer, 0, bytesRead);
                                    }
                                }
                                LogUtil.d(TAG, "Book saved to cache: " + cacheBook.get().getAbsolutePath());
                                // 传递给epub加载
                                loadFromCacheBook(cacheBook.get());
                            }
                        } catch (Exception e) {
                            LogUtil.e(TAG, "Error downloading book", e);
                            showError(getString(R.string.error_network) + ": " + e.getMessage());
                        }
                    }).start();
                } else {
                    showError(getString(R.string.get_ebook_url_failed));
                }
            });
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
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

    // 检测是否为暗黑模式
    private boolean isDarkModeEnabled() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    // 新增配置方法
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 启用 WebView 缓存
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

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

        initWebView(view);

        View aiReadingLayout = view.findViewById(R.id.ai_reading_layout);

        // 初始化TabLayout
        tabLayout = view.findViewById(R.id.tab_layout);
        
        // 初始时禁用TabLayout
        setTabLayoutEnabled(false);
        
        // 应用主题
        applyTabTheme();
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 如果书籍正在加载中，不处理Tab切换事件
                if (isBookLoading) {
                    // 恢复到第一个Tab
                    tabLayout.selectTab(tabLayout.getTabAt(0));
                    Toast.makeText(getContext(), getString(R.string.please_wait_book_loading), Toast.LENGTH_SHORT).show();
                    return;
                }
                
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

    // 添加一个方法来设置TabLayout的启用/禁用状态
    private void setTabLayoutEnabled(boolean enabled) {
        if (tabLayout != null) {
            ViewGroup tabStrip = (ViewGroup) tabLayout.getChildAt(0);
            if (tabStrip != null) {
                for (int i = 0; i < tabStrip.getChildCount(); i++) {
                    tabStrip.getChildAt(i).setEnabled(enabled);
                }
            }
            // 视觉上的反馈
            tabLayout.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 设置全屏模式
        if (getActivity() != null) {
            FragmentActivity activity = getActivity();
            SystemUIUtils.setImmersiveMode(activity, true);
            SystemUIUtils.handleDisplayCutout(activity);
            // 确保底部导航栏隐藏
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).hideBottomNavigation();
            }
        }
    }

    private void applyTabTheme() {
        if (tabLayout != null) {
            int backgroundColor = ContextCompat.getColor(requireContext(), R.color.background);
            int textColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
            int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
            
            tabLayout.setBackgroundColor(backgroundColor);
            tabLayout.setTabTextColors(textColor, primaryColor);
            tabLayout.setSelectedTabIndicatorColor(primaryColor);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 检测主题变化并更新WebView
        if (webViewReader != null) {
            boolean isDarkMode = isDarkModeEnabled();
            String themeScript = String.format("setThemeMode(%s)", isDarkMode);
            webViewReader.post(() -> {
                if (webViewReader != null) webViewReader.evaluateJavascript(themeScript, null);
            });
        }
        
        // 更新Tab主题
        applyTabTheme();
        
        // 通知Activity更新主题
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateTheme();
        }
        
        // 更新AI伴读Fragment的主题
        FragmentManager fm = getChildFragmentManager();
        Fragment aiFragment = fm.findFragmentByTag("AIReadingFragment");
        if (aiFragment instanceof AIReadingFragment) {
            ((AIReadingFragment) aiFragment).updateTheme();
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
        // 退出前更新进度
        SharedPreferences prefs = requireActivity().getSharedPreferences("reading_progress", Context.MODE_PRIVATE);
        String progress = prefs.getString(fileId, "");
        String cfi = prefs.getString(fileId + "_cfi", "");
        readingRecord.setUserId(userId);
        readingRecord.setFileId(fileId);
        if(!TextUtils.isEmpty(progress)) {
            readingRecord.setProgress(Integer.parseInt(progress));
        }
        if(!TextUtils.isEmpty(cfi)) {
            readingRecord.setCfi(cfi);
        }
        LogUtil.d(TAG, "updateReadingRecord: " + new Gson().toJson(readingRecord));
        updateReadingRecord(readingRecord);
    }

    // JavaScript接口类
    private class WebAppInterface {
        @JavascriptInterface
        public void onBookMetadata(String title, String author) {
            LogUtil.d(TAG, "onBookMetaData update: " + title + ", author: " + author);
            // 更新记录，先与onBookLoaded执行
            readingRecord.setFileId(fileId);
            readingRecord.setUserId(userId);
            readingRecord.setBookId(bookId);
            readingRecord.setHashId(hashId);
            readingRecord.setTitle(title);
            readingRecord.setAuthor(author);
            // 保存阅读记录
            saveReadingRecord();
        }

        @JavascriptInterface
        public void onBookLoaded() {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideLoading();
                    // 书籍加载完成，更新状态并启用TabLayout
                    isBookLoading = false;
                    setTabLayoutEnabled(true);
                });
            }
        }

        @JavascriptInterface
        public void copyText(String text) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("selected_text", text);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getActivity(), getString(R.string.copy_to_clipboard_success), Toast.LENGTH_SHORT).show();
                });
            }
        }

        @JavascriptInterface
        public void onProgressUpdate(int progress, String cfi) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // 暂存进度到本地
                    SharedPreferences prefs = getActivity().getSharedPreferences("reading_progress", Context.MODE_PRIVATE);
                    prefs.edit().putString(fileId, String.valueOf(progress)).apply();
                    prefs.edit().putString(fileId + "_cfi", cfi).apply();
                });
            }
        }

        @JavascriptInterface
        public void onLoadError(String error) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
                // 隐藏加载提示
                hideLoading();
                isBookLoading = false;
            });
        }

        @JavascriptInterface
        public void saveAnnotation(String cfi, String type, String color, String text) {
            if (getActivity() != null) {
                Annotation annotation = new Annotation(fileId, cfi, type, color, text);
                annotation.setUserId(userId);
                LogUtil.d(TAG, "Saving annotation: " + new Gson().toJson(annotation));
                // 调用API保存标注
                ApiClient.getBookService().saveAnnotation(annotation).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<BizResponse<Void>> call, @NonNull retrofit2.Response<BizResponse<Void>> response) {
                        if (!response.isSuccessful()) {
                            showError(getString(R.string.save_annotation_failed));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
                        showError(getString(R.string.save_annotation_failed) + ": " + t.getMessage());
                    }
                });
            }
        }

        @JavascriptInterface
        public void loadAnnotations() {
            LogUtil.d(TAG, "loadAnnotations called, fileId: " + fileId);
            if (getActivity() != null) {
                // 从服务器获取标注
                ApiClient.getBookService().getAnnotations(userId, fileId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<BizListResponse<Annotation>> call, @NonNull Response<BizListResponse<Annotation>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // 将标注传递给前端
                            String annotations = new Gson().toJson(response.body().getData());
                            LogUtil.d(TAG, "Annotations: " + annotations);
                            if(TextUtils.isEmpty(annotations)) {
                                return;
                            }
                            // 确保JSON字符串正确转义
                            annotations = BizUtils.escapeJson(annotations);
                            // 使用单引号包裹整个JSON字符串，避免双引号冲突
                            String script = String.format("loadAnnotations('%s')", annotations);
                            if (webViewReader != null) {
                                webViewReader.post(() -> {
                                    if (webViewReader != null) webViewReader.evaluateJavascript(script, null);
                                });
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<BizListResponse<Annotation>> call, @NonNull Throwable t) {
                        showError(getString(R.string.load_annotation_failed) + ": " + t.getMessage());
                    }
                });
            }
        }

        @JavascriptInterface
        public void deleteAnnotation(String cfi, String text) {
            if (TextUtils.isEmpty(cfi) || TextUtils.isEmpty(userId) || TextUtils.isEmpty(text)) {
                return;
            }
            
            // 调用API删除标注
            Annotation annotation = new Annotation();
            annotation.setUserId(userId);
            annotation.setFileId(fileId);
            annotation.setCfi(cfi);
            annotation.setText(text);
            ApiClient.getBookService().deleteAnnotation(annotation).enqueue(new Callback<>() {
	            @Override
	            public void onResponse(@NonNull Call<BizResponse<Void>> call, @NonNull Response<BizResponse<Void>> response) {
		            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
			            LogUtil.d(TAG, "标注删除成功: " + cfi);
		            } else {
			            LogUtil.e(TAG, "标注删除失败: " + cfi);
		            }
	            }

	            @Override
	            public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
		            LogUtil.e(TAG, "标注删除请求失败", t);
	            }
            });
        }

        @JavascriptInterface
        public void onThemeChanged(String theme) {
            LogUtil.d(TAG, "Theme changed to: " + theme);
        }
    }

    // 添加保存阅读记录的方法
    private void saveReadingRecord() {
        if (getActivity() == null || TextUtils.isEmpty(fileId)) {
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
                        LogUtil.e(TAG, "Save reading record failed: " + response.code());
                        return;
                    }
                    BizResponse<Void> bizResponse = response.body();
                    if (bizResponse != null && bizResponse.isSuccess()) {
                        LogUtil.d(TAG, "Save reading record success");
                    } else {
                        String errorMsg = bizResponse != null ? bizResponse.getErrorMsg() : "unknown error";
                        LogUtil.e(TAG, "Save reading record failed: " + errorMsg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
                    LogUtil.e(TAG, "Save reading record failed", t);
                }
            });
    }

    // 更新阅读记录
    private void updateReadingRecord(ReadingRecord readingRecord) {
        String userId = readingRecord.getUserId();
        if(TextUtils.isEmpty(userId)) {
            LogUtil.d(TAG, "Update reading record failed: userId is empty");
            return;
        }
        // 如果没有实质要更新的内容就不更新
        if(TextUtils.isEmpty(readingRecord.getTitle()) && TextUtils.isEmpty(readingRecord.getAuthor())
            && TextUtils.isEmpty(readingRecord.getSummary()) && TextUtils.isEmpty(readingRecord.getPartsSummary())
            && readingRecord.getProgress() == null && TextUtils.isEmpty(readingRecord.getCfi())
            && TextUtils.isEmpty(readingRecord.getHashId())) {
            return;
        }
        ApiClient.getBookService().updateRecord(readingRecord)
            .enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BizResponse<Void>> call,
                                       @NonNull Response<BizResponse<Void>> response) {
                    if (!response.isSuccessful()) {
                        LogUtil.e(TAG, "Update reading record failed: " + response.code());
                        return;
                    }
                    BizResponse<Void> bizResponse = response.body();
                    if (bizResponse != null && bizResponse.isSuccess()) {
                        LogUtil.d(TAG, "Update reading record success");
                    } else {
                        String errorMsg = bizResponse != null ? bizResponse.getErrorMsg() : "unknown error";
                        LogUtil.e(TAG, "Update reading record failed: " + errorMsg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BizResponse<Void>> call, @NonNull Throwable t) {
                    LogUtil.e(TAG, "Update reading record failed", t);
                }
            });
    }
}