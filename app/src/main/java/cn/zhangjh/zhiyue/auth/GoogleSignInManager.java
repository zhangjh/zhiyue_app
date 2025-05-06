package cn.zhangjh.zhiyue.auth;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class GoogleSignInManager {
    private static final int RC_SIGN_IN = 9001;
    private final GoogleSignInClient mGoogleSignInClient;
    private final Activity mActivity;
    private GoogleSignInCallback mCallback;

    public interface GoogleSignInCallback {
        void onSuccess(String idToken, String email);
        void onFailure(String error);
    }

    public GoogleSignInManager(Activity activity) {
        mActivity = activity;
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("YOUR_WEB_CLIENT_ID") // 从Google Cloud Console获取
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public void signIn(GoogleSignInCallback callback) {
        mCallback = callback;
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        mActivity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String idToken = account.getIdToken();
            String email = account.getEmail();
            if (mCallback != null) {
                mCallback.onSuccess(idToken, email);
            }
        } catch (ApiException e) {
            if (mCallback != null) {
                mCallback.onFailure(e.getMessage());
            }
        }
    }

    public void signOut() {
        mGoogleSignInClient.signOut();
    }
}