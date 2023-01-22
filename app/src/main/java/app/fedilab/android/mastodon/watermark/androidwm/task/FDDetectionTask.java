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
import app.fedilab.android.mastodon.watermark.androidwm.utils.FastDctFft;
import app.fedilab.android.mastodon.watermark.androidwm.utils.StringUtils;

/**
 * This is a task for watermark image detection.
 * In FD mode, all the task will return a bitmap;
 *
 * @author huangyz0918 (huangyz0918@gmail.com)
 */
@SuppressWarnings("PMD")
public class FDDetectionTask extends AsyncTask<Bitmap, Void, DetectionReturnValue> {

    private final DetectFinishListener listener;

    public FDDetectionTask(DetectFinishListener listener) {
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

        // divide and conquer
        if (pixels.length < Constant.CHUNK_SIZE) {
            int[] watermarkRGB = BitmapUtils.pixel2ARGBArray(pixels);
            double[] watermarkArray = StringUtils.copyFromIntArray(watermarkRGB);
            FastDctFft.transform(watermarkArray);

            //TODO: do some operations with colorTempArray.


        } else {
            int numOfChunks = (int) Math.ceil((double) pixels.length / Constant.CHUNK_SIZE);
            for (int i = 0; i < numOfChunks; i++) {
                int start = i * Constant.CHUNK_SIZE;
                int length = Math.min(pixels.length - start, Constant.CHUNK_SIZE);
                int[] temp = new int[length];
                System.arraycopy(pixels, start, temp, 0, length);
                double[] colorTempArray = StringUtils.copyFromIntArray(BitmapUtils.pixel2ARGBArray(temp));
                FastDctFft.transform(colorTempArray);

                //TODO: do some operations with colorTempArray.

            }
        }

/*        TODO: new detection operations will replace this block.
        String resultString;

        if (binaryString.contains(LSB_TEXT_PREFIX_FLAG) && binaryString.contains(LSB_TEXT_SUFFIX_FLAG)) {
            resultString = getBetweenStrings(binaryString, true, listener);
            resultString = binaryToString(resultString);
            resultValue.setWatermarkString(resultString);
        } else if (binaryString.contains(LSB_IMG_PREFIX_FLAG) && binaryString.contains(LSB_IMG_SUFFIX_FLAG)) {
            binaryString = getBetweenStrings(binaryString, false, listener);
            resultString = binaryToString(binaryString);
            resultValue.setWatermarkBitmap(BitmapUtils.stringToBitmap(resultString));
        }*/

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