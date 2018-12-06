package keeplive.zune.com.setbingwrapper;

import android.app.Application;

/**
 * Created by leigong2 on 2018-06-16 016.
 */

public class JavaApp extends Application {
    private static JavaApp javaApp;
    @Override
    public void onCreate() {
        super.onCreate();
        javaApp = this;
    }
    public static JavaApp getJavaApp() {
        return javaApp;
    }
}
