package com.myapp.sensordatacollection.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class FileUtils {
    private static final String TAG = "FileUtils";

    //获取app内部缓存
    public static String getAppCachePath(Context context, String type){
        File appCacheDir = null;

        if (type == null || type == ""){
            appCacheDir = context.getCacheDir();
        }else {
            appCacheDir = new File(context.getCacheDir(), type);
        }

        if (!appCacheDir.exists() && !appCacheDir.mkdirs()){
            Log.d(TAG,"get cache directory fail");
        }

        return appCacheDir.getPath() + File.separator;
    }

    public static void deleteCacheFile(Context context, String type, String fileName){
        String filePath = getAppCachePath(context, type);
        File file = new File(filePath + fileName);
        if (file.exists()){
            file.delete();
        }
    }
}
