package cn.zhangjh.zhiyue.api;

import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.request.LoginUserRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UserService {

	@POST("/user/register")
	Call<BizResponse> register(@Body LoginUserRequest loginUserRequest);
}
