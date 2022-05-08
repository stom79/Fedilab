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
package app.fedilab.android.watermark.androidwm.listener;


import app.fedilab.android.watermark.androidwm.task.DetectionReturnValue;

/**
 * This interface is for listening if the task of
 * detecting invisible watermark is finished.
 *
 * @author huangyz0918 (huangyz0918@gmail.com)
 */
public interface DetectFinishListener {

    void onSuccess(DetectionReturnValue returnValue);

    void onFailure(String message);
}
