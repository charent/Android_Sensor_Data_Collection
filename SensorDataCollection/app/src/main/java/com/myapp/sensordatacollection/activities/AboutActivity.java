package com.myapp.sensordatacollection.activities;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.myapp.sensordatacollection.MainActivity;
import com.myapp.sensordatacollection.R;
import com.myapp.sensordatacollection.utils.HttpUtils;
import com.myapp.sensordatacollection.utils.ToastUtils;
import com.myapp.sensordatacollection.utils.VerfyPremission;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import okhttp3.Response;

public class AboutActivity extends AppCompatActivity {

    private static final String TAG = "AboutActivity";
    private static final int DOWNLOAD_OK = 1002;
    private static final int DOWNLOAD_FAIL = 1003;
    private Button download_Button;
    private TextView currentVersionTextView;
    private TextView newVersionTextView;
    private String currentVersion;
    private String newVersion;
    private TextView aboutTextView;

    private Properties properties;
    private String serverURL;
    private Handler handler;
    private HttpUtils httpUtils;
    private DownloadManager downloadManager;
    private long downloadId;
    private BroadcastReceiver broadcastReceiver;
    private int clickedCount;
    private static final String[] egg = {"(⑉･̆-･̆⑉)","（๑ `▽´๑)","(≡•̀·̯•́≡)","(๑￫ܫ￩)","23333","＞﹏＜ ",
            "( ・ˍ・) ", " ⁄(⁄ ⁄•⁄ω⁄•⁄ ⁄)⁄","→_→","🤣","请求积分：∭xdx+y㏑xdy+z¼(∯xdx+㏑ydy)dz"};
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setTitle("关于");
        properties = MainActivity.properties;

        clickedCount = 0;

        initView();
        initServerURL();
        httpUtils = new HttpUtils(getApplicationContext());
        newVersion = "";

        random = new Random();
        checkUpdate();
    }

    private void initView(){
        download_Button = (Button) findViewById(R.id.download_new_version);
        currentVersionTextView = (TextView) findViewById(R.id.current_version);
        newVersionTextView = (TextView) findViewById(R.id.new_version);
        newVersionTextView.setText("正在检查更新...");

        currentVersion = getAppVersionName(getApplicationContext());
        currentVersionTextView.setText(currentVersion);

        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {

                switch (msg.what){
                    case HttpUtils.STATUS_OK:
                        JSONObject json = (JSONObject) msg.obj;
//                        ToastUtils.show(getContext(), json.getString("attitude"));
                        newVersion = json.getString("version");
                        newVersionTextView.setText(newVersion);
//                        updateButton();
                        break;
                    case DOWNLOAD_OK:
                        ToastUtils.show(getApplicationContext(), "下载完成,请到Download目录下进行安装");
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                download_Button.setEnabled(false);
                                download_Button.setText("下载完成,请到Download目录下进行安装");
                            }
                        }, 0);

                        break;
                    case DOWNLOAD_FAIL:
                        ToastUtils.show(getApplicationContext(), "下载失败");
                        break;
                    default:
                        ToastUtils.show(getApplicationContext(), String.format("请求网络错误，错误代码：%d", msg.what));
                        break;
                }
            }
        };

        download_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.d(TAG,String.valueOf(clickedCount));

                if (currentVersion.equals(newVersion)){
                    ToastUtils.show(getApplicationContext(), "已是最新版本，无需更新");
                    return;
                }
                if (newVersion.equals("")){
                    ToastUtils.show(getApplicationContext(),"请等待检查新版本");
                    return;
                }
//                downloadNewVersionAPK();


                VerfyPremission.verifyStoragePermissions(AboutActivity.this);

                //安卓11读写权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    VerfyPremission.checkStorageManagerPermission(getApplicationContext());
                }

                okhttpDownload();
                download_Button.setEnabled(false);
                download_Button.setText("正在下载新版本...");
                ToastUtils.show(getApplicationContext(),"正在下载新版本...");
            }
        });

        aboutTextView = (TextView) findViewById(R.id.aboutTextview);
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
        aboutTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedCount += 1;
                if (clickedCount >= 2){
                    clickedCount = 0;
                    int i = random.nextInt(egg.length);
                    ToastUtils.show(getApplicationContext(),egg[i]);
                }
            }
        });

    }

    private void updateButton(){
        if (currentVersion.equals(newVersion)){
            download_Button.setEnabled(false);
            download_Button.setText("已是最新版本，无需更新");
        }else{
            download_Button.setEnabled(true);
            download_Button.setText("下载最新版本");
        }
    }

    private void initServerURL(){
        int default_server = Integer.valueOf(properties.getProperty("default_server"));
        switch (default_server){
            case 0:
                //主服务器
                serverURL = properties.getProperty("main_host") + ":" + properties.getProperty("main_port");
                break;
            case 1:
                //自定义服务器
                serverURL = properties.getProperty("self_host") + ":" + properties.getProperty("self_port");
                break;
            default:
                break;
        }
//        Log.d(TAG, serverURL);
    }

    private void checkUpdate(){
        Thread thread = new Thread(() -> {
            try {
                Response response = httpUtils.postJson(serverURL + "/check_update","{}");
                Message message = new Message();

                if (response != null && response.isSuccessful()){
                    message.what = HttpUtils.STATUS_OK;
                    message.obj = JSONObject.parse(response.body().string());;
                    handler.handleMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Message message = new Message();
                message.what = -1;
                handler.handleMessage(message);
            }

        });
        thread.start();
    }

    private String getAppVersionName(Context context) {
        String appVersionName = "";
        try {
            PackageInfo packageInfo = context.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            appVersionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("", e.getMessage());
        }
        return appVersionName;
    }

    private void okhttpDownload(){
        Thread thread = new Thread(() -> {
            try {
                Response response = httpUtils.downloadFile(serverURL + "/download_apk");
                Message message = new Message();

                if (response != null && response.isSuccessful()){
                    message.what = DOWNLOAD_OK;
                    writeFile(response);
                    handler.handleMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Message message = new Message();
                message.what = DOWNLOAD_FAIL;
                handler.handleMessage(message);
            }

        });
        thread.start();
    }

    private void writeFile(Response response){
        try {
            byte[] bytes = response.body().bytes();
            String fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            fileName += "/输入姿态识别_"+ newVersion +".apk";
            File file = new File(fileName);
            if (file.exists()){
                file.delete();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
            fileOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadNewVersionAPK(){

        Uri uri = Uri.parse(serverURL + "/download_apk");
        Log.d(TAG,"downloadNewVersionAPK:" + uri.toString());
        Context context = getApplicationContext();
        downloadManager = (DownloadManager) context.getSystemService(context.DOWNLOAD_SERVICE);
        DownloadManager.Request requestAPK = new DownloadManager.Request(uri);

        requestAPK.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        requestAPK.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        requestAPK.setTitle("下载");
        requestAPK.setAllowedOverRoaming(false);
        requestAPK.setDescription("正在下载输入姿态识别新版本");
        requestAPK.setVisibleInDownloadsUi(true);


//        Log.d(TAG,Environment.DIRECTORY_DOWNLOADS);
        requestAPK.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"输入姿态识别_"+ newVersion +".apk");

        downloadId = downloadManager.enqueue(requestAPK);

        broadcastReceiver =  new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkStatus();
            }
        };

        this.registerReceiver(broadcastReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void checkStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        //通过下载的id查找
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                //下载暂停
                case DownloadManager.STATUS_PAUSED:
                    break;
                //下载延迟
                case DownloadManager.STATUS_PENDING:
                    break;
                //正在下载
                case DownloadManager.STATUS_RUNNING:
                    break;
                //下载完成
                case DownloadManager.STATUS_SUCCESSFUL:
                    //下载完成
                    ToastUtils.show(getApplicationContext(), "下载完成,请到Download目录下进行安装");
                    download_Button.setEnabled(false);
                    download_Button.setText("下载完成,请到Download目录下进行安装");
                    cursor.close();
                    break;
                //下载失败
                case DownloadManager.STATUS_FAILED:
                    ToastUtils.show(getApplicationContext(), "下载失败");
                    cursor.close();
                    break;
                default:
                    download_Button.setText("下载出错：" + status);
                    cursor.close();
                    break;
            }
        }
    }
}