package com.myapp.sensordatacollection.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.myapp.sensordatacollection.R;
import com.myapp.sensordatacollection.utils.SensorDataBase;
import com.myapp.sensordatacollection.utils.ToastUtils;
import com.myapp.sensordatacollection.utils.VerfyPremission;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class CollectionActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "CollectionActivity";
    private RadioGroup radioGroup1;
    private RadioGroup radioGroup2;

    private static final int ID_NUMBER_MAX_LENGTH = 18;

    //正常按钮
    private RadioButton normalButton;

    //异常按钮(输入中换人)
    private RadioButton inputChangeButton;

    //行走输入，不换人
    private RadioButton walkingInputButton;

    private Button saveButton;
    private EditText editTextIdNumber;
    private TextView idNumberCount;
    private TextView collectState;

    private CheckBox agreeCheckBox;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    //两次检测时间间隔
    private static int UPDATE_INTERVAL_TIME;
    private StringBuilder sampleStringBuilder;

    //要保存的数据的stringBuilder
    private StringBuilder collectedStringBuilder;

    private int fileTypeId;

    Handler collectHandler;
    Runnable runnable;

    Boolean startCollect;

    //支持采集的几个传感器
    private final int typeArray[] = {Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_GYROSCOPE};
    private final String id2SensorName[] = {"gravity", "linear_accel", "accel", "magnet", "gyro"};
    private static String csvHeader;

    private SensorDataBase sensorDataBase;
    private Handler handler;
    private int collectedLine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        handler = new Handler(Looper.getMainLooper());

        startCollect = false;
        collectedStringBuilder = new StringBuilder();

        sampleStringBuilder = new StringBuilder();
        try {
            sensorDataBase = SensorDataBase.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sampleStringBuilder.append("timestamp,");

        //初始化csv标题，不想一个一个抄下来了
        for (String s: id2SensorName){
            sampleStringBuilder.append(String.format("%s_x,%s_y,%s_z,",s,s,s));
        }
        //删除后面的逗号
        sampleStringBuilder.delete(sampleStringBuilder.length() - 1, sampleStringBuilder.length());
        sampleStringBuilder.append("\n");
        csvHeader = sampleStringBuilder.toString();
        sampleStringBuilder.delete(0,sampleStringBuilder.length());

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("bundle");
        fileTypeId = bundle.getInt("fileTypeId");
        UPDATE_INTERVAL_TIME = bundle.getInt("sampleTime");
        initView();
        setTitle("数据采集");

        setRadioButtonState(fileTypeId);

        VerfyPremission.verifyStoragePermissions(CollectionActivity.this);

        //安卓11读写权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            VerfyPremission.checkStorageManagerPermission(getApplicationContext());
        }
    }
    //设置循环定时器，循环采集传感器的数据
    public void startCollectSensorData(){
//        Log.d(TAG, "startCollectSensorData");
        if (!agreeCheckBox.isChecked()) {
            ToastUtils.show(getApplicationContext(), "请同意收集手机传感器数据");
            return;
        }
        collectedStringBuilder.delete(0, collectedStringBuilder.length());
        collectedLine = 0;

        runnable = new Runnable() {
            @Override
            public void run() {
                sensorDataBase.collectCurrentTimeSensorData(sampleStringBuilder, collectedStringBuilder,true);
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
    public void stopCollectSensorData() {
        if (!startCollect) {
            return;
        }
        handler.removeCallbacks(runnable);
        startCollect = false;
        collectState.setTextColor(Color.GREEN);
        collectState.setText(String.format("已停止采集(%.2fHz)", 1000.0 / UPDATE_INTERVAL_TIME));
    }

    //根据fileTypeId设置RadioButton选择状态
    private void setRadioButtonState(int fileTypeId){
        radioGroup1.clearCheck();
        radioGroup2.clearCheck();

        switch (fileTypeId){
            case 0:
                normalButton.setChecked(true);
                break;
            case 1:
                inputChangeButton.setChecked(true);
                break;
            case 2:
                walkingInputButton.setChecked(true);
                break;
            default:
                break;
        }
    }

    private void initGroupRadioButton(){
        radioGroup1 = (RadioGroup) findViewById(R.id.checkGroup);
        radioGroup2 = (RadioGroup) findViewById(R.id.checkGroup2);

        normalButton = (RadioButton) findViewById(R.id.normalButton);
        inputChangeButton = (RadioButton) findViewById(R.id.inputChangeButton);
        walkingInputButton = (RadioButton) findViewById(R.id.walkingInputButton);

        normalButton.setOnCheckedChangeListener(this::onCheckedChanged);
        inputChangeButton.setOnCheckedChangeListener(this::onCheckedChanged);
        walkingInputButton.setOnCheckedChangeListener(this::onCheckedChanged);
    }
    private void initView(){
        //初始化6个单选radio button
        initGroupRadioButton();

        collectState = (TextView) findViewById(R.id.collectState);

        idNumberCount = (TextView) findViewById(R.id.idNumberCount);
        idNumberCount.setText(String.valueOf(ID_NUMBER_MAX_LENGTH));

        editTextIdNumber = (EditText) findViewById(R.id.cardNumberEditText);
        collectState.setText(String.format("未采集(%.2fHz)", 1000.0 / UPDATE_INTERVAL_TIME));

        agreeCheckBox = (CheckBox) findViewById(R.id.agreeCheckBox);

        editTextIdNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (!startCollect){
                    startCollectSensorData();
                }
                int textLength = editTextIdNumber.getText().length();
                if ( textLength == ID_NUMBER_MAX_LENGTH) {

                    stopCollectSensorData();
                    ToastUtils.show(getApplicationContext(),"输入完成");
                    saveButton.setEnabled(true);
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
                //光标聚焦并且没有开始采集，开始采集
                //不判断是否开始采集，可能会重复开始采集
                if (focus && !startCollect){
                    startCollectSensorData();
                }

                //失去光标
                if (!focus && textLength == 0){
                    stopCollectSensorData();
                    collectedStringBuilder.delete(0, collectedStringBuilder.length());
                    collectState.setTextColor(Color.GRAY);
                    collectState.setText(String.format("未采集(%.2fHz)", 1000.0 / UPDATE_INTERVAL_TIME));
                    saveButton.setEnabled(false);
                }

            }
        });

        //点击保存按钮后的动作
        saveButton = (Button) findViewById(R.id.uploadButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (collectedStringBuilder.length() == 0){
                    ToastUtils.show(getApplicationContext(),"未采集任何数据，无法保存！");
                    return;
                }

                if (editTextIdNumber.getText().length() == 0){
                    ToastUtils.show(getApplicationContext(),"请完成xxx号码输入后再提交");
                    return;
                }

                //保存数据
                Random random = new Random();
                String fileName = String.format("%d.%d.%d.csv", fileTypeId, System.currentTimeMillis(), random.nextInt(100));
                String filePath = getSaveFilePath();
                try {


                    File file = new File(filePath);
                    if (!file.exists()){
                        if(!file.mkdir()){
                            ToastUtils.show(getApplicationContext(),"创建Collection文件夹失败");
                            return;
                        }
                    }


                    FileWriter writer = new FileWriter(filePath + fileName);
                    writer.write(csvHeader);
                    writer.write(collectedStringBuilder.toString());
                    writer.close();
                } catch (IOException e){
                    e.printStackTrace();
                    ToastUtils.show(getApplicationContext(),"文件保存失败，请检查存储权限和存储空间大小");
                    return;
                }

                //结束当前activity
                ToastUtils.show(getApplicationContext(), "文件：" + fileName + " 保存成功");

                Intent intent = new Intent();
                intent.putExtra("radioSelectId", fileTypeId);

                CollectionActivity.this.setResult(RESULT_OK, intent);

                CollectionActivity.this.finish();
            }
        });


    }

    //单选按钮group处理
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        //过滤掉非点击导致的状态更新，避免调用两次
        if (!compoundButton.isPressed()){
            return;
        }

        switch (compoundButton.getId()){
            case R.id.normalButton:
                fileTypeId = 0;
                radioGroup2.clearCheck();
                break;
            case R.id.inputChangeButton:
                fileTypeId = 1;
                radioGroup1.clearCheck();
                break;
            case R.id.walkingInputButton:
                fileTypeId = 2;
                radioGroup1.clearCheck();
                break;
            default:
                break;
        }
    }

    //获取文件的存储目录
    public String getSaveFilePath(){
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Collection/";
            return  path;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        //注销服务和删除builder的数据
        collectedStringBuilder.delete(0, collectedStringBuilder.length());

        Intent intent = new Intent();
        intent.putExtra("radioSelectId", fileTypeId);
        this.setResult(RESULT_OK, intent);

        super.onDestroy();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            collectedStringBuilder.delete(0, collectedStringBuilder.length());

            Intent intent = new Intent();
            intent.putExtra("radioSelectId", fileTypeId);
            this.setResult(RESULT_OK, intent);

            this.finish();
        }

        return super.onKeyDown(keyCode, event);
    }

}
