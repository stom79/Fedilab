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
import android.os.AsyncTask;

import app.fedilab.android.mastodon.watermark.androidwm.listener.DetectFinishListener;
import app.fedilab.android.mastodon.watermark.androidwm.utils.BitmapUtils;
import app.fedilab.android.mastodon.watermark.androidwm.utils.Constant;
import app.fedilab.android.mastodon.watermark.androidwm.utils.StringUtils;

/**
 * This is a task for watermark image detection.
 * In LSB mode, all the task will return a bitmap;
 *
 * @author huangyz0918 (huangyz0918@gmail.com)
 */
public class LSBDetectionTask extends AsyncTask<Bitmap, Void, DetectionReturnValue> {

    private final DetectFinishListener listener;

    public LSBDetectionTask(DetectFinishListener listener) {
        this.listener = listener;
    }

    @Override
    protected DetectionReturnValue doInBackground(Bitmap... bitmaps) {
        Bitmap markedBitmap = bitmaps[0];
        DetectionReturnValue resultValue = new DetectionReturnValue();

        if (markedBitmap == null) {
            listener.onFailure(Constant.ERROR_BITMAP_NULL);
            return null;
        }

        if (markedBitmap.getWidth() > Constant.MAX_IMAGE_SIZE || markedBitmap.getHeight() > Constant.MAX_IMAGE_SIZE) {
            listener.onFailure(Constant.WARNING_BIG_IMAGE);
            return null;
        }

        int[] pixels = BitmapUtils.getBitmapPixels(markedBitmap);
        int[] colorArray = BitmapUtils.pixel2ARGBArray(pixels);

        for (int i = 0; i < colorArray.length; i++) {
            colorArray[i] = colorArray[i] % 10;
        }

        StringUtils.replaceNinesJ(colorArray);
        String binaryString = StringUtils.intArrayToStringJ(colorArray);
        String resultString;

        if (binaryString.contains(Constant.LSB_TEXT_PREFIX_FLAG) && binaryString.contains(Constant.LSB_TEXT_SUFFIX_FLAG)) {
            resultString = StringUtils.getBetweenStrings(binaryString, true, listener);
            resultString = StringUtils.binaryToString(resultString);
            resultValue.setWatermarkString(resultString);
        } else if (binaryString.contains(Constant.LSB_IMG_PREFIX_FLAG) && binaryString.contains(Constant.LSB_IMG_SUFFIX_FLAG)) {
            binaryString = StringUtils.getBetweenStrings(binaryString, false, listener);
            resultString = StringUtils.binaryToString(binaryString);
            resultValue.setWatermarkBitmap(BitmapUtils.stringToBitmap(resultString));
        }

        return resultValue;
    }

    @Override
    protected void onPostExecute(DetectionReturnValue detectionReturnValue) {
        if (detectionReturnValue == null) {
            listener.onFailure(Constant.ERROR_DETECT_FAILED);
            return;
        }

        if (detectionReturnValue.getWatermarkString() != null &&
                !"".equals(detectionReturnValue.getWatermarkString()) ||
                detectionReturnValue.getWatermarkBitmap() != null) {
            listener.onSuccess(detectionReturnValue);
        } else {
            listener.onFailure(Constant.ERROR_DETECT_FAILED);
        }
        super.onPostExecute(detectionReturnValue);
    }

}
