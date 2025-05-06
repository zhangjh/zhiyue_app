package cn.zhangjh.zhiyue.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.auth.GoogleSignInManager;

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
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            loginSuccess();
        }
    }

    private void handleGoogleSignIn() {
        // 显示加载提示
        Toast.makeText(this, getString(R.string.google_sign_in_loading), Toast.LENGTH_SHORT).show();
        
        googleSignInManager.signIn(new GoogleSignInManager.GoogleSignInCallback() {
            @Override
            public void onSuccess(String idToken, String email) {
                // 保存登录状态和用户信息
                SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
                prefs.edit()
                    .putString("idToken", idToken)
                    .putString("email", email)
                    .putBoolean("isLoggedIn", true)
                    .apply();
                
                Log.d(TAG, "Google sign in success, idToken " + idToken + ", email: " + email);
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