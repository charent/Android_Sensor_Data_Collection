package com.myapp.sensordatacollection.utils;

import static android.content.Context.SENSOR_SERVICE;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.myapp.sensordatacollection.MainActivity;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SensorDataBase implements SensorEventListener {

    private static final String TAG = "CollectionBaseFragment";

    //最大收集行数，100ms采样间隔时50s的最长采样时间
    public static final int MAX_COLLECT_LINE= 512;

    private final int typeArray[] = {Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_GYROSCOPE};
//    private final String id2SensorName[] = {"gravity", "linear_accel", "accel", "magnet", "gyro"};
    public ConcurrentHashMap<Integer, Vector<Double>> sensorDataMap;

    //两次检测时间间隔
    private SensorManager sensorManager;
    private Activity activity;
    private static final ReentrantLock lock = new ReentrantLock(true);

    private static volatile SensorDataBase sensorDataBase;

    //单例模式
    public static SensorDataBase getInstance(Activity activity) throws Exception {

        if (sensorDataBase == null){
            synchronized (SensorDataBase.class){
                if (sensorDataBase == null){
                    if (! (activity instanceof MainActivity)){
                        throw new Exception("SensorDataBase only can create by MainActivity");
                    }
                    sensorDataBase = new SensorDataBase(activity);
                }
            }
        }

        return sensorDataBase;
    }

    //单例模型构造函数为私有变量
    private  SensorDataBase(Activity activity){
        this.activity = activity;

        sensorDataMap = new ConcurrentHashMap<Integer, Vector<Double>>();


        sensorManager = (SensorManager) activity.getSystemService(SENSOR_SERVICE);
        for (int i = 0; i < typeArray.length; ++i){
            //注册服务
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(typeArray[i]),sensorManager.SENSOR_DELAY_UI);
            Vector<Double> vector = new Vector<Double>();
            for (int j = 0; j < 3; ++j){
                vector.add(0.0);
            }
            sensorDataMap.put(typeArray[i], vector);
        }
    }

//    public void setSampleTime(int sampleTime){
//        //如果已经开始采集了，更改采样时间要先停止采集
//       boolean isStatusChange = false;
//       if (startCollect){
//           stopCollectSensorData();
//           isStatusChange = true;
//       }
//
//       this.UPDATE_INTERVAL_TIME = sampleTime;
//
//       if (isStatusChange){
//           startCollectSensorData();
//       }
//    }

//    //设置循环定时器，循环采集传感器的数据
//    public void startCollectSensorData(){
////        Log.d(TAG, "startCollectSensorData");
//        collectedStringBuilder.delete(0, collectedStringBuilder.length());
//        collectedLine = 0;
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                collectCurrentTimeSensorData();
//            }
//        };
//        timer.schedule(timerTask, 0, UPDATE_INTERVAL_TIME);
//        startCollect = true;
//    }

//    //停止采集数据
//    public void stopCollectSensorData(){
//        if (!startCollect){
//            return;
//        }
////        Log.d(TAG, "stopCollectSensorData");
//        timer.cancel();
//
//        startCollect = false;
//    }

    public void collectCurrentTimeSensorData(StringBuilder sampleStringBuilder, StringBuilder collectedStringBuilder, boolean withTimestamp){
//        Log.d(TAG,"collectCurrentTimeSensorData:" );

        sampleStringBuilder.delete(0, sampleStringBuilder.length());

        if (withTimestamp){
            sampleStringBuilder.append(String.format("%d,", System.currentTimeMillis()));
        }

        lock.lock();
        for (int i = 0; i < typeArray.length; ++i){
            Vector<Double> vector = sensorDataMap.get(typeArray[i]);
            for (int j = 0; j < 3; ++j){
                sampleStringBuilder.append(vector.get(j));
                sampleStringBuilder.append(',');
            }
        }
        lock.unlock();
        //删除最后面的逗号
        sampleStringBuilder.delete(sampleStringBuilder.length() - 1, sampleStringBuilder.length());
        sampleStringBuilder.append("\n");

        collectedStringBuilder.append(sampleStringBuilder.toString());

//        if (collectedLine >= MAX_COLLECT_LINE) {
//            stopCollectSensorData();
////            Log.d(TAG,"到达最大采集时间或数据行数:" + String.valueOf(collectedLine));
//            ToastUtils.show(activity.getApplicationContext(),"到达最大采集时间或数据行数");
//        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x_value = sensorEvent.values[0];
        double y_value = sensorEvent.values[1];
        double z_value = sensorEvent.values[2];

        lock.lock();
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY){
            //重力
            Vector<Double> vector = sensorDataMap.get(Sensor.TYPE_GRAVITY);
            vector.set(0, x_value);
            vector.set(1, y_value);
            vector.set(2, z_value);
            sensorDataMap.put(Sensor.TYPE_GRAVITY, vector);

        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            //线性加速度
            Vector<Double> vector = sensorDataMap.get(Sensor.TYPE_LINEAR_ACCELERATION);
            vector.set(0, x_value);
            vector.set(1, y_value);
            vector.set(2, z_value);
            sensorDataMap.put(Sensor.TYPE_LINEAR_ACCELERATION, vector);

        }else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            //加速度
            Vector<Double> vector = sensorDataMap.get(Sensor.TYPE_ACCELEROMETER);
            vector.set(0, x_value);
            vector.set(1, y_value);
            vector.set(2, z_value);
            sensorDataMap.put(Sensor.TYPE_ACCELEROMETER, vector);

        }else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //磁场
            Vector<Double> vector = sensorDataMap.get(Sensor.TYPE_MAGNETIC_FIELD);
            vector.set(0, x_value);
            vector.set(1, y_value);
            vector.set(2, z_value);
            sensorDataMap.put(Sensor.TYPE_MAGNETIC_FIELD, vector);

        }else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //陀螺仪
            //需要将弧度转为角度
            double x_deg = Math.toDegrees(x_value);
            double y_deg = Math.toDegrees(y_value);
            double z_deg = Math.toDegrees(z_value);

            Vector<Double> vector = sensorDataMap.get(Sensor.TYPE_GYROSCOPE);
            vector.set(0, x_deg);
            vector.set(1, y_deg);
            vector.set(2, z_deg);
            sensorDataMap.put(Sensor.TYPE_GYROSCOPE, vector);
        }
        lock.unlock();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void stopListenSensor(){
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void finalize() throws Throwable {
        stopListenSensor();
        sensorManager.unregisterListener(this);
        super.finalize();
    }
}
