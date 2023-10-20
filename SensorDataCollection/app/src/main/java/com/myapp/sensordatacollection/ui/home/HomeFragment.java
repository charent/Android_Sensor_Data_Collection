package com.myapp.sensordatacollection.ui.home;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.SENSOR_SERVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavArgument;

import com.myapp.sensordatacollection.MainActivity;
import com.myapp.sensordatacollection.activities.CollectionActivity;
import com.myapp.sensordatacollection.databinding.FragmentHomeBinding;
import com.myapp.sensordatacollection.utils.PropertiesUtils;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;
    private Button startButton;

    private SensorManager sensorManager;
    private List<Sensor> sensorList;

    private TextView sensorListView;

    //记录本次打开的选中的radioButton id
    private static int radioSelectId;
    private Properties properties;

    //采样时间
    private static int sampleTime;
    private EditText sampleTimeEditText;
    private TextView sampleHzTextView;

    private final int RequestCode = 1;
    private Map<String, NavArgument> map;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
//        map = NavHostFragment.findNavController(this).getGraph().getArguments();

        homeViewModel = new ViewModelProvider(getActivity()).get(HomeViewModel.class);
        properties = homeViewModel.getProperties();

        //默认中正常单手操作
        radioSelectId = Integer.parseInt(properties.getProperty("radio_select_id"));
        sampleTime =  Integer.parseInt(properties.getProperty("sample_time"));

        initView();
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CART_BROADCAST");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String cmd = intent.getStringExtra("cmd");
                if ("refresh".equals(cmd)){
//                    refresh();
                    //不用调用refresh，重新onResume或者onStart方法即可
                    return;
                }
            }
        };


        broadcastManager.registerReceiver(receiver, intentFilter);
    }

    private void refresh(){
        properties = homeViewModel.getProperties();
//        Log.d(TAG,"refresh " + properties.getProperty("sample_time"));
        sampleTime = Integer.valueOf(properties.getProperty("sample_time"));
        float hz = (float) (1000.0 / sampleTime);
        sampleTimeEditText.setText(String.valueOf(sampleTime));
        sampleHzTextView.setText(String.format("%.2f Hz", hz));
    }

    private void initView(){
        startButton = binding.startButton;
        sampleTimeEditText = binding.sampleTimeEditText;
        sensorListView = binding.sensorListTextView;

        sensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("该设备支持的传感器：\n");
        for (Sensor sensor : sensorList) {
            stringBuffer.append("   " + sensor.getName() + ", max range: "+  + sensor.getMaximumRange()  + " \n");
        }


        sampleTimeEditText.setText(String.valueOf(sampleTime));
        sampleHzTextView = binding.sampleHzTextView;

        float hz = (float) (1000.0 / sampleTime);
        sampleHzTextView.setText(String.format("%.2f Hz", hz));

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getActivity(),CollectionActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("fileTypeId", radioSelectId);
                bundle.putInt("sampleTime", sampleTime);
                intent.putExtra("bundle", bundle);
                startActivityForResult(intent,RequestCode);

            }
        });

        sensorListView.setText(stringBuffer.toString());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //更新RadioSelectId
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCode && resultCode == RESULT_OK && data != null){
            Bundle bundle = data.getExtras();
            int return_select_id = bundle.getInt("radioSelectId");
            if (return_select_id != radioSelectId){
                radioSelectId = return_select_id;
                properties.setProperty("radio_select_id", String.valueOf(return_select_id));
                PropertiesUtils.saveUserProperties(getActivity().getApplicationContext(), properties, MainActivity.PROPERTIES_FILE_NAME);
                homeViewModel.setLiveProperties(properties);
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        Log.d(TAG, "onResume");
        refresh();
    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        Log.d(TAG, "onStart");
//        refresh();
//    }
}