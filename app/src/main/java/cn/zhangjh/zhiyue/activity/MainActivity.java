package cn.zhangjh.zhiyue.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.auth.GoogleSignInManager;
import cn.zhangjh.zhiyue.fragment.AISummaryFragment;
import cn.zhangjh.zhiyue.fragment.MindMapFragment;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private NavController navController;
    private String mode = "init";

    private GoogleSignInManager googleSignInManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mode = "init";

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // 先设置默认导航行为
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            
            // 然后覆盖导航栏点击监听
             bottomNavigationView.setOnItemSelectedListener(item -> {
                 // 首页直接点击阅读器菜单项
                 if (item.getItemId() == R.id.readerFragment && Objects.equals(mode, "init")) {
                     // 如果直接点击阅读选项，显示提示
                     Toast.makeText(this, "请搜索你想看的书籍", Toast.LENGTH_SHORT).show();
                     return false;
                 }
                 // 其他选项正常导航
                 return NavigationUI.onNavDestinationSelected(item, navController);
             });
        }
        // for test only
        navigateToReader("1", "2");
    }

    // 隐藏底部导航栏
    public void hideBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    // 显示底部导航栏
    public void showBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
    }

    // 切换到阅读器页面
    public void navigateToReader(String bookId, String hashId) {
        if (navController != null) {
            // 设置跳转阅读器场景的标识符
            mode = "search";
            // 切换到阅读器页面（底部导航第二个选项）
            bottomNavigationView.setSelectedItemId(R.id.readerFragment);
            // 隐藏底部导航栏
            hideBottomNavigation();
            
            Bundle args = new Bundle();
            args.putString("book_id", bookId);
            args.putString("hash_id", hashId);
            navController.navigate(R.id.readerFragment, args);
        }
    }

    public void navigateToSearch() {
        // 切换到找书页面
        bottomNavigationView.setSelectedItemId(R.id.booksFragment);
    }

    @Override
    protected void onDestroy() {
        AISummaryFragment.closeWebSocket();
        MindMapFragment.closeWebSocket();
        super.onDestroy();
    }

    public void startGoogleSignIn() {
        googleSignInManager.signIn(new GoogleSignInManager.GoogleSignInCallback() {
            @Override
            public void onSuccess(String idToken, String email) {
                // TODO: 发送token到服务器验证
                Toast.makeText(MainActivity.this, 
                    getString(R.string.google_sign_in_success), 
                    Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(MainActivity.this, 
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
}