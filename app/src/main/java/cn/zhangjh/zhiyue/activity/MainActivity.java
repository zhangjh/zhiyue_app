package cn.zhangjh.zhiyue.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

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
            
            // 先设置默认导航行为
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            
            // 然后覆盖导航栏点击监听
            bottomNavigationView.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.readerFragment) {
                    // 如果直接点击阅读选项，显示提示
                    Toast.makeText(this, "请搜索你想看的书籍", Toast.LENGTH_SHORT).show();
                    return false;
                }
                // 其他选项正常导航
                return NavigationUI.onNavDestinationSelected(item, navController);
            });
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