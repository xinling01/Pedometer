package utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by linger on 2018/10/21.
 */

public class SaveKeyValues {
    public static SharedPreferences sharePreferences;//轻量级存储
    private static SharedPreferences.Editor editor;    //修改存储内容
    //初始化轻量级存储
    public static void createSharePreferences(Context context){
        String appName=context.getPackageName();   //获取包名
        sharePreferences=context.getSharedPreferences(appName,Context.MODE_WORLD_WRITEABLE);
        editor=sharePreferences.edit();   //修改存储内容对象
    }
    //判断SharedPreferences是否被创建
    public static boolean isUnCreate(){
        boolean result=(sharePreferences==null)?true:false;
        if(result){
            Log.e("提醒","SharedPreferences未被创建");
        }
        return result;
    }
    //保存int类型值的方法
    public static boolean putIntValues(String key,int values){
        if(isUnCreate()){
            return false;
        }
        editor.putInt(key,values);
        return editor.commit();
    }
    //获取int 类型值的方法
    public static int getIntValues(String key,int defValue){
        if(isUnCreate()){
            return 0;
        }
        int int_value=sharePreferences.getInt(key,defValue);
        return int_value;
    }
}
