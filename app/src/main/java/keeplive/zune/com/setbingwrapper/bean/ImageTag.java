package keeplive.zune.com.setbingwrapper.bean;

import android.support.annotation.NonNull;

/**
 * Created by leigong2 on 2018-03-17 017.
 */

public class ImageTag implements Comparable<ImageTag> {
    public String url;
    public Integer position;

    @Override
    public int compareTo(@NonNull ImageTag o) {
        if (position != null && o != null && o.position != null) {
            return position.compareTo(o.position);
        } else {
            return 0;
        }
    }
}
