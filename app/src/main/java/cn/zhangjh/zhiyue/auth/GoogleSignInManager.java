package cn.zhangjh.zhiyue.auth;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.model.GoogleUser;
import cn.zhangjh.zhiyue.utils.LogUtil;

public class GoogleSignInManager {
    private static final String TAG = "GoogleSignInManager";
    private static final int RC_SIGN_IN = 9001;
    
    private final GoogleSignInClient mGoogleSignInClient;
    private final FragmentActivity activity;
    private GoogleSignInCallback callback;
    
    public GoogleSignInManager(FragmentActivity activity, String webClientId) {
        this.activity = activity;
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }
    
    public void signIn(GoogleSignInCallback callback) {
        this.callback = callback;
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    
    public void handleSignInResult(Intent data) {
        if (data == null) {
            LogUtil.e(TAG, "handleSignInResult: data is null");
            if (callback != null) {
                callback.onFailure(activity.getString(R.string.google_sign_in_failed));
            }
            return;
        }
    
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String idToken = account.getIdToken();
            String email = account.getEmail();
            String userId = account.getId();
            Uri photoUrl = account.getPhotoUrl();
            String name = account.getDisplayName();
            if (idToken == null || email == null) {
                LogUtil.e(TAG, "handleSignInResult: idToken or email is null");
                if (callback != null) {
                    callback.onFailure(activity.getString(R.string.google_sign_in_failed));
                }
                return;
            }
            GoogleUser googleUser = new GoogleUser();
            googleUser.setIdToken(idToken);
            googleUser.setUserId(userId);
            googleUser.setEmail(email);
            googleUser.setName(name);
            if(photoUrl != null) {
                googleUser.setAvatar(photoUrl.toString());
            }
            if (callback != null) {
                callback.onSuccess(googleUser);
            }
        } catch (ApiException e) {
            // 添加更详细的错误日志
            LogUtil.e(TAG, "signInResult:failed code=" + e.getStatusCode() + " message=" + e.getMessage());
            String errorMessage;
            switch (e.getStatusCode()) {
                case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                    errorMessage = activity.getString(R.string.google_sign_in_error_cancelled);
                    break;
                case GoogleSignInStatusCodes.NETWORK_ERROR:
                    errorMessage = activity.getString(R.string.error_network);
                    break;
                case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                    errorMessage = activity.getString(R.string.google_sign_in_error_invalid_account);
                    break;
                case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                    errorMessage = activity.getString(R.string.google_sign_in_error_sign_in_required);;
                    break;
                default:
                    errorMessage = activity.getString(R.string.google_sign_in_failed) + e.getMessage();
                    break;
            }
            if (callback != null) {
                callback.onFailure(errorMessage);
            }
        }
    }
    
    public void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(activity, task -> {
                    // 登出成功
                });
    }
    
    public interface GoogleSignInCallback {
        void onSuccess(GoogleUser googleUser);
        void onFailure(String error);
    }
}