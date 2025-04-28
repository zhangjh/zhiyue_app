package cn.zhangjh.zhiyue.api;

import okhttp3.WebSocket;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface WebSocketService {
    @GET
    Call<String> getWebSocketUrl(@Url String url);
    
    @GET("socket/summary")
    Call<WebSocket> getSummary(@Query("fileId") String fileId, @Query("userId") String userId);
}