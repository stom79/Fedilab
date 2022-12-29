package app.fedilab.android.helper;
/* Copyright 2022 Thomas Schneider
 *
 * This file is a part of Fedilab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Fedilab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Fedilab; if not,
 * see <http://www.gnu.org/licenses>. */

import static android.content.Context.DOWNLOAD_SERVICE;
import static app.fedilab.android.BaseMainActivity.currentAccount;
import static app.fedilab.android.helper.Helper.notify_user;
import static app.fedilab.android.helper.LogoHelper.getMainLogo;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.piasy.rxandroidaudio.AudioRecorder;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.activities.ComposeActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.databinding.DatetimePickerBinding;
import app.fedilab.android.databinding.PopupRecordBinding;
import es.dmoral.toasty.Toasty;

public class MediaHelper {


    /**
     * Manage downloads with URLs, does not concern images, they are moved with Glide cache.
     *
     * @param context Context
     * @param url     String download url
     */
    public static long manageDownloadsNoPopup(final Context context, final String url) {

        final DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(Uri.parse(url.trim()));
        } catch (Exception e) {
            Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return -1;
        }
        try {
            String mime = getMimeType(url);

            final String fileName = URLUtil.guessFileName(url, null, null);
            request.allowScanningByMediaScanner();
            String myDir;
            if (mime.toLowerCase().startsWith("video")) {
                myDir = Environment.DIRECTORY_MOVIES + "/" + context.getString(R.string.app_name);
            } else if (mime.toLowerCase().startsWith("audio")) {
                myDir = Environment.DIRECTORY_MUSIC + "/" + context.getString(R.string.app_name);
            } else {
                myDir = Environment.DIRECTORY_DOWNLOADS;
            }

            if (!new File(myDir).exists()) {
                boolean created = new File(myDir).mkdir();
                if (!created) {
                    Toasty.error(context, context.getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                    return -1;
                }
            }
            if (mime.toLowerCase().startsWith("video")) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, context.getString(R.string.app_name) + "/" + fileName);
            } else if (mime.toLowerCase().startsWith("audio")) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, context.getString(R.string.app_name) + "/" + fileName);
            } else {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            }

            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            return dm.enqueue(request);
        } catch (Exception e) {
            Toasty.error(context, context.getString(R.string.error_destination_path), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Download from Glid cache
     *
     * @param context Context
     * @param url     String
     */
    public static void manageMove(Context context, String url, boolean share) {
        Glide.with(context)
                .asFile()
                .load(url)
                .into(new CustomTarget<File>() {
                    @Override
                    public void onResourceReady(@NotNull File file, Transition<? super File> transition) {
                        final String fileName = URLUtil.guessFileName(url, null, null);


                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                        File targeted_folder = new File(path, context.getString(R.string.app_name));
                        if (!targeted_folder.exists()) {
                            boolean created = targeted_folder.mkdir();
                            if (!created) {
                                Toasty.error(context, context.getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        FileInputStream fis = null;
                        FileOutputStream fos = null;
                        FileChannel in = null;
                        FileChannel out = null;
                        try {
                            File backupFile = new File(targeted_folder.getAbsolutePath() + "/" + fileName);
                            //noinspection ResultOfMethodCallIgnored
                            backupFile.createNewFile();
                            fis = new FileInputStream(file);
                            fos = new FileOutputStream(backupFile);
                            in = fis.getChannel();
                            out = fos.getChannel();
                            long size = in.size();
                            in.transferTo(0, size, out);
                            String mime = getMimeType(url);
                            final Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            Uri uri = Uri.fromFile(backupFile);
                            intent.setDataAndType(uri, mime);
                            MediaScannerConnection.scanFile(context, new String[]{backupFile.getAbsolutePath()}, null, null);
                            if (!share) {
                                notify_user(context, currentAccount, intent, BitmapFactory.decodeResource(context.getResources(),
                                        getMainLogo(context)), Helper.NotifType.STORE, context.getString(R.string.save_over), context.getString(R.string.download_from, fileName));
                                Toasty.success(context, context.getString(R.string.save_over), Toasty.LENGTH_LONG).show();
                            } else {
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                shareIntent.setType(mime);
                                try {
                                    context.startActivity(shareIntent);
                                } catch (Exception ignored) {
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (fis != null)
                                    fis.close();
                            } catch (Throwable ignore) {
                            }
                            try {
                                if (fos != null)
                                    fos.close();
                            } catch (Throwable ignore) {
                            }
                            try {
                                if (in != null && in.isOpen())
                                    in.close();
                            } catch (Throwable ignore) {
                            }

                            try {
                                if (out != null && out.isOpen())
                                    out.close();
                            } catch (Throwable ignore) {
                            }
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }


    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }


    public static Uri dispatchTakePictureIntent(Activity activity) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoFileUri = null;
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile(activity);
            } catch (IOException ignored) {
                Toasty.error(activity, activity.getString(R.string.toot_select_image_error), Toast.LENGTH_LONG).show();
            }
            // Continue only if the File was successfully created

            if (photoFile != null) {
                photoFileUri = FileProvider.getUriForFile(activity,
                        BuildConfig.APPLICATION_ID + ".fileProvider",
                        photoFile);
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
            activity.startActivityForResult(takePictureIntent, ComposeActivity.TAKE_PHOTO);
        }
        return photoFileUri;
    }

    private static File createImageFile(Context context) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Save a file: path for use with ACTION_VIEW intents
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    /**
     * Record media
     *
     * @param activity Activity
     * @param listener ActionRecord
     */
    public static void recordAudio(Activity activity, ActionRecord listener) {
        String filePath = activity.getCacheDir() + "/fedilab_recorded_audio.m4a";
        AudioRecorder mAudioRecorder = AudioRecorder.getInstance();
        File mAudioFile = new File(filePath);
        PopupRecordBinding binding = PopupRecordBinding.inflate(activity.getLayoutInflater());
        AlertDialog.Builder audioPopup = new AlertDialog.Builder(activity, Helper.dialogStyle());
        audioPopup.setView(binding.getRoot());
        AlertDialog alert = audioPopup.create();
        alert.show();
        Timer timer = new Timer();
        AtomicInteger count = new AtomicInteger();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(() -> {
                    int value = count.getAndIncrement();
                    String minutes = "00";
                    String seconds;
                    if (value > 60) {
                        minutes = String.valueOf(value / 60);
                        seconds = String.valueOf(value % 60);
                    } else {
                        seconds = String.valueOf(value);
                    }
                    if (minutes.length() == 1) {
                        minutes = "0" + minutes;
                    }
                    if (seconds.length() == 1) {
                        seconds = "0" + seconds;
                    }
                    binding.counter.setText(String.format(Locale.getDefault(), "%s:%s", minutes, seconds));
                });
            }
        }, 1000, 1000);
        binding.record.setOnClickListener(v -> {
            mAudioRecorder.stopRecord();
            timer.cancel();
            alert.dismiss();
            listener.onRecorded(filePath);
        });
        mAudioRecorder.prepareRecord(MediaRecorder.AudioSource.MIC,
                MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.AudioEncoder.AAC,
                mAudioFile);
        mAudioRecorder.startRecord();
    }

    /**
     * Schedule a message
     *
     * @param activity - Activity
     * @param listener - OnSchedule
     */
    public static void scheduleMessage(Activity activity, OnSchedule listener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, Helper.dialogStyle());
        DatetimePickerBinding binding = DatetimePickerBinding.inflate(activity.getLayoutInflater());

        dialogBuilder.setView(binding.getRoot());
        final AlertDialog alertDialog = dialogBuilder.create();

        if (DateFormat.is24HourFormat(activity)) {
            binding.timePicker.setIs24HourView(true);
        }
        //Buttons management
        binding.dateTimeCancel.setOnClickListener(v -> alertDialog.dismiss());
        binding.dateTimeNext.setOnClickListener(v -> {
            binding.datePicker.setVisibility(View.GONE);
            binding.timePicker.setVisibility(View.VISIBLE);
            binding.dateTimePrevious.setVisibility(View.VISIBLE);
            binding.dateTimeNext.setVisibility(View.GONE);
            binding.dateTimeSet.setVisibility(View.VISIBLE);
        });
        binding.dateTimePrevious.setOnClickListener(v -> {
            binding.datePicker.setVisibility(View.VISIBLE);
            binding.timePicker.setVisibility(View.GONE);
            binding.dateTimePrevious.setVisibility(View.GONE);
            binding.dateTimeNext.setVisibility(View.VISIBLE);
            binding.dateTimeSet.setVisibility(View.GONE);
        });
        binding.dateTimeSet.setOnClickListener(v -> {
            int hour, minute;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = binding.timePicker.getHour();
                minute = binding.timePicker.getMinute();
            } else {
                hour = binding.timePicker.getCurrentHour();
                minute = binding.timePicker.getCurrentMinute();
            }
            Calendar calendar = new GregorianCalendar(binding.datePicker.getYear(),
                    binding.datePicker.getMonth(),
                    binding.datePicker.getDayOfMonth(),
                    hour,
                    minute);
            final long[] time = {calendar.getTimeInMillis()};

            if ((time[0] - new Date().getTime()) < 60000) {
                Toasty.warning(activity, activity.getString(R.string.toot_scheduled_date), Toast.LENGTH_LONG).show();
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat(Helper.SCHEDULE_DATE_FORMAT, Locale.getDefault());
                String date = sdf.format(calendar.getTime());
                listener.scheduledAt(date);
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    /**
     * Returns the max height of a list of media
     *
     * @param attachmentList - List<Attachment>
     * @return int - The max height
     */
    @SuppressWarnings("unused")
    public static int returnMaxHeightForPreviews(Context context, List<Attachment> attachmentList) {
        int maxHeight = RelativeLayout.LayoutParams.WRAP_CONTENT;
        if (attachmentList != null && attachmentList.size() > 0) {
            for (Attachment attachment : attachmentList) {
                if (attachment.meta != null && attachment.meta.small != null && attachment.meta.small.height > maxHeight) {
                    maxHeight = (int) Helper.convertDpToPixel(attachment.meta.small.height, context);
                }
            }
        }
        return maxHeight;
    }

    //Listener for recording media
    public interface ActionRecord {
        void onRecorded(String file);
    }

    public interface OnSchedule {
        void scheduledAt(String scheduledDate);
    }

    public static void ResizedImageRequestBody(Context context, Uri uri, File targetedFile) {
        InputStream decodeBitmapInputStream = null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            int orientation = getImageOrientation(uri, context.getContentResolver());
            int scaledImageSize = 1024;
            final int maxRetry = 3;
            int retry = 0;
            do {
                FileOutputStream outputStream = new FileOutputStream(targetedFile);
                decodeBitmapInputStream = context.getContentResolver().openInputStream(uri);
                options.inSampleSize = calculateInSampleSize(options, scaledImageSize, scaledImageSize);
                options.inJustDecodeBounds = false;
                Bitmap scaledBitmap = BitmapFactory.decodeStream(decodeBitmapInputStream, null, options);
                Bitmap reorientedBitmap = reorientBitmap(scaledBitmap, orientation);
                if (reorientedBitmap == null) {
                    scaledBitmap.recycle();
                    return;
                }
                Bitmap.CompressFormat format;
                if (!reorientedBitmap.hasAlpha()) {
                    format = Bitmap.CompressFormat.JPEG;
                } else {
                    format = Bitmap.CompressFormat.PNG;
                }
                reorientedBitmap.compress(format, 100, outputStream);
                reorientedBitmap.recycle();
                scaledImageSize /= 2;
                retry++;
            } while (targetedFile.length() > getMaxSize(targetedFile.length()) && retry < maxRetry);
        } catch (Exception e) {
            e.printStackTrace();
            if (decodeBitmapInputStream != null) {
                try {
                    decodeBitmapInputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int rqWidth, int rqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > rqHeight || width > rqWidth) {

            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > rqHeight && (halfWidth / inSampleSize) > rqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static int getImageOrientation(Uri uri, ContentResolver contentResolver) {
        InputStream inputStream;
        try {
            inputStream = contentResolver.openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
        if (inputStream == null) {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                ExifInterface exifInterface = new ExifInterface(inputStream);
                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                inputStream.close();
                return orientation;
            } catch (IOException e) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return ExifInterface.ORIENTATION_UNDEFINED;
                }
                e.printStackTrace();
                return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } else {
            try {
                ExifInterface exifInterface = new ExifInterface(uri.getPath());
                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                inputStream.close();
                return orientation;
            } catch (IOException e) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return ExifInterface.ORIENTATION_UNDEFINED;
                }
                e.printStackTrace();
                return ExifInterface.ORIENTATION_UNDEFINED;
            }
        }
    }


    private static long getMaxSize(long maxSize) {
        if (MainActivity.instanceInfo != null && MainActivity.instanceInfo.configuration != null && MainActivity.instanceInfo.configuration.media_attachments != null) {
            maxSize = MainActivity.instanceInfo.configuration.media_attachments.image_size_limit;
        }
        return maxSize;
    }

    public static Bitmap reorientBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180.0f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90.0f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90.0f);
                break;
            default:
                return bitmap;
        }
        if (bitmap == null) {
            return null;
        }
        try {
            Bitmap result = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            if (!bitmap.sameAs(result)) {
                bitmap.recycle();
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
