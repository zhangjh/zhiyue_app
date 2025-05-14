package cn.zhangjh.zhiyue.api;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://tx.zhangjh.cn/";
    private static final String TAG = "ApiClient";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            try {
                // 创建详细的日志拦截器
                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                    // 为所有网络相关日志添加自定义标签
                    if (message.startsWith("-->")) {
                        Log.d(TAG, "Sending request: " + message);
                    } else if (message.startsWith("<--")) {
                        Log.d(TAG, "Received response: " + message);
                    } else if (message.startsWith("DNSResult:")) {
                        Log.d(TAG, "DNS Resolution: " + message);
                    } else {
                        Log.d(TAG, "Network Log: " + message);
                    }
                });
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                // 创建 OkHttpClient 并添加详细配置
                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build();

                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                
                Log.d(TAG, "Created new Retrofit instance with base URL: " + BASE_URL);
            } catch (Exception e) {
                Log.e(TAG, "Error creating Retrofit instance", e);
            }
        }
        return retrofit;
    }

    public static BookService getBookService() {
        BookService service = getClient().create(BookService.class);
        Log.d(TAG, "Created BookService instance");
        return service;
    }

    public static UserService getUserService() {
        UserService service = getClient().create(UserService.class);
        Log.d(TAG, "Created UserService instance");
        return service;
    }

    public static AuthService getAuthService() {
        return getClient().create(AuthService.class);
    }
}