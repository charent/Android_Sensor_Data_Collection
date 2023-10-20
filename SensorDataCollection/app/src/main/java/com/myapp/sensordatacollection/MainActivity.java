package com.myapp.sensordatacollection;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.myapp.sensordatacollection.activities.AboutActivity;
import com.myapp.sensordatacollection.activities.SettingsActivity;
import com.myapp.sensordatacollection.databinding.ActivityMainBinding;
import com.myapp.sensordatacollection.ui.home.HomeViewModel;
import com.myapp.sensordatacollection.utils.PropertiesUtils;

import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN_ACTIVITY";
    public static final String PROPERTIES_FILE_NAME = "settings.properties";

    private ActivityMainBinding binding;
    private int settingRequestCode = 101;
    public static Properties properties;
    private NavController navController;
    private HomeViewModel homeViewModel;
    public static String SERVER_RC_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        initView();

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.setLiveProperties(properties);

    }

    private void initView(){
        properties = PropertiesUtils.getUserProperties(getApplicationContext(), PROPERTIES_FILE_NAME);
        initServerURL();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_recognition, R.id.navigation_home, R.id.navigation_dashboard)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    public  void initServerURL(){
        int default_server = Integer.parseInt(properties.getProperty("default_server"));
        switch (default_server){
            case 0:
                //主服务器
                SERVER_RC_URL = properties.getProperty("main_host") + ":" + properties.getProperty("main_port") + properties.getProperty("main_function");
                break;
            case 1:
                //自定义服务器
                SERVER_RC_URL = properties.getProperty("self_host") + ":" + properties.getProperty("self_port") + properties.getProperty("self_function");
                break;
            default:
                break;
        }
//        Log.d(TAG, SERVER_RC_URL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_nav_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()){
            case R.id.server_setting:
                intent = new Intent(this, SettingsActivity.class);
                Bundle bundle = new Bundle();
//                bundle.putSerializable("properties", properties);
                bundle.putString("properties_file_name", PROPERTIES_FILE_NAME);
                intent.putExtra("bundle", bundle);
                startActivityForResult(intent, settingRequestCode);
                break;

            case R.id.about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Log.d(TAG,String.valueOf(requestCode) + " " + String.valueOf(resultCode));
        if (requestCode == settingRequestCode && resultCode == RESULT_OK && data != null){
            Bundle bundle = data.getExtras();
            boolean isSettingChange = bundle.getBoolean("isSettingChange");
            if (isSettingChange){
                properties = PropertiesUtils.getUserProperties(getApplicationContext(), PROPERTIES_FILE_NAME);
                homeViewModel.setLiveProperties(properties);
//                Log.d(TAG, properties.getProperty("sample_time"));
                initServerURL();
                Intent intent= new Intent("android.intent.action.CART_BROADCAST");
                intent.putExtra("cmd","refresh");
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                sendBroadcast(intent);
            }
        }
    }
}