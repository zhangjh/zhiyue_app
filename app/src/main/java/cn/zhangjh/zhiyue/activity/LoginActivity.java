package cn.zhangjh.zhiyue.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.api.ApiClient;
import cn.zhangjh.zhiyue.auth.GoogleSignInManager;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.GoogleUser;
import cn.zhangjh.zhiyue.model.LoginUser;
import cn.zhangjh.zhiyue.request.LoginUserRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getName();
    private GoogleSignInManager googleSignInManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // 初始化Google登录
        String webClientId = getString(R.string.web_client_id);
        googleSignInManager = new GoogleSignInManager(this, webClientId);
        
        MaterialButton googleSignInButton = findViewById(R.id.googleSignInButton);
        googleSignInButton.setOnClickListener(v -> handleGoogleSignIn());

        // 检查是否已登录
        checkLoginState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9001) {
            googleSignInManager.handleSignInResult(data);
        }
    }

    // 添加检查登录状态的方法
    private void checkLoginState() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && !account.isExpired()) {
            // 账号存在且未过期
            loginSuccess();
        } else {
            // 清除本地登录状态
            SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
            prefs.edit()
                .putBoolean("isLoggedIn", false)
                .remove("idToken")
                .apply();
        }
    }

    private void handleGoogleSignIn() {
        // 显示加载提示
        Toast.makeText(this, getString(R.string.google_sign_in_loading), Toast.LENGTH_LONG).show();
        
        googleSignInManager.signIn(new GoogleSignInManager.GoogleSignInCallback() {
            @Override
            public void onSuccess(GoogleUser googleUser) {
                // 保存登录状态和用户信息
                SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
                prefs.edit()
                    .putString("idToken", googleUser.getIdToken())
                    .putString("email", googleUser.getEmail())
                    .putString("userId", googleUser.getUserId())
                    .putString("name", googleUser.getName())
                    .putString("avatar", googleUser.getAvatar())
                    .putBoolean("isLoggedIn", true)
                    .apply();

                // 保存用户信息到服务端
                LoginUserRequest loginUserRequest = new LoginUserRequest();
                loginUserRequest.setExtId(googleUser.getUserId());
                loginUserRequest.setUserName(googleUser.getName());
                loginUserRequest.setAvatar(googleUser.getAvatar());
                loginUserRequest.setEmail(googleUser.getEmail());
                ApiClient.getUserService().register(loginUserRequest)
                        .enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<BizResponse<LoginUser>> call, @NonNull Response<BizResponse<LoginUser>> response) {
                                if (!response.isSuccessful()) {
                                    Log.e(TAG, "Register user failed: " + response.code());
                                    return;
                                }
                                BizResponse<LoginUser> bizResponse = response.body();
                                if (bizResponse != null && bizResponse.isSuccess()) {
                                    Log.d(TAG, "Register user success ");
                                } else {
                                    String errorMsg = (bizResponse != null ? bizResponse.getErrorMsg() : "unknown error");
                                    Log.e(TAG, "Register user failed: " + errorMsg);
                                    Toast.makeText(LoginActivity.this,
                                            errorMsg, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<BizResponse<LoginUser>> call, @NonNull Throwable t) {
                                Log.e(TAG, "Register user failed", t);
                            }
                        });
                
                Log.d(TAG, "Google sign in success, loginUser: " + new Gson().toJson(googleUser));
                loginSuccess();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Google sign in failed: " + error);
                // 根据错误类型显示不同的错误提示
                if (error.contains("网络连接失败")) {
                    Toast.makeText(LoginActivity.this,
                        getString(R.string.google_sign_in_error_network), 
                        Toast.LENGTH_SHORT).show();
                } else if (error.contains("登录已取消")) {
                    Toast.makeText(LoginActivity.this,
                        getString(R.string.google_sign_in_error_cancelled), 
                        Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this,
                        getString(R.string.google_sign_in_error_unknown), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loginSuccess() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}