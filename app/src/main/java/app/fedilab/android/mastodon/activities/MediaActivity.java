package app.fedilab.android.mastodon.activities;
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

import static android.util.Patterns.WEB_URL;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.regex.Matcher;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityMediaPagerBinding;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MediaHelper;
import app.fedilab.android.mastodon.helper.TranslateHelper;
import app.fedilab.android.mastodon.interfaces.OnDownloadInterface;
import app.fedilab.android.mastodon.ui.fragment.media.FragmentMedia;
import app.fedilab.android.mastodon.ui.fragment.media.FragmentMediaProfile;
import es.dmoral.toasty.Toasty;


public class MediaActivity extends BaseTransparentActivity implements OnDownloadInterface {

    int flags;
    private ArrayList<Attachment> attachments;
    private int mediaPosition;
    private long downloadID;

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadID == id) {
                DownloadManager manager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                Uri uri = manager.getUriForDownloadedFile(downloadID);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                ContentResolver cR = context.getContentResolver();
                if (cR != null && uri != null) {
                    shareIntent.setType(cR.getType(uri));
                    try {
                        startActivity(shareIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
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
    private ActivityMediaPagerBinding binding;
    private FragmentMedia mCurrentFragment;
    private Status status;
    private boolean mediaFromProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActivityCompat.postponeEnterTransition(MediaActivity.this);
        binding = ActivityMediaPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        fullscreen = false;
        flags = getWindow().getDecorView().getSystemUiVisibility();
        Bundle args = getIntent().getExtras();
        if (args != null) {
            long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(MediaActivity.this).getBundle(bundleId, Helper.getCurrentAccount(MediaActivity.this), this::initializeAfterBundle);
        } else {
            initializeAfterBundle(null);
        }


    }

    private void initializeAfterBundle(Bundle bundle) {

        if (bundle != null) {
            mediaPosition = bundle.getInt(Helper.ARG_MEDIA_POSITION, 1);
            attachments = (ArrayList<Attachment>) bundle.getSerializable(Helper.ARG_MEDIA_ARRAY);
            mediaFromProfile = bundle.getBoolean(Helper.ARG_MEDIA_ARRAY_PROFILE, false);
            status = (Status) bundle.getSerializable(Helper.ARG_STATUS);
        }

        if (mediaFromProfile && FragmentMediaProfile.mediaAttachmentProfile != null) {
            attachments = new ArrayList<>();
            attachments.addAll(FragmentMediaProfile.mediaAttachmentProfile);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (attachments == null || attachments.size() == 0) {
            finish();
            return;
        }

        setTitle("");
        ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        binding.mediaViewpager.setAdapter(mPagerAdapter);
        binding.mediaViewpager.setSaveEnabled(false);
        binding.mediaViewpager.setCurrentItem(mediaPosition - 1);
        ContextCompat.registerReceiver(MediaActivity.this, onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        String description = attachments.get(mediaPosition - 1).description;
        handler = new Handler();
        if (attachments.get(mediaPosition - 1).status != null) {
            binding.originalMessage.setOnClickListener(v -> {
                Intent intentContext = new Intent(MediaActivity.this, ContextActivity.class);
                Bundle args = new Bundle();
                args.putSerializable(Helper.ARG_STATUS, attachments.get(mediaPosition - 1).status);
                new CachedBundle(MediaActivity.this).insertBundle(args, Helper.getCurrentAccount(MediaActivity.this), bundleId -> {
                    Bundle bundleCached = new Bundle();
                    bundleCached.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intentContext.putExtras(bundleCached);
                    intentContext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentContext);
                });
            });
        }

        binding.mediaDescription.setMovementMethod(LinkMovementMethod.getInstance());
        binding.mediaDescriptionTranslated.setMovementMethod(LinkMovementMethod.getInstance());

        if (description != null && description.trim().length() > 0 && description.trim().compareTo("null") != 0) {
            binding.mediaDescription.setText(description);
            binding.translate.setOnClickListener(v -> {
                String descriptionToTranslate = attachments.get(mediaPosition - 1).description;
                TranslateHelper.translate(MediaActivity.this, descriptionToTranslate, translated -> {
                    if (translated != null) {
                        attachments.get(mediaPosition - 1).translation = translated;
                        binding.mediaDescriptionTranslated.setText(translated);
                        binding.mediaDescriptionTranslated.setVisibility(View.VISIBLE);
                        binding.mediaDescription.setVisibility(View.GONE);
                        if (mCurrentFragment != null) {
                            mCurrentFragment.toggleController(false);
                        }
                    } else {
                        Toasty.error(MediaActivity.this, getString(R.string.toast_error_translate), Toast.LENGTH_LONG).show();
                    }
                });
            });
            if (attachments.get(mediaPosition - 1).translation != null) {
                binding.mediaDescription.setVisibility(View.GONE);
                if (mCurrentFragment != null) {
                    mCurrentFragment.toggleController(false);
                }
                binding.mediaDescriptionTranslated.setText(attachments.get(mediaPosition - 1).translation);
                binding.mediaDescriptionTranslated.setVisibility(View.VISIBLE);
            } else {
                binding.mediaDescription.setVisibility(View.VISIBLE);
                if (mCurrentFragment != null) {
                    mCurrentFragment.toggleController(true);
                }
                binding.mediaDescriptionTranslated.setVisibility(View.GONE);
            }
        }

        binding.mediaViewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                mediaPosition = position;
                String description = attachments.get(position).description;
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }
                handler = new Handler();
                if (description != null && description.trim().length() > 0 && description.trim().compareTo("null") != 0) {
                    binding.mediaDescription.setText(linkify(MediaActivity.this, description), TextView.BufferType.SPANNABLE);
                }
                binding.translate.setOnClickListener(v -> {
                    String descriptionToTranslate = attachments.get(position).description;
                    TranslateHelper.translate(MediaActivity.this, descriptionToTranslate, translated -> {
                        if (translated != null) {
                            attachments.get(position).translation = translated;
                            binding.mediaDescriptionTranslated.setText(translated);
                            binding.mediaDescriptionTranslated.setVisibility(View.VISIBLE);
                            binding.mediaDescription.setVisibility(View.GONE);
                            if (mCurrentFragment != null) {
                                mCurrentFragment.toggleController(true);
                            }
                        } else {
                            Toasty.error(MediaActivity.this, getString(R.string.toast_error_translate), Toast.LENGTH_LONG).show();
                        }
                    });
                });
                if (!fullscreen) {
                    if (attachments.get(position).translation != null) {
                        binding.mediaDescription.setVisibility(View.GONE);
                        if (mCurrentFragment != null) {
                            mCurrentFragment.toggleController(false);
                        }
                        binding.mediaDescriptionTranslated.setText(attachments.get(position).translation);
                        binding.mediaDescriptionTranslated.setVisibility(View.VISIBLE);
                    } else {
                        binding.mediaDescription.setVisibility(View.VISIBLE);
                        if (mCurrentFragment != null) {
                            mCurrentFragment.toggleController(true);
                        }
                        binding.mediaDescriptionTranslated.setVisibility(View.GONE);
                    }
                } else {
                    binding.mediaDescription.setVisibility(View.GONE);
                    if (mCurrentFragment != null) {
                        mCurrentFragment.toggleController(false);
                    }
                    binding.mediaDescriptionTranslated.setVisibility(View.GONE);
                }
            }
        });
        setFullscreen(true);
    }


    private Spannable linkify(Context context, String content) {
        if (content == null) {
            return new SpannableString("");
        }
        Matcher matcher = WEB_URL.matcher(content);
        Spannable contentSpan = new SpannableString(content);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean underlineLinks = sharedpreferences.getBoolean(context.getString(R.string.SET_UNDERLINE_CLICKABLE), false);


        while (matcher.find()) {
            int matchStart = matcher.start();
            int matchEnd = matcher.end();
            String url = content.substring(matchStart, matchEnd);
            if (matchStart >= 0 && matchEnd <= content.length() && matchEnd >= matchStart) {
                contentSpan.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View textView) {
                        Helper.openBrowser(context, url);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        if (!underlineLinks) {
                            ds.setUnderlineText(status != null && status.underlined);
                        }
                    }
                }, matchStart, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
        return contentSpan;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        try {
            return super.dispatchTouchEvent(event);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public void toogleFullScreen() {
        fullscreen = !fullscreen;
        setFullscreen(fullscreen);
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
            try {
                ActivityCompat.finishAfterTransition(MediaActivity.this);
            } catch (Exception ignored) {
            }
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            int position = binding.mediaViewpager.getCurrentItem();
            Attachment attachment = attachments.get(position);
            if (Build.VERSION.SDK_INT >= 23) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
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

    private void toggleScreenContain(boolean fullscreen) {
        if (!fullscreen) {
            String description = attachments.get(binding.mediaViewpager.getCurrentItem()).description;
            if (description != null && description.trim().length() > 0 && description.trim().compareTo("null") != 0) {
                binding.mediaDescription.setText(linkify(MediaActivity.this, description), TextView.BufferType.SPANNABLE);
                if (attachments.get(binding.mediaViewpager.getCurrentItem()).translation != null) {
                    binding.mediaDescription.setVisibility(View.GONE);
                    binding.mediaDescriptionTranslated.setText(attachments.get(binding.mediaViewpager.getCurrentItem()).translation);
                    binding.mediaDescriptionTranslated.setVisibility(View.VISIBLE);
                } else {
                    binding.mediaDescription.setVisibility(View.VISIBLE);
                    binding.mediaDescriptionTranslated.setVisibility(View.GONE);
                }
            } else {
                binding.translate.setVisibility(View.GONE);
                if (status != null) {
                    binding.originalMessage.setVisibility(View.VISIBLE);
                } else {
                    binding.originalMessage.setVisibility(View.INVISIBLE);
                }
                binding.mediaDescriptionTranslated.setVisibility(View.GONE);
                binding.mediaDescription.setVisibility(View.GONE);
            }
        } else {
            binding.originalMessage.setVisibility(View.INVISIBLE);
            binding.translate.setVisibility(View.GONE);
            binding.mediaDescriptionTranslated.setVisibility(View.GONE);
            binding.mediaDescription.setVisibility(View.GONE);
        }
    }


    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(onDownloadComplete);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
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

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(flags |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public boolean getFullScreen() {
        return this.fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        if (!fullscreen) {
            showSystemUI();
            binding.mediaDescription.setVisibility(View.VISIBLE);
            if (mCurrentFragment != null) {
                mCurrentFragment.toggleController(true);
            }
            binding.translate.setVisibility(View.VISIBLE);
            if (mediaFromProfile) {
                binding.originalMessage.setVisibility(View.VISIBLE);
            }
        } else {
            hideSystemUI();
            binding.mediaDescription.setVisibility(View.GONE);
            if (mCurrentFragment != null) {
                mCurrentFragment.toggleController(false);
            }
            binding.translate.setVisibility(View.GONE);
            binding.originalMessage.setVisibility(View.INVISIBLE);
        }
        toggleScreenContain(fullscreen);
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
