package cn.zhangjh.zhiyue.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.ReadingRecord;
import cn.zhangjh.zhiyue.utils.LogUtil;
import cn.zhangjh.zhiyue.viewmodel.BookInfoViewModel;
import io.noties.markwon.Markwon;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Callback;

public class AISummaryFragment extends Fragment {
    private static final String TAG = AISummaryFragment.class.getName();
    private String userId;
    private String fileId;
    private TextView summaryText;
    private View progressLayout;
    private TextView progressPercentage;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressBar;
    private static WebSocket webSocket;
    private static boolean isWebSocketInitialized = false;
    private StringBuilder summaryContent = new StringBuilder();
    private String title, author;
    private final StringBuilder partsSummary = new StringBuilder();
    private boolean isLoadingFromHistory = false;

    public AISummaryFragment() {
    }

    public AISummaryFragment(String fileId) {
        this.fileId = fileId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_summary, container, false);
        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", MODE_PRIVATE);
        this.userId = prefs.getString("userId", "");

        summaryText = view.findViewById(R.id.ai_summary_text);
        progressLayout = view.findViewById(R.id.progress_layout);
        progressBar = view.findViewById(R.id.progress_bar);
        progressPercentage = view.findViewById(R.id.progress_percentage);

        // 先尝试从历史记录中获取总结内容
        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(fileId)) {
            loadSummaryFromHistory();
        } else {
            // 如果没有用户ID或文件ID，显示错误信息
            summaryText.setText(getString(R.string.unknown_error));
            summaryText.setVisibility(View.VISIBLE);
        }
        
        return view;
    }

    private void loadSummaryFromHistory() {
        isLoadingFromHistory = true;
        progressLayout.setVisibility(View.VISIBLE);
        summaryText.setVisibility(View.GONE);
        
        ApiClient.getBookService().getRecordDetail(userId, fileId)
            .enqueue(new Callback<>() {
	            @Override
	            public void onResponse(@NonNull Call<BizResponse<ReadingRecord>> call,
	                                   @NonNull retrofit2.Response<BizResponse<ReadingRecord>> response) {
		            if (!response.isSuccessful()) {
			            LogUtil.e(TAG, "获取记录详情失败: " + response.code());
			            initWebSocketIfNeeded();
			            return;
		            }

		            BizResponse<ReadingRecord> bizResponse = response.body();
		            if (bizResponse == null || !bizResponse.isSuccess()) {
			            String errorMsg = bizResponse != null ? bizResponse.getErrorMsg() : getString(R.string.unknown_error);
			            LogUtil.e(TAG, "获取记录详情失败: " + errorMsg);
			            initWebSocketIfNeeded();
			            return;
		            }

		            ReadingRecord record = bizResponse.getData();
		            if (record != null && !TextUtils.isEmpty(record.getSummary())) {
			            // 有历史总结，直接显示
			            progressLayout.setVisibility(View.GONE);
			            summaryText.setVisibility(View.VISIBLE);

			            summaryContent = new StringBuilder(record.getSummary());
			            Markwon markwon = Markwon.create(requireContext());
			            markwon.setMarkdown(summaryText, summaryContent.toString());

			            // 更新书籍信息
			            updateBookInfo(record.getTitle(), record.getAuthor(), record.getSummary(), record.getPartsSummary());
			            isLoadingFromHistory = false;
		            } else {
			            // 没有历史总结，通过WebSocket获取
			            initWebSocketIfNeeded();
		            }
	            }

	            @Override
	            public void onFailure(@NonNull Call<BizResponse<ReadingRecord>> call, @NonNull Throwable t) {
		            LogUtil.e(TAG, "获取记录详情失败", t);
		            initWebSocketIfNeeded();
	            }
            });
    }

    private void initWebSocketIfNeeded() {
        isLoadingFromHistory = false;
        if (!isWebSocketInitialized) {
            initWebSocket();
            isWebSocketInitialized = true;
        }
    }

    private void initWebSocket() {
        if (webSocket != null) {
            return;
        }
        // 如果没有用户ID，提示登录
        if(userId.isEmpty()) {
            progressLayout.setVisibility(View.GONE);
            summaryText.setVisibility(View.VISIBLE);
            summaryText.setText(getString(R.string.please_login_first));
            return;
        }
        String wsUrl = String.format("wss://tx.zhangjh.cn/socket/summary?userId=%s", userId);
        
        LogUtil.d(TAG, "Connecting to WebSocket: " + wsUrl);
    
        Request request = new Request.Builder()
                .url(wsUrl)
                .build();
                
        OkHttpClient client = new OkHttpClient();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                LogUtil.d(TAG, "SummaryWebSocket connection opened");
                try {
                    SharedPreferences prefs = requireActivity().getSharedPreferences("language", MODE_PRIVATE);
                    String language = prefs.getString("language", "en");
                    JSONObject message = new JSONObject();
                    message.put("fileId", fileId);
                    message.put("userId", userId);
                    message.put("language", language);
                    webSocket.send(message.toString());
                    LogUtil.d(TAG, "Sent message: " + message);
                } catch (JSONException e) {
                    LogUtil.e(TAG, "Error creating message", e);
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                LogUtil.d(TAG, "SummaryWs received message: " + text);
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
                                partsSummary.append(data);
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
                                updateBookInfo(title, author, summaryContent.toString(), partsSummary.toString());
                                closeWebSocket();
                                break;
                        }
                    } catch (JSONException e) {
                        LogUtil.e(TAG, "JSON解析错误: " + e.getMessage());
                    }
                });
            }
            
            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, 
                    @Nullable Response response) {
                LogUtil.e(TAG, "SummaryWs connection failed", t);
                if(getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), getString(R.string.error_network) + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                    progressLayout.setVisibility(View.GONE);
                    summaryText.setVisibility(View.VISIBLE);
                    summaryText.setText(getString(R.string.error_network));
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!isLoadingFromHistory) {
            summaryContent.setLength(0);
            partsSummary.setLength(0);
            updateBookInfo(title, author, "", "");
        }
        isWebSocketInitialized = false;
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

    private void updateBookInfo(String title, String author, String summary, String partsSummary) {
        viewModel.setBookInfo(title, author, summary, partsSummary);
    }

}