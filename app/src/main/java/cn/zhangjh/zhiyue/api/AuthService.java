package cn.zhangjh.zhiyue.api;

import cn.zhangjh.zhiyue.model.BizResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AuthService {
    @GET("/subscribe/authCheck")
    Call<BizResponse<Boolean>> checkUserPermission(@Query("userId") String userId);
}