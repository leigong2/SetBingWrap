package keeplive.zune.com.setbingwrapper

import android.annotation.SuppressLint
import android.os.Build
import zune.keeplivelibrary.app.KeepLiveHelper
import zune.keeplivelibrary.util.NotificationUtils

/**
 * Created by leigong2 on 2018-06-16 016.
 */
class MainApp: JavaApp() {
    var PUSH_APP_ID = "2882303761517566170"
    var PUSH_APP_KEY = "5121756688170"

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: MainApp? = null
        internal fun getApp(): MainApp {
            return context!!
        }
    }
    override fun onCreate() {
        super.onCreate()
        context = this
        initKeepLive()
    }

    private fun initKeepLive() {
        KeepLiveHelper.getDefault().init(this, PUSH_APP_ID, PUSH_APP_KEY)
        NotificationUtils().setNotification("我是壁纸标题", "我是壁纸内容", R.mipmap.icon_round_bing)
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            KeepLiveHelper.getDefault().bindService(BindOService::class.java)
        } else {
            KeepLiveHelper.getDefault().bindService(BindService::class.java)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        KeepLiveHelper.getDefault().onTerminate()
    }
}