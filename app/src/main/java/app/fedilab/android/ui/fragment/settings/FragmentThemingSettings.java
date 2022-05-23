package app.fedilab.android.ui.fragment.settings;
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

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.jaredrummler.cyanea.Cyanea;
import com.jaredrummler.cyanea.prefs.CyaneaSettingsActivity;
import com.jaredrummler.cyanea.prefs.CyaneaTheme;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.ComposeActivity;
import app.fedilab.android.databinding.PopupStatusThemeBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import es.dmoral.toasty.Toasty;

public class FragmentThemingSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {


    private final int PICK_IMPORT_THEME = 5557;
    private List<LinkedHashMap<String, String>> listOfThemes;
    private SharedPreferences appPref;
    private SharedPreferences cyneaPref;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        cyneaPref = requireActivity().getSharedPreferences("com.jaredrummler.cyanea", Context.MODE_PRIVATE);
        appPref = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        createPref();
        listOfThemes = ThemeHelper.getContributorsTheme(requireActivity());
    }


    @Override
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("use_custom_theme")) {
            createPref();
        }
        Helper.recreateMainActivity(requireActivity());
    }


    @SuppressWarnings("deprecation")
    @SuppressLint("ApplySharedPref")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMPORT_THEME && resultCode == RESULT_OK) {
            if (data == null || data.getData() == null) {
                Toasty.error(requireActivity(), getString(R.string.theme_file_error), Toast.LENGTH_LONG).show();
                return;
            }
            if (data.getData() != null) {
                try {
                    InputStream inputStream = requireActivity().getContentResolver().openInputStream(data.getData());
                    readFileAndApply(inputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                Toasty.error(requireActivity(), getString(R.string.theme_file_error), Toast.LENGTH_LONG).show();
            }

        }
    }


    @SuppressLint("SetTextI18n")
    @SuppressWarnings("ConstantConditions")
    private void applyColors(PopupStatusThemeBinding binding, int position) {
        LinkedHashMap<String, String> themeData = listOfThemes.get(position);
        int linksColor = -1;
        int iconsColor = -1;
        int textColor = -1;
        int boostHeaderColor = -1;
        int statusColor = -1;
        int displayNameColor = -1;
        int userNameColor = -1;
        int colorAccent = -1;
        int backgroundColor = -1;
        if (themeData.containsKey("theme_link_color")) {
            linksColor = Integer.parseInt(themeData.get("theme_link_color"));
        }
        if (themeData.containsKey("theme_accent")) {
            colorAccent = Integer.parseInt(themeData.get("theme_accent"));
        }
        if (themeData.containsKey("theme_icons_color")) {
            iconsColor = Integer.parseInt(themeData.get("theme_icons_color"));
        }
        if (themeData.containsKey("theme_text_color")) {
            textColor = Integer.parseInt(themeData.get("theme_text_color"));
        }
        if (themeData.containsKey("theme_boost_header_color")) {
            boostHeaderColor = Integer.parseInt(themeData.get("theme_boost_header_color"));
        }
        if (themeData.containsKey("theme_statuses_color")) {
            statusColor = Integer.parseInt(themeData.get("theme_statuses_color"));
        }
        if (themeData.containsKey("theme_text_header_1_line")) {
            displayNameColor = Integer.parseInt(themeData.get("theme_text_header_1_line"));
        }
        if (themeData.containsKey("theme_text_header_2_line")) {
            userNameColor = Integer.parseInt(themeData.get("theme_text_header_2_line"));
        }
        if (themeData.containsKey("pref_color_background")) {
            backgroundColor = Integer.parseInt(themeData.get("pref_color_background"));
        }

        if (colorAccent != -1) {
            binding.spoilerExpand.setTextColor(colorAccent);
            binding.cardTitle.setTextColor(colorAccent);
        }
        if (backgroundColor != -1) {
            binding.background.setBackgroundColor(backgroundColor);
        }
        if (statusColor != -1) {
            binding.cardviewContainer.setBackgroundColor(statusColor);
            binding.card.setBackgroundColor(statusColor);
        }
        if (boostHeaderColor != -1) {
            binding.headerContainer.setBackgroundColor(boostHeaderColor);
        }
        if (textColor != -1) {
            binding.statusContent.setTextColor(textColor);
            binding.statusContentTranslated.setTextColor(textColor);
            binding.spoiler.setTextColor(textColor);
            binding.cardDescription.setTextColor(textColor);
            binding.time.setTextColor(textColor);
            binding.reblogsCount.setTextColor(textColor);
            binding.favoritesCount.setTextColor(textColor);
            Helper.changeDrawableColor(requireActivity(), binding.repeatInfo, textColor);
            Helper.changeDrawableColor(requireActivity(), binding.favInfo, textColor);
        }
        if (linksColor != -1) {
            binding.cardUrl.setTextColor(linksColor);
        } else {
            binding.cardUrl.setTextColor(ThemeHelper.getAttColor(requireActivity(), R.attr.linkColor));
        }
        if (iconsColor == -1) {
            iconsColor = ThemeHelper.getAttColor(requireActivity(), R.attr.iconColor);
        }
        Helper.changeDrawableColor(requireActivity(), binding.actionButtonReply, iconsColor);
        Helper.changeDrawableColor(requireActivity(), binding.actionButtonMore, iconsColor);
        Helper.changeDrawableColor(requireActivity(), binding.actionButtonBoost, iconsColor);
        Helper.changeDrawableColor(requireActivity(), binding.actionButtonFavorite, iconsColor);
        Helper.changeDrawableColor(requireActivity(), R.drawable.ic_person, iconsColor);
        if (displayNameColor != -1) {
            binding.displayName.setTextColor(displayNameColor);
        } else {
            binding.displayName.setTextColor(ThemeHelper.getAttColor(requireActivity(), R.attr.statusTextColor));
        }
        if (userNameColor != -1) {
            binding.username.setTextColor(userNameColor);
            Helper.changeDrawableColor(requireActivity(), binding.statusBoostIcon, userNameColor);
        } else {
            binding.username.setTextColor(ThemeHelper.getAttColor(requireActivity(), R.attr.statusTextColor));
            Helper.changeDrawableColor(requireActivity(), binding.statusBoostIcon, ThemeHelper.getAttColor(requireActivity(), R.attr.statusTextColor));
        }
        Glide.with(binding.getRoot().getContext())
                .load(R.drawable.fedilab_logo_bubbles)
                .into(binding.statusBoosterAvatar);
        Glide.with(binding.getRoot().getContext())
                .load(R.drawable.fedilab_logo_bubbles)
                .into(binding.avatar);
        binding.displayName.setText("Fedilab");
        binding.username.setText("@apps@toot.fedilab.app");

        binding.author.setText(themeData.get("author"));
        binding.title.setText(themeData.get("name"));
        binding.cardviewContainer.invalidate();
        binding.time.setText(Helper.dateToString(new Date()));
    }

    @SuppressLint("ApplySharedPref")
    private void readFileAndApply(InputStream inputStream) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String sCurrentLine;
            SharedPreferences.Editor appEditor = appPref.edit();
            Cyanea.Editor cyaneaEditor = Cyanea.getInstance().edit();
            appEditor.putBoolean("use_custom_theme", true);
            while ((sCurrentLine = br.readLine()) != null) {
                String[] line = sCurrentLine.split(",");
                if (line.length > 1) {
                    String key = line[0];
                    String value = line[1];
                    if (key.compareTo("pref_color_navigation_bar") == 0) {
                        cyaneaEditor.shouldTintNavBar(Boolean.parseBoolean(value));
                    } else if (key.compareTo("pref_color_background") == 0) {
                        cyaneaEditor.backgroundDarkLighter(Integer.parseInt(value));
                        cyaneaEditor.backgroundLightDarker(Integer.parseInt(value));
                        cyaneaEditor.backgroundDark(Integer.parseInt(value));
                        cyaneaEditor.backgroundLightLighter(Integer.parseInt(value));
                        cyaneaEditor.backgroundDarkDarker(Integer.parseInt(value));
                        cyaneaEditor.background(Integer.parseInt(value));
                        cyaneaEditor.backgroundDark(Integer.parseInt(value));
                        cyaneaEditor.backgroundLight(Integer.parseInt(value));
                    } else if (key.compareTo("base_theme") == 0) {
                        List<CyaneaTheme> list = CyaneaTheme.Companion.from(requireActivity().getAssets(), "themes/cyanea_themes.json");
                        CyaneaTheme theme = list.get(Integer.parseInt(value));
                        cyaneaEditor.baseTheme(theme.getBaseTheme());
                        if (Integer.parseInt(value) == 0 || Integer.parseInt(value) == 2) {
                            cyaneaEditor.menuIconColor(ContextCompat.getColor(requireActivity(), R.color.dark_text));
                            cyaneaEditor.subMenuIconColor(ContextCompat.getColor(requireActivity(), R.color.dark_text));
                        } else {
                            cyaneaEditor.menuIconColor(ContextCompat.getColor(requireActivity(), R.color.black));
                            cyaneaEditor.subMenuIconColor(ContextCompat.getColor(requireActivity(), R.color.black));
                        }
                    } else if (key.compareTo("theme_accent") == 0) {
                        cyaneaEditor.accentLight(Integer.parseInt(value));
                        cyaneaEditor.accent(Integer.parseInt(value));
                        cyaneaEditor.accentDark(Integer.parseInt(value));
                    } else if (key.compareTo("theme_primary") == 0) {
                        cyaneaEditor.primary(Integer.parseInt(value));
                        cyaneaEditor.primaryLight(Integer.parseInt(value));
                        cyaneaEditor.primaryDark(Integer.parseInt(value));
                    } else {
                        if (value != null && value.matches("-?\\d+")) {
                            appEditor.putInt(key, Integer.parseInt(value));
                        } else {
                            appEditor.remove(key);
                        }
                    }
                }
            }
            appEditor.commit();
            cyaneaEditor.apply().recreate(requireActivity());
            Helper.recreateMainActivity(requireActivity());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void createPref() {
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
        addPreferencesFromResource(R.xml.pref_theming);
        if (getPreferenceScreen() == null) {
            Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
            return;
        }
        Preference launch_custom_theme = findPreference("launch_custom_theme");
        if (launch_custom_theme != null) {
            launch_custom_theme.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireActivity(), CyaneaSettingsActivity.class));
                return false;
            });

        }
        Preference contributors_themes = findPreference("contributors_themes");
        if (contributors_themes != null) {
            contributors_themes.setOnPreferenceClickListener(preference -> {
                final int[] currentPosition = {0};
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(requireActivity(), Helper.dialogStyle());
                builderSingle.setTitle(getString(R.string.select_a_theme));
                PopupStatusThemeBinding binding = PopupStatusThemeBinding.inflate(getLayoutInflater(), new LinearLayout(requireActivity()), false);
                binding.selectTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                        currentPosition[0] = position;
                        applyColors(binding, position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                applyColors(binding, 0);
                builderSingle.setView(binding.getRoot());
                String[] listOfTheme = new String[listOfThemes.size()];
                int i = 0;
                for (LinkedHashMap<String, String> values : listOfThemes) {
                    listOfTheme[i] = values.get("name");
                    i++;
                }
                //fill data in spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, listOfTheme);
                binding.selectTheme.setAdapter(adapter);
                binding.selectTheme.setSelection(0);
                builderSingle.setPositiveButton(R.string.validate, (dialog, which) -> {
                    try {
                        String[] list = requireActivity().getAssets().list("themes/contributors");
                        InputStream is = requireActivity().getAssets().open("themes/contributors/" + list[currentPosition[0]]);
                        readFileAndApply(is);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                });
                builderSingle.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                builderSingle.show();
                return false;
            });

        }

        ListPreference settings_theme = findPreference("settings_theme");
        if (settings_theme != null) {
            settings_theme.getContext().setTheme(Helper.dialogStyle());
        }

        Preference theme_link_color = findPreference("theme_link_color");
        Preference theme_boost_header_color = findPreference("theme_boost_header_color");
        Preference theme_text_header_1_line = findPreference("theme_text_header_1_line");
        Preference theme_text_header_2_line = findPreference("theme_text_header_2_line");
        Preference theme_statuses_color = findPreference("theme_statuses_color");
        Preference theme_icons_color = findPreference("theme_icons_color");
        Preference theme_text_color = findPreference("theme_text_color");
        Preference pref_import = findPreference("pref_import");
        Preference pref_export = findPreference("pref_export");
        Preference reset_pref = findPreference("reset_pref");
        PreferenceCategory cyanea_preference_category = getPreferenceScreen().findPreference("cyanea_preference_category");
        //No custom theme data must be removed
        if (!appPref.getBoolean("use_custom_theme", false) && cyanea_preference_category != null) {
            if (theme_link_color != null) {
                cyanea_preference_category.removePreference(theme_link_color);
            }
            if (theme_boost_header_color != null) {
                cyanea_preference_category.removePreference(theme_boost_header_color);
            }
            if (theme_text_header_1_line != null) {
                cyanea_preference_category.removePreference(theme_text_header_1_line);
            }
            if (theme_text_header_2_line != null) {
                cyanea_preference_category.removePreference(theme_text_header_2_line);
            }
            if (contributors_themes != null) {
                cyanea_preference_category.removePreference(contributors_themes);
            }
            if (theme_statuses_color != null) {
                cyanea_preference_category.removePreference(theme_statuses_color);
            }
            if (theme_icons_color != null) {
                cyanea_preference_category.removePreference(theme_icons_color);
            }
            if (theme_text_color != null) {
                cyanea_preference_category.removePreference(theme_text_color);
            }
            if (reset_pref != null) {
                cyanea_preference_category.removePreference(reset_pref);
            }
            if (pref_export != null) {
                cyanea_preference_category.removePreference(pref_export);
            }
        }
        //These are default values (first three ones)
        if (pref_export != null) {
            pref_export.setOnPreferenceClickListener(preference -> {
                exportColors();
                return true;
            });
        }

        if (pref_import != null) {
            pref_import.setOnPreferenceClickListener(preference -> {
                if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            ComposeActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    return true;
                }
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] mimetypes = {"*/*"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                startActivityForResult(intent, PICK_IMPORT_THEME);
                return true;
            });
        }
        if (reset_pref != null) {
            reset_pref.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireActivity(), Helper.dialogStyle());
                dialogBuilder.setMessage(R.string.reset_color);
                dialogBuilder.setPositiveButton(R.string.reset, (dialog, id) -> {
                    reset();
                    dialog.dismiss();
                    setPreferenceScreen(null);
                    createPref();

                });
                dialogBuilder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.setCancelable(false);
                alertDialog.show();
                return true;
            });
        }
    }

    @SuppressLint("ApplySharedPref")
    private void reset() {

        SharedPreferences.Editor editor = appPref.edit();
        editor.remove("theme_link_color");
        editor.remove("theme_boost_header_color");
        editor.remove("theme_text_header_1_line");
        editor.remove("theme_text_header_2_line");
        editor.remove("theme_icons_color");
        editor.remove("theme_text_color");
        editor.remove("use_custom_theme");
        editor.commit();
    }


    private void exportColors() {

        try {
            String fileName = "Fedilab_color_export_" + Helper.dateFileToString(getActivity(), new Date()) + ".csv";
            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            String fullPath = filePath + "/" + fileName;
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fullPath), StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            int theme_boost_header_color = appPref.getInt("theme_boost_header_color", -1);
            int theme_text_header_1_line = appPref.getInt("theme_text_header_1_line", -1);
            int theme_text_header_2_line = appPref.getInt("theme_text_header_2_line", -1);
            int theme_statuses_color = appPref.getInt("theme_statuses_color", -1);
            int theme_link_color = appPref.getInt("theme_link_color", -1);
            int theme_icons_color = appPref.getInt("theme_icons_color", -1);
            int pref_color_background = cyneaPref.getInt("pref_color_background", -1);
            boolean pref_color_navigation_bar = cyneaPref.getBoolean("pref_color_navigation_bar", true);
            boolean pref_color_status_bar = cyneaPref.getBoolean("pref_color_status_bar", true);
            int theme_accent = cyneaPref.getInt("theme_accent", -1);
            int theme_text_color = appPref.getInt("theme_text_color", -1);
            int theme_primary = cyneaPref.getInt("theme_primary", -1);

            int theme = appPref.getInt(getString(R.string.SET_THEME), 0);


            builder.append("base_theme").append(',');
            builder.append(theme);
            builder.append('\n');

            builder.append("theme_boost_header_color").append(',');
            builder.append(theme_boost_header_color);
            builder.append('\n');

            builder.append("theme_text_header_1_line").append(',');
            builder.append(theme_text_header_1_line);
            builder.append('\n');

            builder.append("theme_text_header_2_line").append(',');
            builder.append(theme_text_header_2_line);
            builder.append('\n');

            builder.append("theme_statuses_color").append(',');
            builder.append(theme_statuses_color);
            builder.append('\n');

            builder.append("theme_link_color").append(',');
            builder.append(theme_link_color);
            builder.append('\n');

            builder.append("theme_icons_color").append(',');
            builder.append(theme_icons_color);
            builder.append('\n');

            builder.append("pref_color_background").append(',');
            builder.append(pref_color_background);
            builder.append('\n');

            builder.append("pref_color_navigation_bar").append(',');
            builder.append(pref_color_navigation_bar);
            builder.append('\n');

            builder.append("pref_color_status_bar").append(',');
            builder.append(pref_color_status_bar);
            builder.append('\n');

            builder.append("theme_accent").append(',');
            builder.append(theme_accent);
            builder.append('\n');

            builder.append("theme_text_color").append(',');
            builder.append(theme_text_color);
            builder.append('\n');

            builder.append("theme_primary").append(',');
            builder.append(theme_primary);
            builder.append('\n');


            pw.write(builder.toString());
            pw.close();
            String message = getString(R.string.data_export_theme_success);
            Intent intentOpen = new Intent();
            intentOpen.setAction(android.content.Intent.ACTION_VIEW);
            Uri uri = Uri.parse("file://" + fullPath);
            intentOpen.setDataAndType(uri, "text/csv");
            String title = getString(R.string.data_export_theme);
            Helper.notify_user(getActivity(), Helper.NOTIFICATION_THEMING, BaseMainActivity.accountWeakReference.get(), intentOpen, BitmapFactory.decodeResource(requireActivity().getResources(),
                    R.mipmap.ic_launcher), Helper.NotifType.BACKUP, title, message);
        } catch (Exception e) {
            e.printStackTrace();
            Toasty.error(requireActivity(), getString(R.string.toast_error), Toast.LENGTH_LONG).show();
        }
    }
}
