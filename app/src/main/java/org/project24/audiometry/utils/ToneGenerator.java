package org.project24.audiometry.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class ToneGenerator {
    /**
     * Generates the tone based on the increment and volume, used in inner loop
     * @param increment - the amount to increment by
     * @param volume - the volume to generate
     */
    public float[] genTone(float increment, int volume, int numSamples){

        float angle = 0;
        float[] generatedSnd = new float[numSamples];
        for (int i = 0; i < numSamples; i++){
            generatedSnd[i] = (float) (Math.sin(angle)*volume/32768);
            angle += increment;
        }
        return generatedSnd;
    }

    /**
     * Writes the parameter byte array to an AudioTrack and plays the array
     * @param generatedSnd- input PCM float array
     */
    public AudioTrack playSound(float[] generatedSnd, int ear, int sampleRate) {
//        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT, generatedSnd.length, AudioTrack.MODE_STATIC);
//
//        audioTrack.write(generatedSnd,0,generatedSnd.length,AudioTrack.WRITE_BLOCKING);
//        if (ear == 0) {
//            audioTrack.setStereoVolume(0, AudioTrack.getMaxVolume());
//        } else {
//            audioTrack.setStereoVolume(AudioTrack.getMaxVolume(),  0);
//        }
//        audioTrack.play();
//        return audioTrack;

        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO; // Use stereo output
        int bufferSize = generatedSnd.length * Float.BYTES; // Correct buffer size

        AudioTrack audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build(),
                bufferSize,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
        );

        // Convert mono sound to stereo by duplicating samples with volume adjustments
        float[] stereoSnd = new float[generatedSnd.length * 2];
        for (int i = 0, j = 0; i < generatedSnd.length; i++, j += 2) {
            if (ear == 0) { // Left ear (right channel active)
                stereoSnd[j] = 0;                  // Left channel silent
                stereoSnd[j + 1] = generatedSnd[i]; // Right channel plays sound
            } else { // Right ear (left channel active)
                stereoSnd[j] = generatedSnd[i]; // Left channel plays sound
                stereoSnd[j + 1] = 0;           // Right channel silent
            }
        }

        audioTrack.write(stereoSnd, 0, stereoSnd.length, AudioTrack.WRITE_BLOCKING);
        audioTrack.play();

        return audioTrack;

    }

    /**
     * Generates the tone based on the increment and volume, used in inner loop
     * @param increment - the amount to increment by
     * @param volume - the volume to generate
     */
    public float[] genStereoTone(float increment, int volume, int numSamples, int ear){

        float angle = 0;
        float[] generatedSnd = new float[2*numSamples];
        for (int i = 0; i < numSamples; i=i+2){
            if (ear == 0) {
                generatedSnd[i] = (float) (Math.sin(angle)*volume/32768);
                generatedSnd[i+1] = 0;
            } else {
                generatedSnd[i] = 0;
                generatedSnd[i+1] = (float) (Math.sin(angle)*volume/32768);
            }
            angle += increment;
        }
        return generatedSnd;
    }

    /**
     * Writes the parameter byte array to an AudioTrack and plays the array
     * @param generatedSnd- input PCM float array
     * PROBLEM: On some devices sound from one stereo channel (as created by genStereoSound) can be heard on the other channel
     */
    public AudioTrack playStereoSound(float[] generatedSnd, int sampleRate) {
//        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT, generatedSnd.length, AudioTrack.MODE_STATIC);
//        audioTrack.write(generatedSnd,0,generatedSnd.length,AudioTrack.WRITE_BLOCKING);
//        audioTrack.setVolume(AudioTrack.getMaxVolume());
//        audioTrack.play();
//        return audioTrack;

        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(generatedSnd.length * 4) // float is 4 bytes
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();

        audioTrack.write(generatedSnd, 0, generatedSnd.length, AudioTrack.WRITE_BLOCKING);
        audioTrack.setVolume(AudioTrack.getMaxVolume());
        audioTrack.play();
        return audioTrack;
    }
}
