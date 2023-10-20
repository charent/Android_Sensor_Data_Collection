package com.myapp.sensordatacollection.utils;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

public class ToastUtils {
    private static Toast toast = null;

    public static void show(Context context, String text){
        try {
            if (toast != null){
                toast.setText(text);
            }else {
                toast = Toast.makeText(context, text,Toast.LENGTH_SHORT);
            }
            toast.show();
        } catch (Exception e){
            //在子线程中要loop
            if (Looper.myLooper() == null){
                Looper.prepare();
            }
            Toast.makeText(context, text,Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }
}
