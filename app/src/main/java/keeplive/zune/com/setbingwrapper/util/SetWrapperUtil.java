package keeplive.zune.com.setbingwrapper.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import keeplive.zune.com.setbingwrapper.MainApp;
import keeplive.zune.com.setbingwrapper.bean.ImageTag;
import keeplive.zune.com.setbingwrapper.bean.ListImageTagBean;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by leigong2 on 2018-03-18 018.
 */

public class SetWrapperUtil {
    private static final int REQUEST_CODE_READ = 1001;
    private List<ImageTag> images;
    private static SetWrapperUtil util = new SetWrapperUtil();
    private boolean hasSdCard;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE
            , Manifest.permission.READ_EXTERNAL_STORAGE};
    private boolean hasCreate;

    private SetWrapperUtil() {
    }

    public static SetWrapperUtil getInstanse() {
        return util;
    }

    @SuppressLint("HandlerLeak")
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1001) {
            } else if (msg.what == 1002) {
                //Todo 加载完毕
                removeImage();
                Collections.sort(images);
                for (int i = 0; i < images.size(); i++) {
                    Log.i("zune: ", "position = " + images.get(i).position + ", url = "
                            + images.get(i).url);
                }
                ListImageTagBean tagBean = new ListImageTagBean();
                tagBean.imageTags = images;
                SharedPreferenceUtil.setObject("image", tagBean);
                setImages();
            } else if (msg.what == 1003) {
                Bitmap bitmap = (Bitmap) msg.obj;
                if (bitmap == null) {
                    return;
                }
                WallpaperManager manager = WallpaperManager.getInstance(MainApp.getJavaApp());
                try {
                    WindowManager wm = (WindowManager) MainApp.getJavaApp()
                            .getSystemService(Context.WINDOW_SERVICE);
                    int width = wm.getDefaultDisplay().getWidth();
                    int height = wm.getDefaultDisplay().getHeight();
                    if (bitmap.getHeight() != height || bitmap.getWidth() != width) {
                        bitmap = resizeBitmap(bitmap);
                    }
                    if (width > height) {
                        return;
                    }
                    Log.i("zune: ", "设置壁纸的size = " + bitmap.getWidth() + ".." + bitmap.getHeight());
                    Rect rect = new Rect(0, 0,width,height);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        manager.setBitmap(bitmap, rect, false
                                , WallpaperManager.FLAG_LOCK| WallpaperManager.FLAG_SYSTEM);
                    } else {
                        manager.setBitmap(bitmap);
                    }
                    Log.i("zune: ", "设置好壁纸了");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void requestPermisson(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, permissions[0]) != PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, permissions[1]) != PERMISSION_GRANTED) {
            // 如果没有授予该权限，就去提示用户请求
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_READ);
        } else {
            permissionResult();
        }
    }

    private void permissionResult() {
        if (requestPermissionListener != null) {
            requestPermissionListener.onRequestPermission();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_READ) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                //用户同意授权
                permissionResult();
            }
        }
    }

    public void createImages() {
        if (hasCreate) {
            return;
        }
        hasCreate = true;
        hasSdCard = MySDUtils.getInstance(MainApp.getJavaApp()).enableSdCard();
        if (hasSdCard) {
            MySDUtils.requestTreeUri();
            DocumentFile rootDir = MySDUtils.getInstance(MainApp.getJavaApp()).getRootDir();
            MySDUtils.getInstance(MainApp.getJavaApp()).makeRootFileDir(rootDir, "Bing");
        }
        if (!isWifiConnected(MainApp.getJavaApp())) {
            if (hasSdCard) {
                dispatchSdLocal();
            } else {
                dispatchLocal();
            }
            return;
        }
        ListImageTagBean imageTagBean = SharedPreferenceUtil.getObject("image");
        if (imageTagBean == null || imageTagBean.imageTags == null || imageTagBean.imageTags.isEmpty()) {
            images = new ArrayList<>();
        } else {
            images = imageTagBean.imageTags;
        }
        if (needUpdate() || images.size() < 8) {
            images.clear();
            for (int i = 0; i < 8; i++) {
                getImages(i);
            }
        } else {
            removeImage();
            Collections.sort(images);
            for (int i = 0; i < images.size(); i++) {
                Log.i("zune: ", "position = " + images.get(i).position + ", url = "
                        + images.get(i).url);
            }
            setImages();
        }
    }

    /*zune： 去重**/
    private void removeImage() {
        if (images.size() == 8) {
            return;
        }
        List<ImageTag> temp = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            if (!contains(temp, images.get(i))) {
                temp.add(images.get(i));
            }
        }
        Log.i("zune: ", "temp size = " + temp.size());
        images.clear();
        images.addAll(temp);
    }

    /*zune： 判断集合中是否有该元素**/
    private boolean contains(List<ImageTag> temp, ImageTag imageTag) {
        for (int i = 0; i < temp.size(); i++) {
            if (temp.get(i).position == imageTag.position) {
                return true;
            }
        }
        return false;
    }

    /*zune： 判断wifi**/
    private boolean isWifiConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWiFiNetworkInfo = mConnectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWiFiNetworkInfo != null) {
                return mWiFiNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    /*zune： 判断壁纸是否需要更新*/
    private boolean needUpdate() {
        long date = SharedPreferenceUtil.getLong("mills", -1L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.CHINA);
        Date dates = new Date(date);
        String format = sdf.format(dates);
        String[] formats = format.split("-");

        String now = sdf.format(new Date(System.currentTimeMillis()));
        String[] nows = now.split("-");
        if (Integer.parseInt(formats[0]) < Integer.parseInt(nows[0])) {
            return true;
        }
        if (Integer.parseInt(formats[1]) < Integer.parseInt(nows[1])) {
            return true;
        }
        if (Integer.parseInt(formats[2]) < Integer.parseInt(nows[2])) {
            return true;
        }
        if (Integer.parseInt(formats[3]) < Integer.parseInt(nows[3])) {
            return true;
        }
        return false;
    }

    /*zune： 判断指定的那天，有没有本地的bing图片**/
    private File hasImage(int nextInt) {
        String SAVE_PIC_PATH = Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)
                ? Environment.getExternalStorageDirectory().getPath() : "/mnt/sdcard";//保存到SD卡
        String SAVE_REAL_PATH = SAVE_PIC_PATH + "/Bing";//保存的确切位置
        Log.i("zune: ", "保存图片 = " + SAVE_REAL_PATH);
        File fileDir = new File(SAVE_REAL_PATH);
        String fileName = getName(nextInt) + ".jpg";
        File file = new File(fileDir, fileName);
        return file;
    }

    private DocumentFile hasSdImage(Integer position) {
        DocumentFile rootDir = MySDUtils.getInstance(MainApp.getJavaApp()).getRootDir();
        if (rootDir == null) {
            return null;
        }
        MySDUtils.getInstance(MainApp.getJavaApp()).makeRootFileDir(rootDir, "Bing");
        for (int i = 0; i < rootDir.listFiles().length; i++) {
            if (rootDir.listFiles()[i] != null && TextUtils.equals("Bing", rootDir.listFiles()[i].getName())
                    && rootDir.listFiles()[i].listFiles() != null && rootDir.listFiles()[i].listFiles().length > 0) {
                DocumentFile[] documentFiles = rootDir.listFiles()[i].listFiles();
                String fileName = getName(position) + ".jpg";
                Log.i("zune: ", "fileName = " + fileName);
                if (documentFiles != null) {
                    for (int i1 = 0; i1 < documentFiles.length; i1++) {
                        if (fileName.equals(documentFiles[i1].getName())) {
                            return documentFiles[i1];
                        }
                    }
                }
                break;
            }
        }
        return null;
    }

    /*zune： 根据距离今天的天数， 获取那时的网络bing图片**/
    private void getImages(final int i) {
        new Thread() {
            @Override
            public void run() {
                String path = "http://cn.bing.com/HPImageArchive.aspx?idx=" + i + "&n=1";
                try {
                    String htmlContent = getHtml(path);
                    String[] urls = htmlContent.split("url");
                    String imageUrl = null;
                    for (String url : urls) {
                        if (url.endsWith(".jpg</") || url.endsWith(".png</")) {
                            String temp = "http://cn.bing.com" + url.substring(1, url.length() - 2);
                            imageUrl = temp.replace("1366x768", "768x1366");
                            break;
                        }
                    }
                    ImageTag imageTag = new ImageTag();
                    imageTag.url = imageUrl;
                    imageTag.position = i;
                    images.add(imageTag);
                } catch (Exception e) {
                    Log.i("zune: ", "getImages e = " + e);
                }
                if (images.size() == 8) {
                    handler.sendEmptyMessage(1002);
                }
            }
        }.start();
    }

    /*zune： 从xml的url地址获取网络bing的地址**/
    private String getHtml(String path) throws Exception {
        // 通过网络地址创建URL对象
        URL url = new URL(path);
        // 根据URL
        // 打开连接，URL.openConnection函数会根据URL的类型，返回不同的URLConnection子类的对象，这里URL是一个http，因此实际返回的是HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // 设定URL的请求类别，有POST、GET 两类
        conn.setRequestMethod("GET");
        //设置从主机读取数据超时（单位：毫秒）
        conn.setConnectTimeout(5000);
        //设置连接主机超时（单位：毫秒）
        conn.setReadTimeout(5000);
        // 通过打开的连接读取的输入流,获取html数据
        InputStream inStream = conn.getInputStream();
        // 得到html的二进制数据
        byte[] data = readInputStream(inStream);
        // 是用指定的字符集解码指定的字节数组构造一个新的字符串
        String html = new String(data, "utf-8");
        return html;
    }

    /*zune： 读取流，转换为字节数组**/
    private byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        return outStream.toByteArray();
    }

    /*zune： 随机选择的8天图片，获取一张，并转换为bitmap， 设置壁纸**/
    private void setImages() {
        Log.i("zune: ", "images size = " + images.size());
        if (images.size() == 0) {
            return;
        }
        SharedPreferenceUtil.setLong("mills", System.currentTimeMillis());
        new Thread() {
            @Override
            public void run() {
                try {
                    // 通过网络地址创建URL对象
                    Random random = new Random();
                    int nextInt = random.nextInt(images.size());
                    Log.i("zune: ", "nextInt = " + nextInt);
                    if (hasSdCard) {
                        DocumentFile file = hasSdImage(images.get(nextInt).position);
                        Log.i("zune: ", "setImages file = " + images.get(nextInt).position);
                        if (file != null) {
                            Bitmap sdBitmap = getSdBitmap(file);
                            Log.i("zune: ", "setImages bitmap = " + images.get(nextInt).url);
                            if (sdBitmap != null) {
                                Message msg = new Message();
                                msg.what = 1003;
                                msg.obj = sdBitmap;
                                handler.sendMessage(msg);
                                return;
                            }
                        }
                    } else {
                        File file = hasImage(images.get(nextInt).position);
                        if (file != null) {
                            Bitmap image = getBitmap(file);
                            if (file.exists() && image != null) {
                                Message msg = new Message();
                                msg.what = 1003;
                                msg.obj = image;
                                handler.sendMessage(msg);
                                return;
                            }
                        }
                    }
                    URL url = new URL(images.get(nextInt).url);
                    // 根据URL
                    // 打开连接，URL.openConnection函数会根据URL的类型，返回不同的URLConnection子类的对象，这里URL是一个http，因此实际返回的是HttpURLConnection
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    // 设定URL的请求类别，有POST、GET 两类
                    conn.setRequestMethod("GET");
                    //设置从主机读取数据超时（单位：毫秒）
                    conn.setConnectTimeout(5000);
                    //设置连接主机超时（单位：毫秒）
                    conn.setReadTimeout(5000);
                    InputStream inputStream = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        Log.i("zune: ", "网络resize之前的size = " + bitmap.getWidth() + ".." + bitmap.getHeight());
                        Bitmap resize = resizeBitmap(bitmap);
                        Log.i("zune: ", "网络resize之后的size = " + resize.getWidth() + ".." + resize.getHeight());
                        if (hasSdCard) {
                            saveSdBitmap(resize, images.get(nextInt).position);
                        } else {
                            saveBitmap(resize, images.get(nextInt).position);
                        }
                        Message msg = new Message();
                        msg.what = 1003;
                        msg.obj = resize;
                        handler.sendMessage(msg);
                    } else {
                        if (hasSdCard) {
                            dispatchSdLocal();
                        } else {
                            dispatchLocal();
                        }
                    }
                } catch (Exception e) {
                    Log.i("zune: ", "setImages e = " + e);
                    if (hasSdCard) {
                        dispatchSdLocal();
                    } else {
                        dispatchLocal();
                    }
                }
            }
        }.start();
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        WindowManager wm = (WindowManager) MainApp.getJavaApp()
            .getSystemService(Context.WINDOW_SERVICE);
        float width = wm.getDefaultDisplay().getWidth();
        float height = wm.getDefaultDisplay().getHeight();
        float bitmapHeight = bitmap.getHeight();
        float bitmapWidth = bitmap.getWidth();
        Matrix matrix = new Matrix();
        matrix.postScale(width / bitmapWidth, height / bitmapHeight); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth()
                , bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    /*zune： 从本地bing图片中，随机选择一张，设置壁纸**/
    private void dispatchLocal() {
        String SAVE_PIC_PATH = Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)
                ? Environment.getExternalStorageDirectory().getPath() : "/mnt/sdcard";//保存到SD卡
        String SAVE_REAL_PATH = SAVE_PIC_PATH + "/Bing";//保存的确切位置
        Log.i("zune: ", "保存图片 = " + SAVE_REAL_PATH);
        File fileDir = new File(SAVE_REAL_PATH);
        File[] files = fileDir.listFiles();
        if (files != null && files.length > 0) {
            Random random = new Random();
            int nextInt = random.nextInt(files.length);
            File file = files[nextInt];
            Bitmap image = getBitmap(file);
            if (file.exists() && image != null) {
                Message msg = new Message();
                msg.what = 1003;
                msg.obj = image;
                handler.sendMessage(msg);
                return;
            }
        } else {
        }
    }

    /*zune： 从本地bing图片中，随机选择一张，设置壁纸**/
    private void dispatchSdLocal() {
        DocumentFile rootDir = MySDUtils.getInstance(MainApp.getJavaApp()).getRootDir();
        if (rootDir == null) {
            return;
        }
        DocumentFile[] documentFiles = rootDir.listFiles();
        DocumentFile[] selectFiles = null;
        for (int i = 0; i < documentFiles.length; i++) {
            DocumentFile documentFile = documentFiles[i];
            if (documentFile != null && TextUtils.equals("Bing", documentFile.getName()) && documentFile.listFiles().length > 0) {
                selectFiles = documentFile.listFiles();
                break;
            }
        }
        if (selectFiles != null && selectFiles.length > 0) {
            Random random = new Random();
            int nextInt = random.nextInt(selectFiles.length);
            DocumentFile file = selectFiles[nextInt];
            Bitmap image = getSdBitmap(file);
            if (file.exists() && image != null) {
                Message msg = new Message();
                msg.what = 1003;
                msg.obj = image;
                handler.sendMessage(msg);
                return;
            }
        } else {
        }
    }

    /*zune： file转换bitmap**/
    private Bitmap getBitmap(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            if (bitmap != null) {
                Log.i("zune: ", "本地resize之前的size = " + bitmap.getWidth() + ".." + bitmap.getHeight());
            }
            return bitmap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*zune： file转换bitmap**/
    private Bitmap getSdBitmap(DocumentFile file) {
        if (file == null) {
            return null;
        }
        try {
            ContentResolver contentResolver = MainApp.getJavaApp().getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(file.getUri());
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                Log.i("zune: ", "本地resize之前的size = " + bitmap.getWidth() + ".." + bitmap.getHeight());
            }
            return bitmap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*zune： 将指定那一天的bitmap，保存到本地**/
    private void saveBitmap(Bitmap bitmap, int nextInt) {
        String SAVE_PIC_PATH = Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)
                ? Environment.getExternalStorageDirectory().getPath() : "/mnt/sdcard";//保存到SD卡
        String SAVE_REAL_PATH = SAVE_PIC_PATH + "/Bing";//保存的确切位置
        Log.i("zune: ", "保存图片 = " + SAVE_REAL_PATH);
        File fileDir = new File(SAVE_REAL_PATH);
        if (!fileDir.exists()) {
            boolean mkdir = fileDir.mkdirs();
            Log.i("zune: ", "mkDir = " + mkdir);
        }
        String fileName = getName(nextInt) + ".jpg";
        File file = new File(fileDir, fileName);
        try {
            if (!file.exists()) {
                boolean newFile = file.createNewFile();
                Log.i("zune: ", "newFile = " + newFile);
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("zune: ", "file e = " + e);
        }

        // 最后通知图库更新
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        MainApp.getJavaApp().sendBroadcast(intent);
    }

    /*zune： 将指定那一天的bitmap，保存到本地**/
    private void saveSdBitmap(Bitmap bitmap, int nextInt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        InputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        String fileName = getName(nextInt) + ".jpg";
        MySDUtils.getInstance(MainApp.getJavaApp())
                .makeInputStreamFile(isBm, "image/*", "Bing/" + fileName);
    }

    /*zune： 根据索引，获取指定的具体是哪一天**/
    private String getName(int nextInt) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        String format = sdf.format(new Date(System.currentTimeMillis() - nextInt * 24 * 1000 * 3600L));
        return format;
    }

    public interface RequestPermissionListener {
        void onRequestPermission();
    }
    private RequestPermissionListener requestPermissionListener;

    public void setRequestPermissionListener(RequestPermissionListener requestPermissionListener) {
        this.requestPermissionListener = requestPermissionListener;
    }
}
