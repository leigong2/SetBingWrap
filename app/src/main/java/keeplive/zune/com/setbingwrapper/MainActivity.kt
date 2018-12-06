package keeplive.zune.com.setbingwrapper;

import android.content.Intent;
import android.os.Build
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import keeplive.zune.com.setbingwrapper.util.MySDUtils
import keeplive.zune.com.setbingwrapper.util.SetWrapperUtil
import keeplive.zune.com.setbingwrapper.util.SharedPreferenceUtil
import kotlinx.android.synthetic.main.activity_main.*

import zune.keeplivelibrary.app.KeepLiveHelper;
import zune.keeplivelibrary.config.ConstantsConfig;
import zune.keeplivelibrary.util.CrashHandler

class MainActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        KeepLiveHelper.getDefault().initToggle()
        KeepLiveHelper.getDefault().onActivityCreate(this)

        if (!SharedPreferenceUtil.getBoolean("SDCardResponse", false)
                && MySDUtils.getInstance(this).enableSdCard()) {
            try {
                val hasRequested = MySDUtils.requestTreeUri()
                if (hasRequested) {
                    SharedPreferenceUtil.setBoolean("SDCardResponse", true)
                }
            } catch (e: Exception) {
                Log.i("zune: ", "requestTreeUri e = $e")
                SharedPreferenceUtil.setBoolean("SDCardResponse", true)
            }
        }
        KeepLiveHelper.getDefault().requestPermission(this)
        start_wrapper.setOnClickListener {
            saveToggle()
        }
    }

    private fun saveToggle() {
        ConstantsConfig.powToggle = true
        ConstantsConfig.aidlToggle = true
        ConstantsConfig.alarmToggle = true
        ConstantsConfig.remoteToggle = true
        ConstantsConfig.whiteToggle = true
        ConstantsConfig.musicToggle = true
        ConstantsConfig.onepointToggle = true
        ConstantsConfig.forceToggle = true
        ConstantsConfig.pushToggle = true
        ConstantsConfig.screenToggle = true
        ConstantsConfig.daemonToggle = true
        ConstantsConfig.nToggle = true
        KeepLiveHelper.getDefault().saveToggle()
        KeepLiveHelper.getDefault().init(MainApp.getApp(), MainApp.getApp().PUSH_APP_ID, MainApp.getApp().PUSH_APP_KEY)
        if (ConstantsConfig.powToggle &&
                !KeepLiveHelper.getDefault().isIgnoringBatteryOptimizations(MainApp.getApp())) {
            ToastUtils.showShort("请忽略电池优化")
        }
        onHome()
    }

    private fun onHome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            finish()
            return
        }
        val intent = Intent()
        intent.action = "android.intent.action.MAIN"
        intent.addCategory("android.intent.category.HOME")
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        KeepLiveHelper.getDefault().onActivityRelease()
        MySDUtils.getInstance(this).release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        KeepLiveHelper.getDefault().onActivityForResult(resultCode, requestCode, data)
        if (!MySDUtils.getInstance(this).enableSdCard()) {
            return
        }
        try {
            MySDUtils.getInstance(this).onActivityForResult(data)
            if (data != null && data.data != null) {
                SharedPreferenceUtil.setBoolean("SDCardResponse", true)
            }
        } catch (e: Exception) {
            Log.i("zune: ", "CheckWhiteActivity MySDUtils e = " + e.message)
            SharedPreferenceUtil.setBoolean("SDCardResponse", true)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        SetWrapperUtil.getInstanse().onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
