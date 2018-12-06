package keeplive.zune.com.setbingwrapper.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import static android.os.Build.VERSION_CODES.M;

/**
 * create by wangzhilong at 2017/5/4 004
 * 该工具能获取安卓6.0外置sd卡上的Android/data目录下的数据,并读写
 * 需要加上权限
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 * <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
 * 其中read权限需要动态申请
 * treeUri获取方式
 * Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
 * startActivityForResult(intent, REQUEST_DOCUMENT);
 * 在onActivityResult中通过treeUri = data.getData();回调得到treeUri
 */
public class MySDUtils {

    private static final int REQUEST_DOCUMENT = 1001;
    private static MySDUtils mySDUtils;
    private static Context activity;
    private static Uri treeUri;

    public static MySDUtils getInstance(Context context) {
        activity = context;
        synchronized (MySDUtils.class) {
            if (mySDUtils == null) {
                mySDUtils = new MySDUtils();
            }
            if (mySDUtils.enableSdCard()) {
                requestTreeUri();
            }
        }
        SharedPreferences sp = activity.getSharedPreferences("sdcard_treeuri.xml", Context.MODE_PRIVATE);
        if (!TextUtils.isEmpty(sp.getString("treeuri", ""))) {
            treeUri = Uri.parse(sp.getString("treeuri", ""));
        }
        return mySDUtils;
    }
    public static boolean requestTreeUri() {
        SharedPreferences sp = activity.getSharedPreferences("sdcard_treeuri.xml", Context.MODE_PRIVATE);
        if (!TextUtils.isEmpty(sp.getString("treeuri", ""))) {
            treeUri = Uri.parse(sp.getString("treeuri", ""));
            return true;
        }
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        if (activity instanceof Activity) {
            ((Activity)activity).startActivityForResult(intent, REQUEST_DOCUMENT);
        }
        return false;
    }
    public void onActivityForResult(Intent data) {
        treeUri = data.getData();
        SharedPreferences sp = activity.getSharedPreferences("sdcard_treeuri.xml", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString("treeuri", treeUri.toString());
        edit.commit();
    }
    public void release() {
        mySDUtils = null;
        activity = null;
    }

    /**
     * zune: 获取路所有存储器路径,一般情况下position = 0是内置存储,position= 1是外置存储
     **/
    public String[] getExtSDCardPath() {
        StorageManager storageManager = (StorageManager) activity.getSystemService(Context
                .STORAGE_SERVICE);
        try {
            Class<?>[] paramClasses = {};
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", paramClasses);
            getVolumePathsMethod.setAccessible(true);
            Object[] params = {};
            Object invoke = getVolumePathsMethod.invoke(storageManager, params);
            return (String[]) invoke;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean enableSdCard() {
        if (getExtSDCardPath() != null && getExtSDCardPath().length > 1 && Build.VERSION.SDK_INT >= M) {
            return true;
        }
        return false;
    }

    public DocumentFile getRootDir() {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(activity, treeUri);
        return pickedDir;
    }

    /*zune： 在选择的文件夹里面创建xxx/xxx格式文件夹**/
    public boolean makeRootFileDir(DocumentFile file, String fileName) {
        String[] split = fileName.split("/");
        if (!contains(file, split[0])) {
            file.createDirectory(split[0]);
        }
        DocumentFile temp = null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            if (i > 0 && i < split.length - 1) {
                sb.append(split[i]).append("/");
            } else if (i > 0) {
                sb.append(split[i]);
            }
        }
        if (file == null) {
            return false;
        }
        for (int i = 0; i < file.listFiles().length; i++) {
            if (TextUtils.equals(split[0], file.listFiles()[i].getName())) {
                temp = file.listFiles()[i];
            }
        }
        if (split.length > 1 && temp != null) {
            makeRootFileDir(temp, sb.toString());
        }
        return false;
    }
    /**
     * @prame mimeType = "text/*";"image/*";"audio/*";"video/*";
     * zune： 在选择的文件夹里面创建xxx/xxx.txt格式的文件， 需要指定文件类型**/
    public DocumentFile makeRootFile(DocumentFile file, String mimeType, String fileName) {
        if (file == null) {
            return null;
        }
        DocumentFile documentFile = null;
        String[] split = fileName.split("/");
        if (split.length == 1 && !contains(file, split[0])) {
            documentFile = file.createFile(mimeType, split[0]);
        } else if (!contains(file, split[0])){
            file.createDirectory(split[0]);
        } else if (split.length == 1) {
            for (int i = 0; i < file.listFiles().length; i++) {
                if (TextUtils.equals(split[0], file.listFiles()[i].getName())) {
                    documentFile = file.listFiles()[0];
                }
            }
        }
        if (split.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                if (i > 0 && i < split.length - 1) {
                    sb.append(split[i]).append("/");
                } else if (i > 0) {
                    sb.append(split[i]);
                }
            }
            DocumentFile temp = null;
            for (int i = 0; i < file.listFiles().length; i++) {
                if (TextUtils.equals(file.listFiles()[i].getName(), split[0])) {
                    temp = file.listFiles()[i];
                }
            }
            documentFile = makeRootFile(temp, mimeType, sb.toString());
        }
        return documentFile;
    }

    private boolean contains(DocumentFile pickedDir, String fileDir) {
        DocumentFile[] documentFiles = pickedDir.listFiles();
        for (int i = 0; i < documentFiles.length; i++) {
            String name = documentFiles[i].getName();
            if (TextUtils.equals(name, fileDir)) {
                return true;
            }
        }
        return false;
    }

    /**
     * zune: 将一个输入流指定一个类型,复制到目标文件,格式为"路径/路径/路径/文件名"
     * @prame mimeType = "text/*";"image/*";"audio/*";"video/*";
     **/
    public boolean makeInputStreamFile(InputStream in, String mimeType, String targetPath) {
        DocumentFile documentFile = makeRootFile(getRootDir(), mimeType, targetPath);
        if (documentFile == null) {
            return false;
        }
        try {
            /**zune: 将流拷贝到新文件**/
            Uri uri = documentFile.getUri();
            Log.i("c:zune:", "uri = " + uri.getPath());
            OutputStream out = activity.getContentResolver().openOutputStream(uri);
            int b;
            byte[] arr = new byte[1024];
            while ((b = in.read(arr)) != -1) {
                out.write(arr, 0, b);
            }
            out.close();
            in.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("c:zune:", "copy失败e = " + e);
            return false;
        }
    }

    /**
     * zune: 在包名下创建一个文件(夹),创建文件夹只需将mimeType设定为null
     *
     * @prame mimeType = "text/*";"image/*";"audio/*";"video/*";
     **/
    public boolean makeFile(String mimeType, String fileName) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(activity, treeUri);
        if (pickedDir == null) {
            return false;
        }
        for (DocumentFile file : pickedDir.listFiles()) {
            if (file.getName().equals("Android")) {
                DocumentFile[] documentFiles = file.listFiles();
                for (DocumentFile documentFile : documentFiles) {
                    if (documentFile.getName().equals("data")) {
                        DocumentFile[] datas = documentFile.listFiles();
                        for (DocumentFile data : datas) {
                            if (data.getName().equals(activity.getPackageName())) {
                                return createFile(mimeType, data, fileName);
                            }
                        }
                        DocumentFile data = documentFile.createDirectory(activity.getPackageName());
                        return createFile(mimeType, data, fileName);
                    }
                }
            }
        }
        return false;
    }

    /**
     * zune: 在一个文件夹中创建一个文件,用'/'切割文件夹
     **/
    private boolean createFile(String mimeType, DocumentFile data, String fileName) {
        String[] names = fileName.split("/");
        boolean hasDir = false;
        boolean createSuccess = false;
        if (data == null) {
            return false;
        }
        for (DocumentFile file : data.listFiles()) {
            if (file.getName().equals(names[0])) {
                if (names.length > 1) {
                    String substring = fileName.substring(names[0].length() + 1);
                    createSuccess = createFile(mimeType, file, substring);
                    hasDir = true;
                }
            }
        }
        if (!hasDir) {
            if (names.length == 1 & mimeType != null) {
                boolean hasFile = false;
                DocumentFile[] files = data.listFiles();
                for (DocumentFile file : files) {
                    if (file.getName().equals(names[0])) {
                        hasFile = true;
                    }
                }
                if (files.length > 0 && hasFile) {

                } else {
                    DocumentFile file1 = data.createFile(mimeType, names[0]);
                    if (file1.exists()) {
                        createSuccess = true;
                    }
                }
            } else if (names.length > 1) {
                DocumentFile file = data.createDirectory(names[0]);
                String substring = fileName.substring(names[0].length() + 1);
                createSuccess = createFile(mimeType, file, substring);
            } else {
                boolean hasFile = false;
                DocumentFile[] files = data.listFiles();
                for (DocumentFile file : files) {
                    if (file.getName().equals(names[0])) {
                        hasFile = true;
                    }
                }
                if (files.length > 0 && hasFile) {

                } else {
                    DocumentFile directory = data.createDirectory(names[0]);
                    if (directory.exists()) {
                        createSuccess = true;
                    }
                }
            }
        }
        return createSuccess;
    }

    /**
     * zune: 将一个输入流指定一个类型,复制到目标文件,格式为"路径/路径/路径/文件(夹)名"
     **/
    public boolean copyFile(InputStream in, String mimeType, String targetPath) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(activity, treeUri);
        boolean fileIsExist = false;
        boolean weichatIsExist = false;
        DocumentFile newFile = null;
        if (pickedDir == null) {
            return false;
        }
        /**zune: 根文件夹的列表**/
        for (DocumentFile file : pickedDir.listFiles()) {
            Log.d("zune", "fileName " + file.getName());
            if (file.getName().equals("Android")) {
                DocumentFile[] documentFiles = file.listFiles();
                for (DocumentFile documentFile : documentFiles) {
                    if (documentFile.getName().equals("data")) {
                        pickedDir = documentFile;
                        DocumentFile[] datas = documentFile.listFiles();
                        for (DocumentFile data : datas) {
                            if (data.getName().equals(activity.getPackageName())) {
                                pickedDir = data;
                                weichatIsExist = true;
                                DocumentFile[] caches = data.listFiles();
                                for (DocumentFile cache : caches) {
                                    if (cache.getName().contains(targetPath)) {
                                        fileIsExist = true;
                                        newFile = cache;
                                        /**zune: 复用原文件**/
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        /**zune: 创建新文件**/
        if (!fileIsExist) {
            DocumentFile weichat;
            if (weichatIsExist) {
                weichat = pickedDir;
            } else {
                weichat = pickedDir.createDirectory(activity.getPackageName());
            }
            newFile = weichat.createFile(mimeType, targetPath);
        }
        OutputStream out = null;
        if (newFile == null) {
            return false;
        }
        try {
            /**zune: 将流拷贝到新文件**/
            Uri uri = newFile.getUri();
            Log.i("c:zune:", "uri = " + uri.getPath());
            out = activity.getContentResolver().openOutputStream(uri);
            int b;
            byte[] arr = new byte[1024];
            while ((b = in.read(arr)) != -1) {
                out.write(arr, 0, b);
            }
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("c:zune:", "copy失败e = " + e);
            return false;
        }
    }

    /**
     * zune: 将源文件夹路径复制到目标文件夹路径,格式为"路径/路径/路径/文件(夹)名"
     **/
    public void copyFile(String source, String target) {
        try {
            DocumentFile rootFiles = getRootFiles();
            DocumentFile sourceFiles = getDocumentFileFormPath(rootFiles, source);
            DocumentFile targetFiles = getDocumentFileFormPath(rootFiles, target);
            if (target.equals(activity.getPackageName()))
                copySimpleFile(sourceFiles, rootFiles);
            else
                copySimpleFile(sourceFiles, targetFiles);
        } catch (Exception e) {
            Log.e("c:zune:", "e = " + e);
            Toast.makeText(activity, "路径错误", Toast.LENGTH_SHORT).show();
        }
    }

    private static void copySimpleFile(DocumentFile sourceFiles, DocumentFile targetFiles) {
        boolean hasFile = false;
        if (sourceFiles == null || targetFiles == null) {
            return;
        }
        if (sourceFiles.isFile()) {
            targetFiles.createFile(sourceFiles.getType(), sourceFiles.getName());
            return;
        } else if (sourceFiles.isDirectory()) {
            for (DocumentFile file : targetFiles.listFiles()) {
                if (file.getName().equals(sourceFiles.getName())) {
                    hasFile = true;
                }
            }
            if (!hasFile)
                targetFiles.createDirectory(sourceFiles.getName());
            if (sourceFiles.listFiles() == null) {
                return;
            } else {
                for (DocumentFile file : sourceFiles.listFiles()) {
                    for (DocumentFile documentFile : targetFiles.listFiles()) {
                        if (documentFile.getName().equals(sourceFiles.getName())) {
                            copySimpleFile(file, documentFile);
                        }
                    }
                }
            }
        }
    }

    /**
     * zune: 根据treeUri在包名下面删除一个文件,path是文件(夹)名格式为"路径/路径/路径/文件(夹)名"
     **/
    public boolean deleteFile(String path) {
        try {
            String[] paths = path.split("/");
            DocumentFile rootFiles = getRootFiles();
            dispatchDelete(rootFiles, paths);
            return checkDelete(rootFiles, paths);
        } catch (Exception e) {
            Log.e("c:zune:", "e = " + e);
            Toast.makeText(activity, "路径错误", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean checkDelete(DocumentFile rootFiles, String[] paths) {
        for (DocumentFile file : rootFiles.listFiles()) {
            if (file.getName().equals(paths[0])) {
                if (paths.length == 1) {
                    return false;
                } else {
                    String[] newPaths = new String[paths.length - 1];
                    for (int i = 0; i < paths.length - 1; i++) {
                        newPaths[i] = paths[i + 1];
                    }
                    return checkDelete(file, newPaths);
                }
            }
        }
        return true;
    }

    private void dispatchDelete(DocumentFile rootFiles, String[] paths) {
        if (rootFiles == null) {
            return;
        }
        for (DocumentFile file : rootFiles.listFiles()) {
            if (paths.length > 1) {
                String[] newPath = new String[paths.length - 1];
                for (int i = 0; i < paths.length - 1; i++) {
                    newPath[i] = paths[i + 1];
                }
                for (int i = 0; i < paths.length; i++) {
                    if (paths[i].equals(file.getName())) {
                        dispatchDelete(file, newPath);
                    }
                }
            } else if (paths[0].equals(file.getName())) {
                file.delete();
            }
        }
    }

    /**
     * zune: 将源文件夹移动到目标文件source和target格式为"路径/路径/路径/文件(夹)名"
     **/

    public void moveFile(String source, String target) {
        try {
            copyFile(source, target);
            deleteFile(source);
        } catch (Exception e) {
            Log.e("c:zune:", "e = " + e);
            Toast.makeText(activity, "路径错误", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * zune: mimeType如果是文件的话必须要指定,否则默认是文件夹
     **/
    public void renameFile(String source, String target) {
        try {
            DocumentFile rootFiles = getRootFiles();
            String[] sources = source.split("/");
            String[] targets = target.split("/");
            dispatchRename(rootFiles, sources, targets);
        } catch (Exception e) {
            Log.e("c:zune:", "e = " + e);
            Toast.makeText(activity, "路径错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchRename(DocumentFile rootFiles, String[] sources, String[] targets) {
        if (rootFiles == null) {
            return;
        }
        for (DocumentFile file : rootFiles.listFiles()) {
            if (file.getName().equals(sources[0])) {
                if (sources.length == 1) {
                    if (file.getName().equals(sources[0])) {
                        if (file.isFile()) {
                            rootFiles.createFile(file.getType(), targets[0]);
                        } else {
                            DocumentFile target = rootFiles.createDirectory(targets[0]);
                            if (file.listFiles().length > 0) {
                                for (DocumentFile documentFile : file.listFiles()) {
                                    copySimpleFile(documentFile, target);
                                }
                            }
                        }
                        file.delete();
                    }
                } else {
                    String[] newSources = new String[sources.length - 1];
                    for (int i = 0; i < sources.length - 1; i++) {
                        newSources[i] = sources[i + 1];
                    }
                    String[] newTargets = new String[targets.length - 1];
                    for (int i = 0; i < targets.length - 1; i++) {
                        newTargets[i] = targets[i + 1];
                    }
                    dispatchRename(file, newSources, newTargets);
                }
            }
        }
    }

    /**
     * zune: 根据路径获取DocumentFile
     **/
    private DocumentFile getDocumentFileFormPath(DocumentFile rootFiles, String source) {
        DocumentFile lastFile = null;
        String[] sources = source.split("/");
        if (rootFiles != null) {
            for (DocumentFile file : rootFiles.listFiles()) {
                if (file.getName().equals(sources[0])) {
                    if (sources.length > 1) {
                        StringBuffer newSources = new StringBuffer();
                        for (int i = 0; i < sources.length - 1; i++) {
                            if (i == sources.length - 2)
                                newSources.append(sources[i + 1]);
                            else
                                newSources.append(sources[i + 1] + "/");
                        }
                        lastFile = getDocumentFileFormPath(file, newSources.toString());
                    } else {
                        lastFile = file;
                    }
                }
            }
        }
        return lastFile;
    }

    /**
     * zune: 根据包名获取根DocumentFile
     **/
    private DocumentFile getRootFiles() {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(activity, treeUri);
        if (pickedDir == null) {
            return null;
        }
        DocumentFile rootFiles = null;
        for (DocumentFile file : pickedDir.listFiles()) {
            if (file.getName().equals("Android")) {
                for (DocumentFile documentFile : file.listFiles()) {
                    if (documentFile.getName().equals("data")) {
                        for (DocumentFile file1 : documentFile.listFiles()) {
                            if (file1.getName().equals(activity.getPackageName())) {
                                rootFiles = file1;
                            }
                        }
                        if (rootFiles == null) {
                            DocumentFile file1 = documentFile.createDirectory(activity.getPackageName());
                            if (file1.getName().equals(activity.getPackageName())) {
                                rootFiles = file1;
                            }
                        }
                    }
                }
            }
        }
        return rootFiles;
    }
}
