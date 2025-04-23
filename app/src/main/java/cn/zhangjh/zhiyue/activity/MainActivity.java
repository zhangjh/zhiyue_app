package cn.zhangjh.zhiyue.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import cn.zhangjh.zhiyue.R;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
        }
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
    public void navigateToReader(String bookId) {
        if (navController != null) {
            // 切换到阅读器页面（底部导航第二个选项）
            bottomNavigationView.setSelectedItemId(R.id.readerFragment);
            // 隐藏底部导航栏
            hideBottomNavigation();
            
            // TODO: 传递书籍ID到阅读器页面
            Bundle args = new Bundle();
            args.putString("book_id", bookId);
            navController.navigate(R.id.readerFragment, args);
        }
    }
}