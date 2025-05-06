package cn.zhangjh.zhiyue.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.auth.GoogleSignInManager;

public class LoginActivity extends AppCompatActivity {
    private GoogleSignInManager googleSignInManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        googleSignInManager = new GoogleSignInManager(this);
        
        MaterialButton googleSignInButton = findViewById(R.id.googleSignInButton);
        googleSignInButton.setOnClickListener(v -> handleGoogleSignIn());
    }

    private void handleGoogleSignIn() {
        googleSignInManager.signIn(new GoogleSignInManager.GoogleSignInCallback() {
            @Override
            public void onSuccess(String idToken, String email) {
                // TODO: 发送token到服务器验证
                loginSuccess();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(LoginActivity.this, 
                    getString(R.string.google_sign_in_failed), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9001) {
            googleSignInManager.handleSignInResult(data);
        }
    }

    private void loginSuccess() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}