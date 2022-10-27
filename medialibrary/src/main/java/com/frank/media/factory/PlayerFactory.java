package com.frank.media.factory;

import androidx.annotation.IntDef;

import com.frank.media.listener.IMediaPlayer;
import com.frank.media.player.FFmpegPlayer;
import com.frank.media.player.SystemMediaPlayer;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author xufulong
 * @date 2022/10/26 5:43 下午
 * @desc
 */
public class PlayerFactory {

    public static final int PLAYER_TYPE_FFMPEG = 0x01;
    public static final int PLAYER_TYPE_SYSTEM = 0x02;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PLAYER_TYPE_FFMPEG, PLAYER_TYPE_SYSTEM})
    public @interface PlayerType{}

    public static IMediaPlayer createPlayer(@PlayerType int playerType) {
        switch (playerType) {
            case PLAYER_TYPE_FFMPEG:
                return new FFmpegPlayer();
            case PLAYER_TYPE_SYSTEM:
                return new SystemMediaPlayer();
            default:
                return null;
        }
    }

}
