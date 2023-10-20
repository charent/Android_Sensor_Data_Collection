package com.myapp.sensordatacollection.utils;

public class Tools {
    public static final String TAG = "Tools";

    //去除空格
    public static String strip(String string){
        while (string.startsWith(" ")){
            string = string.substring(1, string.length()).trim();
        }

        while (string.endsWith(" ")){
            string = string.substring(0, string.length() - 1).trim();
        }

        return  string;
    }

    public static boolean isIPAddress(String ip){
        ip = strip(ip);

        if (ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")){
            String string[] = ip.split("\\.");
            if (string.length != 4){
                return false;
            }

            for (int i = 0; i < string.length; ++i){
                if (Integer.parseInt(string[i])  > 255){
                    return  false;
                }
            }
        }else {
            return false;
        }

        return true;
    }
}
