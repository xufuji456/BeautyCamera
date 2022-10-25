package com.frank.media.mediainfo;

import java.util.Locale;

/**
 * @author xufulong
 * @date 2022/10/25 10:13 上午
 * @desc
 */
public class MediaTrack {

    public int trackId;

    public String language;

    public void setLanguage(String language) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Locale locale = Locale.forLanguageTag(language);
            this.language = locale.getDisplayName();
        } else {
            this.language = language;
        }
    }

}
