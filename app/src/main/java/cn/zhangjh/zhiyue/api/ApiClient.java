package cn.zhangjh.zhiyue.api;

import java.util.concurrent.TimeUnit;

import cn.zhangjh.zhiyue.utils.LogUtil;
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
                HttpLoggingInterceptor loggingInterceptor =
                        new HttpLoggingInterceptor(message -> LogUtil.d(TAG, "Network Log: " + message));
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                // 创建 OkHttpClient 并添加详细配置
                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(300, TimeUnit.SECONDS)
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
                
                LogUtil.d(TAG, "Created new Retrofit instance with base URL: " + BASE_URL);
            } catch (Exception e) {
                LogUtil.e(TAG, "Error creating Retrofit instance", e);
            }
        }
        return retrofit;
    }

    public static BookService getBookService() {
        BookService service = getClient().create(BookService.class);
        LogUtil.d(TAG, "Created BookService instance");
        return service;
    }

    public static UserService getUserService() {
        UserService service = getClient().create(UserService.class);
        LogUtil.d(TAG, "Created UserService instance");
        return service;
    }
}