/*
 *    Copyright 2018 Yizheng Huang
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package app.fedilab.android.mastodon.watermark.androidwm.task;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;

import app.fedilab.android.mastodon.watermark.androidwm.bean.AsyncTaskParams;
import app.fedilab.android.mastodon.watermark.androidwm.bean.WatermarkText;
import app.fedilab.android.mastodon.watermark.androidwm.listener.BuildFinishListener;
import app.fedilab.android.mastodon.watermark.androidwm.utils.BitmapUtils;
import app.fedilab.android.mastodon.watermark.androidwm.utils.Constant;
import app.fedilab.android.mastodon.watermark.androidwm.utils.StringUtils;

/**
 * This is a background task for adding the specific invisible text
 * into the background image. We don't need to read every pixel's
 * RGB value, we just read the length values that can put our encrypted
 * text in.
 *
 * @author huangyz0918 (huangyz0918@gmail.com)
 */
public class LSBWatermarkTask extends AsyncTask<AsyncTaskParams, Void, Bitmap> {

    private final BuildFinishListener<Bitmap> listener;

    public LSBWatermarkTask(BuildFinishListener<Bitmap> callback) {
        this.listener = callback;
    }

    @Override
    protected Bitmap doInBackground(AsyncTaskParams... params) {
        Bitmap backgroundBitmap = params[0].getBackgroundImg();
        WatermarkText watermarkText = params[0].getWatermarkText();
        Bitmap watermarkBitmap = params[0].getWatermarkImg();
        String watermarkString;

        if (backgroundBitmap == null) {
            listener.onFailure(Constant.ERROR_NO_BACKGROUND);
            return null;
        }

        // convert the watermark bitmap into a String.
        if (watermarkBitmap != null) {
            watermarkString = BitmapUtils.bitmapToString(watermarkBitmap);
        } else {
            watermarkString = watermarkText.getText();
        }

        if (watermarkString == null) {
            listener.onFailure(Constant.ERROR_NO_WATERMARKS);
            return null;
        }

        Bitmap outputBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(), backgroundBitmap.getHeight(),
                backgroundBitmap.getConfig());

        int[] backgroundPixels = BitmapUtils.getBitmapPixels(backgroundBitmap);
        int[] backgroundColorArray = BitmapUtils.pixel2ARGBArray(backgroundPixels);

        // convert the Sting into a binary string, and, replace the single digit number.
        // using the rebuilt pixels to create a new watermarked image.
        String watermarkBinary = StringUtils.stringToBinary(watermarkString);

        if (watermarkBitmap != null) {
            watermarkBinary = Constant.LSB_IMG_PREFIX_FLAG + watermarkBinary + Constant.LSB_IMG_SUFFIX_FLAG;
        } else {
            watermarkBinary = Constant.LSB_TEXT_PREFIX_FLAG + watermarkBinary + Constant.LSB_TEXT_SUFFIX_FLAG;
        }

        int[] watermarkColorArray = StringUtils.stringToIntArray(watermarkBinary);
        if (watermarkColorArray.length > backgroundColorArray.length) {
            listener.onFailure(Constant.ERROR_PIXELS_NOT_ENOUGH);
        } else {
            int chunkSize = watermarkColorArray.length;
            int numOfChunks = (int) Math.ceil((double) backgroundColorArray.length / chunkSize);
            for (int i = 0; i < numOfChunks - 1; i++) {
                int start = i * chunkSize;
                for (int j = 0; j < chunkSize; j++) {
                    backgroundColorArray[start + j] = StringUtils.replaceSingleDigit(backgroundColorArray[start + j]
                            , watermarkColorArray[j]);
                }
            }

            for (int i = 0; i < backgroundPixels.length; i++) {
                int color = Color.argb(
                        backgroundColorArray[4 * i],
                        backgroundColorArray[4 * i + 1],
                        backgroundColorArray[4 * i + 2],
                        backgroundColorArray[4 * i + 3]
                );
                backgroundPixels[i] = color;
            }

            outputBitmap.setPixels(backgroundPixels, 0, backgroundBitmap.getWidth(), 0, 0,
                    backgroundBitmap.getWidth(), backgroundBitmap.getHeight());

            return outputBitmap;

        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap resultBitmap) {
        if (listener != null) {
            if (resultBitmap != null) {
                listener.onSuccess(resultBitmap);
            } else {
                listener.onFailure(Constant.ERROR_CREATE_FAILED);
            }
        }
        super.onPostExecute(resultBitmap);
    }

}
