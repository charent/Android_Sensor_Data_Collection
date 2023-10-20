package com.myapp.sensordatacollection.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.myapp.sensordatacollection.R;
import com.myapp.sensordatacollection.utils.PropertiesUtils;
import com.myapp.sensordatacollection.utils.ToastUtils;
import com.myapp.sensordatacollection.utils.Tools;

import java.util.Properties;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "SettingActivity";
    private boolean isSettingChange;

    private Properties properties;
    private String properties_file_name;

    private EditText sampleTimeEditText;
    private TextView sampleHzTextView;
    private RadioButton mainServerRadioButton;
    private RadioButton selfServerRadioButton;

    private EditText serverIPEditText;
    private EditText serverPortEditText;
    private EditText serverFunction;

    private Button saveButton;
    private Button saveQuitButton;
    private int default_server;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("设置");
        isSettingChange = false;
        initView();

    }

    private void initView(){
        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("bundle");
        properties_file_name = bundle.getString("properties_file_name");
        properties = PropertiesUtils.getUserProperties(getApplicationContext(), properties_file_name);

        sampleTimeEditText = (EditText) findViewById(R.id.sampleTimeEditText);
        mainServerRadioButton = (RadioButton) findViewById(R.id.mainServerRadioButton);
        selfServerRadioButton = (RadioButton) findViewById(R.id.selfServerRadioButton);

        serverIPEditText = (EditText) findViewById(R.id.serverIP);
        serverPortEditText = (EditText) findViewById(R.id.serverPort);
        serverFunction = (EditText) findViewById(R.id.serverFunction);
        sampleHzTextView = (TextView) findViewById(R.id.sampleHzTextView);

        saveButton = (Button) findViewById(R.id.saveButton);
        saveQuitButton = (Button) findViewById(R.id.saveQuitButton);

        default_server = Integer.valueOf((String) properties.get("default_server"));
        switch (default_server){
            case 0:
                mainServerRadioButton.setChecked(true);
                setServerEditTextEnable(false);
                updateServerEditText((String) properties.get("main_host"), (String) properties.get("main_port"),
                        (String) properties.get("main_function"));
                break;
            case 1:
                selfServerRadioButton.setChecked(true);
                setServerEditTextEnable(true);
                updateServerEditText((String) properties.get("self_host"), (String) properties.get("self_port"),
                        (String) properties.get("self_function"));
                break;
            default:
                break;
        }

        mainServerRadioButton.setOnCheckedChangeListener(this::onCheckedChanged);
        selfServerRadioButton.setOnCheckedChangeListener(this::onCheckedChanged);

        setReadSampleTimeEditText();
        sampleTimeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = String.valueOf(sampleTimeEditText.getText());
                if (text == null || text == "" || text.length() == 0){
                    ToastUtils.show(getApplicationContext(),"采样时间不能为空");
                    saveButton.setEnabled(false);
                    saveQuitButton.setEnabled(false);
                    return;
                }

                saveButton.setEnabled(true);
                saveQuitButton.setEnabled(true);
                int sampleTime = Integer.valueOf(text);
                if (sampleTime <= 0){
                    ToastUtils.show(getApplicationContext(), "采样时间为>=1的整数！");
                    return;
                }
                properties.setProperty("sample_time", String.valueOf(sampleTime));

                sampleHzTextView.setText(String.format("%.2f Hz", (float) 1000f / sampleTime));
            }
        });

        //保存
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.d(TAG, properties.getProperty("sample_time"));
                if (!checkProperties()){
                    return;
                }
                PropertiesUtils.saveUserProperties(getApplicationContext(), properties, properties_file_name);
                isSettingChange = true;
                ToastUtils.show(getApplicationContext(),"保存成功");
            }
        });

        saveQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkProperties()){
                    return;
                }
                properties.setProperty("default_server", String.valueOf(default_server));
                PropertiesUtils.saveUserProperties(getApplicationContext(), properties, properties_file_name);
                isSettingChange = true;
                Intent intent = new Intent();
                intent.putExtra("isSettingChange", isSettingChange);
                SettingsActivity.this.setResult(RESULT_OK, intent);
                ToastUtils.show(getApplicationContext(),"保存成功");
                finish();
            }
        });


    }

    private boolean checkProperties(){

        if (default_server == 0){
            return true;
        }

        String host = String.valueOf(serverIPEditText.getText());
        String port = String.valueOf(serverPortEditText.getText());
        String func = String.valueOf(serverFunction.getText());

        if (host.length() == 0 || port.length() == 0 || func.length() == 0){
            ToastUtils.show(getApplicationContext(), "自定义服务器参数不能为空！");
            return false;
        }

        String ip = "";
        if (host.startsWith("http://")) {
            ip = host.substring(7, host.length());
        }else if (host.startsWith("https://")){
            ip = host.substring(8, host.length());
        }else {
            ToastUtils.show(getApplicationContext(), "服务器地址必须以 http:// 或者 https:// 开头！");
            return false;
        }

        if (!Tools.isIPAddress(ip)){
            ToastUtils.show(getApplicationContext(),"请输入合法的IPv4地址！");
            return false;
        }

        int p = Integer.valueOf(port);
        if (p > 65535 || p < 0 ){
            ToastUtils.show(getApplicationContext(), "端口号范围：[0, 65535)");
            return false;
        }

        if (!func.startsWith("/")){
            ToastUtils.show(getApplicationContext(), "方法开头必须为”/");
            return false;
        }

        //设置properties
        properties.setProperty("self_host", host);
        properties.setProperty("self_port", port);
        properties.setProperty("self_function", func);

        return true;
    }


    private void setReadSampleTimeEditText(){
        String sampleTime = (String) properties.get("sample_time");
        sampleTimeEditText.setText(sampleTime);
        int time = Integer.valueOf(sampleTime);
        sampleHzTextView.setText(String.format("%.2f Hz", (float) 1000f /time));

    }

    private void updateServerEditText(String host, String port, String func){
        serverIPEditText.setText(host);
        serverPortEditText.setText(port);
        serverFunction.setText(func);
    }

    private void setServerEditTextEnable(boolean b){
        serverIPEditText.setEnabled(b);
        serverPortEditText.setEnabled(b);
        serverPortEditText.setEnabled(b);
        serverFunction.setEnabled(b);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        //过滤掉非点击导致的状态更新，避免调用两次
        if (!compoundButton.isPressed()){
            return;
        }

        switch (compoundButton.getId()){
            case R.id.mainServerRadioButton:
                setServerEditTextEnable(false);
                default_server = 0;
                updateServerEditText((String) properties.get("main_host"), (String) properties.get("main_port"),
                        (String) properties.get("main_function"));
                break;
            case R.id.selfServerRadioButton:
                setServerEditTextEnable(true);
                default_server = 1;
                updateServerEditText((String) properties.get("self_host"), (String) properties.get("self_port"),
                        (String) properties.get("self_function"));
                break;
            default:
                break;

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            intent.putExtra("isSettingChange", isSettingChange);
            this.setResult(RESULT_OK, intent);
            this.finish();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent();
        intent.putExtra("isSettingChange", isSettingChange);
        this.setResult(RESULT_OK, intent);
        super.onDestroy();
    }

}
