package com.myapp.sensordatacollection.utils;

import android.content.Context;
import android.util.Log;

import com.myapp.sensordatacollection.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class PropertiesUtils {

    private static final String TAG = "PropertiesUtils";

    //或者app内置的properties
    public static Properties getPropertiesFromRaw(Context context){
        Properties properties = new Properties();
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.setting);
            properties.load(inputStream);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }

    //或者用户自定义的保存在内部app目录的properties
    //如果没有则返回app内的
    public static Properties getUserProperties(Context context, String fileName){
        Properties properties = new Properties();

        try {
            FileInputStream inputStream = new FileInputStream(context.getFilesDir().toString() +File.separator + fileName);
            properties.load(inputStream);
            inputStream.close();
        } catch (FileNotFoundException e) {
//            Log.d(TAG,e.getMessage());
//            Log.d(TAG, "get_raw_properties");
            return  getPropertiesFromRaw(context);
        } catch (IOException e) {
//            Log.d(TAG, "get_raw_properties");
            return  getPropertiesFromRaw(context);
        }

        return properties;
    }

    //保存
    public static void saveUserProperties(Context context, Properties properties, String fileName){
        try {
            OutputStream outputStream = new FileOutputStream(context.getFilesDir().toString() + File.separator + fileName);
            properties.store(outputStream, "");
            outputStream.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG,e.getMessage());
        } catch (IOException e) {
            Log.d(TAG,e.getMessage());
        }
    }

}
