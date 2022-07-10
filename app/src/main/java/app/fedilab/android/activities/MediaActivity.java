package app.fedilab.android.activities;
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

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.databinding.ActivityMediaPagerBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MediaHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.interfaces.OnDownloadInterface;
import app.fedilab.android.ui.fragment.media.FragmentMedia;
import es.dmoral.toasty.Toasty;


public class MediaActivity extends BaseActivity implements OnDownloadInterface {

    int flags;
    private ArrayList<Attachment> attachments;
    private int mediaPosition;
    private long downloadID;

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadID == id) {
                DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                assert manager != null;
                Uri uri = manager.getUriForDownloadedFile(downloadID);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                ContentResolver cR = context.getContentResolver();
                if (cR != null && uri != null) {
                    shareIntent.setType(cR.getType(uri));
                    try {
                        startActivity(shareIntent);
                    } catch (Exception ignored) {
                    }
                } else {
                    Toasty.error(context, context.getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                }
            } else {
                Toasty.success(context, context.getString(R.string.save_over), Toasty.LENGTH_LONG).show();
            }
        }
    };
    private boolean fullscreen;
    private Handler handler;
    private int minTouch, maxTouch;
    private float startX;
    private float startY;
    private ActivityMediaPagerBinding binding;
    private FragmentMedia mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        ThemeHelper.applyThemeBar(this);
        super.onCreate(savedInstanceState);
        ActivityCompat.postponeEnterTransition(MediaActivity.this);
        binding = ActivityMediaPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        fullscreen = false;
        flags = getWindow().getDecorView().getSystemUiVisibility();
        Bundle b = getIntent().getExtras();
        if (b != null) {
            mediaPosition = b.getInt(Helper.ARG_MEDIA_POSITION, 1);
            attachments = (ArrayList<Attachment>) b.getSerializable(Helper.ARG_MEDIA_ARRAY);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }

        if (attachments == null || attachments.size() == 0)
            finish();

        setTitle("");

        ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        binding.mediaViewpager.setAdapter(mPagerAdapter);
        binding.mediaViewpager.setSaveEnabled(false);
        binding.mediaViewpager.setCurrentItem(mediaPosition - 1);
        binding.haulerView.setOnDragDismissedListener(dragDirection -> ActivityCompat.finishAfterTransition(MediaActivity.this));
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        String description = attachments.get(mediaPosition - 1).description;
        handler = new Handler();
        if (description != null && description.trim().length() > 0 && description.trim().compareTo("null") != 0) {
            binding.mediaDescription.setText(description);

        }
        binding.mediaViewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                String description = attachments.get(position).description;
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }
                handler = new Handler();
                if (description != null && description.trim().length() > 0 && description.trim().compareTo("null") != 0) {
                    binding.mediaDescription.setText(description);
                }
            }
        });


        setFullscreen(true);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenHeight = size.y;
        minTouch = (int) (screenHeight * 0.1);
        maxTouch = (int) (screenHeight * 0.9);

    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_media, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            ActivityCompat.finishAfterTransition(MediaActivity.this);
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            int position = binding.mediaViewpager.getCurrentItem();
            Attachment attachment = attachments.get(position);
            if (Build.VERSION.SDK_INT >= 23) {
                if (ContextCompat.checkSelfPermission(MediaActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MediaActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MediaActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Helper.EXTERNAL_STORAGE_REQUEST_CODE_MEDIA_SAVE);
                } else {
                    if (attachment.type.compareTo("image") == 0) {
                        MediaHelper.manageMove(MediaActivity.this, attachment.url, false);
                    } else {
                        MediaHelper.manageDownloadsNoPopup(MediaActivity.this, attachment.url);
                        downloadID = -1;
                    }
                }
            } else {
                if (attachment.type.compareToIgnoreCase("image") == 0) {
                    MediaHelper.manageMove(MediaActivity.this, attachment.url, false);
                } else {
                    MediaHelper.manageDownloadsNoPopup(MediaActivity.this, attachment.url);
                    downloadID = -1;
                }
            }
        } else if (item.getItemId() == R.id.action_share) {
            int position = binding.mediaViewpager.getCurrentItem();
            Attachment attachment = attachments.get(position);
            if (attachment.type.compareTo("image") == 0) {
                MediaHelper.manageMove(MediaActivity.this, attachment.url, true);
            } else if (attachment.type.equalsIgnoreCase("video") || attachment.type.equalsIgnoreCase("audio") || attachment.type.equalsIgnoreCase("gifv")) {
                downloadID = MediaHelper.manageDownloadsNoPopup(MediaActivity.this, attachment.url);
            } else {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (ContextCompat.checkSelfPermission(MediaActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MediaActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MediaActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Helper.EXTERNAL_STORAGE_REQUEST_CODE_MEDIA_SHARE);
                    } else {
                        downloadID = MediaHelper.manageDownloadsNoPopup(MediaActivity.this, attachment.url);
                    }
                } else {
                    downloadID = MediaHelper.manageDownloadsNoPopup(MediaActivity.this, attachment.url);
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Helper.EXTERNAL_STORAGE_REQUEST_CODE_MEDIA_SAVE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                int position = binding.mediaViewpager.getCurrentItem();
                Attachment attachment = attachments.get(position);
                if (attachment.type.compareToIgnoreCase("image") == 0) {
                    MediaHelper.manageMove(MediaActivity.this, attachment.url, false);
                } else {
                    MediaHelper.manageDownloadsNoPopup(MediaActivity.this, attachment.url);
                    downloadID = -1;
                }
            } else { /*Todo: Toast "Storage Permission Required" */ }
        } else if (requestCode == Helper.EXTERNAL_STORAGE_REQUEST_CODE_MEDIA_SHARE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                int position = binding.mediaViewpager.getCurrentItem();
                Attachment attachment = attachments.get(position);
                downloadID = MediaHelper.manageDownloadsNoPopup(MediaActivity.this, attachment.url);
            } else { /*Todo: Toast "Storage Permission Required" */ }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float endX = event.getX();
                float endY = event.getY();
                if (endY > minTouch && endY < maxTouch && isAClick(startX, endX, startY, endY)) {
                    setFullscreen(!fullscreen);
                    if (!fullscreen) {
                        String description = attachments.get(binding.mediaViewpager.getCurrentItem()).description;
                        if (handler != null) {
                            handler.removeCallbacksAndMessages(null);
                        }
                        handler = new Handler();
                        if (description != null && description.trim().length() > 0 && description.trim().compareTo("null") != 0) {
                            binding.mediaDescription.setText(description);
                        }
                    }
                }
                break;
        }
        try {
            return super.dispatchTouchEvent(event);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;

    }


    private boolean isAClick(float startX, float endX, float startY, float endY) {
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);
        int CLICK_ACTION_THRESHOLD = 200;
        return !(differenceX > CLICK_ACTION_THRESHOLD/* =5 */ || differenceY > CLICK_ACTION_THRESHOLD);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(onDownloadComplete);
        super.onDestroy();
    }

    @Override
    public void onDownloaded(String saveFilePath, String downloadUrl, Error error) {

    }

    @Override
    public void onUpdateProgress(int progress) {

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }


    public boolean getFullScreen() {
        return this.fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        if (!fullscreen) {
            showSystemUI();
            binding.mediaDescription.setVisibility(View.VISIBLE);
        } else {
            binding.mediaDescription.setVisibility(View.GONE);
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public FragmentMedia getCurrentFragment() {
        return mCurrentFragment;
    }

    /**
     * Media Pager
     */
    @SuppressWarnings("deprecation")
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            FragmentMedia mediaSliderFragment = new FragmentMedia();
            bundle.putInt(Helper.ARG_MEDIA_POSITION, position);
            bundle.putSerializable(Helper.ARG_MEDIA_ATTACHMENT, attachments.get(position));
            mediaSliderFragment.setArguments(bundle);
            return mediaSliderFragment;
        }

        @Override
        public void setPrimaryItem(@NotNull ViewGroup container, int position, @NotNull Object object) {
            if (getCurrentFragment() != object) {
                mCurrentFragment = ((FragmentMedia) object);
            }
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public int getCount() {
            return attachments.size();
        }
    }
}
