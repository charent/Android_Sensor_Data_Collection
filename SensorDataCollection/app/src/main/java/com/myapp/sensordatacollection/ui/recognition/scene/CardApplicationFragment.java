package com.myapp.sensordatacollection.ui.recognition.scene;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSONObject;
import com.myapp.sensordatacollection.MainActivity;
import com.myapp.sensordatacollection.databinding.FragmentCardApplicationBinding;
import com.myapp.sensordatacollection.ui.home.HomeViewModel;
import com.myapp.sensordatacollection.utils.FileUtils;
import com.myapp.sensordatacollection.utils.HttpUtils;
import com.myapp.sensordatacollection.utils.SensorDataBase;
import com.myapp.sensordatacollection.utils.ToastUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import okhttp3.Response;


public class CardApplicationFragment extends Fragment {

    public static final String TAG = "CardApplicationFragment";
    public static final int SCENE_ID = 0;

    private SensorDataBase sensorDataBase;
    private HomeViewModel homeViewModel;
    private Properties properties;
    private int UPDATE_INTERVAL_TIME;
    private boolean startCollect;
    private StringBuilder collectedStringBuilder;
    private StringBuilder sampleStringBuilder;
    private List<String> cacheFileList;
    private static final String CACHE_FILE_TYPE = "csv_data";


    private FragmentCardApplicationBinding binding;

    private int collectedLine;

    private Button uploadButton;
    private EditText editTextIdNumber;
    private TextView idNumberCount;
    private TextView collectState;
    private TextView inputAttitude;

    private String serverURL;
    private Handler handler;
    private CheckBox agreeCheckBox;
    private static int ID_NUMBER_MAX_LENGTH = 18;
    private HttpUtils httpUtils;
    private Runnable runnable;

    public CardApplicationFragment() {
        // Required empty public constructor
    }

    public static CardApplicationFragment newInstance() {
        CardApplicationFragment fragment = new CardApplicationFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentCardApplicationBinding.inflate(inflater,container,false);
//        Log.d(TAG,String.valueOf(binding == null));

        homeViewModel = new ViewModelProvider(getActivity()).get(HomeViewModel.class);
        properties = homeViewModel.getProperties();
        UPDATE_INTERVAL_TIME = Integer.parseInt(properties.getProperty("sample_time"));
        collectedStringBuilder = new  StringBuilder();
        sampleStringBuilder = new StringBuilder();
        httpUtils = new HttpUtils(getContext());

        serverURL = MainActivity.SERVER_RC_URL;

        try {
            sensorDataBase = SensorDataBase.getInstance(getActivity());
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Log.d(TAG, getActivity().toString());
        cacheFileList = new ArrayList<>();

        initView();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CART_BROADCAST");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String cmd = intent.getStringExtra("cmd");
                if ("refresh".equals(cmd)){
                    refresh();
                    //不用调用refresh，重新onResume或者onStart方法即可
                    return;
                }
            }
        };

        broadcastManager.registerReceiver(receiver, intentFilter);
        return binding.getRoot();
    }

    private void initView(){

        uploadButton = binding.uploadButton;
        editTextIdNumber = binding.idNumberEditText;
        idNumberCount = binding.idNumberCount;
        collectState = binding.collectState;
        inputAttitude = binding.inputAttitude;
        agreeCheckBox = binding.agreeCheckBox;

        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {

                switch (msg.what){
                    case HttpUtils.STATUS_OK:
                        JSONObject json = (JSONObject) msg.obj;
//                        ToastUtils.show(getContext(), json.getString("attitude"));
                        int risk = json.getInteger("risk");
                        if (risk == 0){
                            inputAttitude.setTextColor(Color.GREEN);
                        }else {
                            inputAttitude.setTextColor(Color.RED);
                        }
                        inputAttitude.setText(json.getString("attitude"));
                        break;
                    default:
                        ToastUtils.show(getContext(), String.format("服务器响应状态码：%d", msg.what));
                        break;
                }
            }
        };

        collectState.setText(String.format("未采集(%.2fHz)", 1000.0 / UPDATE_INTERVAL_TIME));
        idNumberCount.setText(String.valueOf(ID_NUMBER_MAX_LENGTH));

        editTextIdNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (!startCollect){
                    startCollectSensorData();
                }
                int textLength = editTextIdNumber.getText().length();
                if ( textLength == ID_NUMBER_MAX_LENGTH) {
                    stopCollectSensorData();
                    inputAttitude.setTextColor(Color.GRAY);
                    inputAttitude.setText("正在识别...");

                    //保存数据
                    Random random = new Random();
                    String fileName = String.format("%d.%d.csv", System.currentTimeMillis(), random.nextInt(100));
                    String filePath = FileUtils.getAppCachePath(getContext(),CACHE_FILE_TYPE);

                    saveCsvFileToCache(filePath, fileName);

                    File file = new File(filePath + fileName);

                    postCSVFile(file);
//                    uploadButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                int textLength = editTextIdNumber.getText().length();

                idNumberCount.setText(String.valueOf(ID_NUMBER_MAX_LENGTH - textLength));
            }
        });

        editTextIdNumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focus) {
                int textLength = editTextIdNumber.getText().length();
                if ( textLength == ID_NUMBER_MAX_LENGTH){
                    return;
                }

                if (focus){
                    inputAttitude.setTextColor(Color.GRAY);
                    inputAttitude.setText("等待输入完成...");
                }else{
                    if (textLength > 0 && textLength < ID_NUMBER_MAX_LENGTH){
                        inputAttitude.setTextColor(Color.GRAY);
                        inputAttitude.setText("请继续完成输入");
                    }else{
                        inputAttitude.setTextColor(Color.GRAY);
                        inputAttitude.setText("未识别");
                    }
                }

                //光标聚焦并且没有开始采集，开始采集
                //不判断是否开始采集，可能会重复开始采集
                if (focus && !startCollect){
                    startCollectSensorData();
                }

                //失去光标
                if (!focus && textLength == 0){
                    stopCollectSensorData();
                    collectedStringBuilder.delete(0,collectedStringBuilder.length());
                    collectState.setTextColor(Color.GRAY);
                    collectState.setText(String.format("未采集(%.2fHz)", 1000.0 / UPDATE_INTERVAL_TIME));
                    uploadButton.setEnabled(false);
                }

            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (collectedStringBuilder.length() == 0){
                    ToastUtils.show(getContext(),"未采集任何数据，无法保存！");
                    return;
                }

                if (editTextIdNumber.getText().length() == 0){
                    ToastUtils.show(getContext(),"请完成xxx号码输入后再提交");
                    return;
                }

                //保存数据
                Random random = new Random();
                String fileName = String.format("%d.%d.csv", System.currentTimeMillis(), random.nextInt(100));
                String filePath = FileUtils.getAppCachePath(getContext(),CACHE_FILE_TYPE);

                saveCsvFileToCache(filePath, fileName);

                File file = new File(filePath + fileName);

                postCSVFile(file);

            }
        });
    }

    private void postCSVFile(File csvFile){
        Thread thread = new Thread(() -> {
            try {
                Response response = httpUtils.postCsvFile(serverURL, csvFile, SCENE_ID);
                Message message = new Message();

                if (response != null && response.isSuccessful()){
                    message.what = HttpUtils.STATUS_OK;
                    message.obj = JSONObject.parse(response.body().string());
                    handler.sendMessage(message);
                    return;
                }

                message.what = response.code();
                handler.sendMessage(message);

//                Log.d(TAG, String.valueOf(response.code()));
            } catch (Exception e) {
                e.printStackTrace();
                ToastUtils.show(getContext(), e.getMessage());
            }

        });
        thread.start();
    }

    private void saveCsvFileToCache(String filePath, String fileName){
        try {
            File file = new File(filePath);
            if (!file.exists()){
                if(!file.mkdir()){
                    ToastUtils.show(getContext(),"创建Collection文件夹失败");
                    return;
                }
            }

            FileWriter writer = new FileWriter(filePath + fileName);
            writer.write(collectedStringBuilder.toString());
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
            ToastUtils.show(getContext(),"文件保存失败，请检查存储权限和存储空间大小");
        }

        cacheFileList.add(filePath + fileName);
    }


    //设置循环定时器，循环采集传感器的数据
    public void startCollectSensorData(){
//        Log.d(TAG, "startCollectSensorData");
        if (!agreeCheckBox.isChecked()) {
            ToastUtils.show(getContext(), "请同意收集手机传感器数据");
            return;
        }
        collectedStringBuilder.delete(0, collectedStringBuilder.length());
        collectedLine = 0;
//        timer = new Timer();
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                sensorDataBase.collectCurrentTimeSensorData(sampleStringBuilder, collectedStringBuilder);
//                collectedLine += 1;
//                Log.d(TAG,sampleStringBuilder.toString());
//            }
//        };
//        timer.schedule(timerTask, 0, UPDATE_INTERVAL_TIME);

         runnable = new Runnable() {
            @Override
            public void run() {
                sensorDataBase.collectCurrentTimeSensorData(sampleStringBuilder, collectedStringBuilder, false);
                collectedLine += 1;
//                Log.d(TAG,sampleStringBuilder.toString());
                handler.postDelayed(this,UPDATE_INTERVAL_TIME);
            }
        };
        handler.post(runnable);
        startCollect = true;
        collectState.setTextColor(Color.RED);
        collectState.setText(String.format("正在采集(%.2fHz)", 1000.0 / UPDATE_INTERVAL_TIME));
    }

    //停止采集数据
    public void stopCollectSensorData(){
        if (!startCollect){
            return;
        }
//        Log.d(TAG, "stopCollectSensorData");
//        timer.cancel();
//        timer.purge();
//        timer = null;
        handler.removeCallbacks(runnable);
        startCollect = false;
        collectState.setTextColor(Color.GREEN);
        collectState.setText(String.format("已停止采集(%.2fHz)", 1000.0 / UPDATE_INTERVAL_TIME));
    }

    private void refresh(){
        properties = homeViewModel.getProperties();
//        Log.d(TAG,"refresh " + properties.getProperty("sample_time"));
        UPDATE_INTERVAL_TIME = Integer.valueOf(properties.getProperty("sample_time"));
        float hz = (float) (1000.0 / UPDATE_INTERVAL_TIME);
        serverURL = MainActivity.SERVER_RC_URL;
        collectState.setText(String.format("未采集(%.2fHz)", hz));
    }


    private void clearCacheFiles(){
        for (String fileName: cacheFileList) {
            File file =  new File(fileName);
            if (file.exists()){
                file.delete();
//                Log.d(TAG,"deleted:" + fileName);
            }
        }
    }

    @Override
    public void onResume() {
        clearCacheFiles();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        clearCacheFiles();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        clearCacheFiles();
        super.onDetach();
    }

}