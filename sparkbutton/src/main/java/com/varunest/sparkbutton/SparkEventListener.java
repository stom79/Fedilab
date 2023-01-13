package com.varunest.sparkbutton;

import androidx.annotation.NonNull;

/**
 * @author varun on 07/07/16.
 */
public interface SparkEventListener {
    boolean onEvent(@NonNull SparkButton button, boolean buttonState);
}