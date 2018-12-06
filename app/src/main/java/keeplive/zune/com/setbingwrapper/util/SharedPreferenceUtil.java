package keeplive.zune.com.setbingwrapper.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import keeplive.zune.com.setbingwrapper.MainApp;
import keeplive.zune.com.setbingwrapper.bean.ListImageTagBean;

/**
 * Created by leigong2 on 2018-03-18 018.
 */

public class SharedPreferenceUtil {
    private static String PREDERENCE_NAME = "SharedPreferenceUtil";


    public static void init() {
        PREDERENCE_NAME = MainApp.getJavaApp().getPackageName();
    }

    private static Context getContext() {
        return MainApp.getJavaApp();
    }

    public static void setBoolean(String key, Boolean value) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);

        if (value != sp.getBoolean(key, false)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(key, value);
            editor.commit();
        }
    }

    public static boolean getBoolean(String key, Boolean defaultValue) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);
        return sp.getBoolean(key, defaultValue);
    }

    public static void setString(String key, String value) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);

        if (!sp.getString(key, "").equals(value)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(key, value);
            editor.commit();
        }
    }

    public static String getString(String key, String defaultValue) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);
        return sp.getString(key, defaultValue);
    }

    public static void setLong(String key, Long value) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);

        if (value != sp.getLong(key, 0xFF)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong(key, value);
            editor.commit();
        }
    }

    public static long getLong(String key, Long defaultValue) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);
        return sp.getLong(key, defaultValue);
    }

    public static void setInt(String key, int value) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);
        if (value != sp.getInt(key, 0xFF)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(key, value);
            editor.commit();
        }
    }

    public static int getInt(String key, int defaultValue) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);
        return sp.getInt(key, defaultValue);
    }

    public static void setOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void clearOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = getContext().getSharedPreferences(PREDERENCE_NAME, 0);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    /**
     * 保存List
     *
     * @param tag
     * @param datalist
     */
    public static  <T> void setDataList(String tag, List<T> datalist) {
        if (null == datalist || datalist.size() <= 0)
            return;
        Gson gson = new Gson();
        //转换成json数据，再保存
        String strJson = gson.toJson(datalist);
        setString(tag, strJson);
    }

    /**
     * 获取List
     *
     * @param tag
     * @return
     */
    public static <T> List<T> getDataList(String tag) {
        List<T> datalist = new ArrayList<>();
        String strJson = getString(tag, "");
        if (TextUtils.isEmpty(strJson)) {
            return datalist;
        }
        Gson gson = new Gson();
        datalist = gson.fromJson(strJson, new TypeToken<List<T>>() {
        }.getType());
        return datalist;
    }

    public synchronized static void setObject(String tag, ListImageTagBean tagBean) {
        Gson gson = new Gson();
        //转换成json数据，再保存
        String strJson = gson.toJson(tagBean, ListImageTagBean.class);
        setString(tag, strJson);
    }

    public static ListImageTagBean getObject(String tag) {
        String strJson = getString(tag, "");
        Gson gson = new Gson();
        ListImageTagBean imageTagBean = gson.fromJson(strJson, ListImageTagBean.class);
        return imageTagBean;
    }
}
