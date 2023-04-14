package com.frank.videoedit.transform.listener;


public interface EncoderBitrateProvider {

  int getBitrate(String encoderName, int width, int height, float frameRate);
}
