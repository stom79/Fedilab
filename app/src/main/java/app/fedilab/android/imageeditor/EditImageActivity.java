package app.fedilab.android.imageeditor;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityEditImageBinding;
import app.fedilab.android.helper.CirclesDrawingView;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.imageeditor.base.BaseActivity;
import app.fedilab.android.imageeditor.filters.FilterListener;
import app.fedilab.android.imageeditor.filters.FilterViewAdapter;
import app.fedilab.android.imageeditor.tools.EditingToolsAdapter;
import app.fedilab.android.imageeditor.tools.ToolType;
import es.dmoral.toasty.Toasty;
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;
import ja.burhanrashid52.photoeditor.ViewType;
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder;
import ja.burhanrashid52.photoeditor.shape.ShapeType;

public class EditImageActivity extends BaseActivity implements OnPhotoEditorListener,
        View.OnClickListener,
        PropertiesBSFragment.Properties,
        ShapeBSFragment.Properties,
        EmojiBSFragment.EmojiListener, EditingToolsAdapter.OnItemSelected, FilterListener {

    private static final int CAMERA_REQUEST = 52;
    private static final int PICK_REQUEST = 53;
    private final int STORE_REQUEST = 54;
    private final EditingToolsAdapter mEditingToolsAdapter = new EditingToolsAdapter(this);
    private final FilterViewAdapter mFilterViewAdapter = new FilterViewAdapter(this);
    private final ConstraintSet mConstraintSet = new ConstraintSet();
    PhotoEditor mPhotoEditor;
    String path;
    private PropertiesBSFragment mPropertiesBSFragment;
    private ShapeBSFragment mShapeBSFragment;
    private ShapeBuilder mShapeBuilder;
    private EmojiBSFragment mEmojiBSFragment;
    private boolean mIsFilterVisible;
    private Uri uri;
    private boolean exit;
    private ActivityEditImageBinding binding;
    CropImageContractOptions cropImageContractOptions;
    ActivityResultLauncher<CropImageContractOptions> cropImageContractOptionsActivityResultLauncher;

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Bundle b = getIntent().getExtras();
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        if (b != null)
            path = b.getString("imageUri", null);

        if (path == null) {
            finish();
        }
        uri = Uri.parse("file://" + path);
        exit = false;
        initViews();
        mPropertiesBSFragment = new PropertiesBSFragment();
        mEmojiBSFragment = new EmojiBSFragment();
        mShapeBSFragment = new ShapeBSFragment();
        mEmojiBSFragment.setEmojiListener(this);
        mPropertiesBSFragment.setPropertiesChangeListener(this);
        mShapeBSFragment.setPropertiesChangeListener(this);

        LinearLayoutManager llmTools = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvConstraintTools.setLayoutManager(llmTools);
        binding.rvConstraintTools.setAdapter(mEditingToolsAdapter);

        LinearLayoutManager llmFilters = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvFilterView.setLayoutManager(llmFilters);
        binding.rvFilterView.setAdapter(mFilterViewAdapter);

        Typeface mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf");

        mPhotoEditor = new PhotoEditor.Builder(this, binding.photoEditorView)
                .setPinchTextScalable(true)
                .setDefaultEmojiTypeface(mEmojiTypeFace)
                .build(); // build photo editor sdk

        mPhotoEditor.setOnPhotoEditorListener(this);


        //Set Image Dynamically
        try {
            binding.photoEditorView.getSource().setImageURI(uri);
        } catch (Exception e) {
            Toasty.error(EditImageActivity.this, getString(R.string.toast_error)).show();
        }

        if (uri != null) {
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                ExifInterface exif = new ExifInterface(inputStream);
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationInDegrees = exifToDegrees(rotation);
                binding.photoEditorView.getSource().setRotation(rotationInDegrees);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        cropImageContractOptions = new CropImageContractOptions(uri, new CropImageOptions())
                .setGuidelines(CropImageView.Guidelines.ON)
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setAllowRotation(true)
                .setAllowFlipping(true)
                .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                .setAllowCounterRotation(true)
                .setImageSource(true, false)
                .setScaleType(CropImageView.ScaleType.CENTER);
        cropImageContractOptionsActivityResultLauncher = registerForActivityResult(
                new CropImageContract(),
                result -> {
                    if (result.isSuccessful()) {
                        Uri resultUri = result.getUriContent();
                        if (resultUri != null) {
                            binding.photoEditorView.getSource().setImageURI(resultUri);
                            if (uri != null && uri.getPath() != null) {
                                File fdelete = new File(uri.getPath());
                                if (fdelete.exists()) {
                                    //noinspection ResultOfMethodCallIgnored
                                    fdelete.delete();
                                }
                            }
                            uri = resultUri;
                        }
                    } else {
                        Log.e(Helper.TAG, "onActivityResult...Error CropImage: " + result.getError());
                    }
                });
        mPhotoEditor.setFilterEffect(PhotoFilter.NONE);
        binding.send.setOnClickListener(v -> {
            exit = true;
            saveImage();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORE_REQUEST) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage();
            }
        }
    }


    private void initViews() {
        binding.imgUndo.setOnClickListener(this);
        binding.imgRedo.setOnClickListener(this);
        binding.imgClose.setOnClickListener(this);
    }

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode) {
        TextEditorDialogFragment textEditorDialogFragment =
                TextEditorDialogFragment.show(this, text, colorCode);
        textEditorDialogFragment.setOnTextEditorListener((inputText, newColorCode) -> {
            final TextStyleBuilder styleBuilder = new TextStyleBuilder();
            styleBuilder.withTextColor(newColorCode);

            mPhotoEditor.editText(rootView, inputText, styleBuilder);
            binding.txtCurrentTool.setText(R.string.label_text);
        });
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
    }

    @Override
    public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
    }

    @Override
    public void onTouchSourceImage(MotionEvent event) {
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imgUndo) {
            mPhotoEditor.undo();
        } else if (id == R.id.imgRedo) {
            mPhotoEditor.redo();
        } else if (id == R.id.imgClose) {
            onBackPressed();
        }
    }

    private void saveImage() {
        if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showLoading(getString(R.string.saving));
            File file = new File(path);
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();

                SaveSettings saveSettings = new SaveSettings.Builder()
                        .setClearViewsEnabled(true)
                        .setTransparencyEnabled(true)
                        .build();
                if (ContextCompat.checkSelfPermission(EditImageActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(EditImageActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            STORE_REQUEST);
                    return;
                }
                mPhotoEditor.saveAsFile(file.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                    @Override
                    public void onSuccess(@NonNull String imagePath) {
                        hideLoading();
                        showSnackbar(getString(R.string.image_saved));
                        binding.photoEditorView.getSource().setImageURI(Uri.fromFile(new File(imagePath)));
                        if (exit) {
                            Intent intentImage = new Intent(Helper.INTENT_SEND_MODIFIED_IMAGE);
                            intentImage.putExtra("imgpath", imagePath);
                            CirclesDrawingView.CircleArea circleArea = binding.focusCircle.getTouchedCircle();
                            if (circleArea != null) {
                                //Dimension of the editor containing the image
                                int pHeight = binding.photoEditorView.getHeight();
                                int pWidth = binding.photoEditorView.getWidth();
                                //Load the original image in a bitmap
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(new File(imagePath).getAbsolutePath(), options);
                                //Get height and width of the original image
                                int imageHeight = options.outHeight;
                                int imageWidth = options.outWidth;

                                //Evaluate the dimension of the image in the editor
                                int imgHeightInEditor;
                                int imgWidthInEditor;
                                //If the original image has its height greater than width => heights are equals
                                float focusX = -2, focusY = -2;
                                if (imageHeight > imageWidth) {
                                    imgHeightInEditor = pHeight;
                                    float ratio = (float) pHeight / (float) imageHeight;
                                    imgWidthInEditor = (int) (pWidth * ratio);
                                } else { //Otherwise widths are equals
                                    imgWidthInEditor = pWidth;
                                    float ratio = (float) pWidth / (float) imageWidth;
                                    imgHeightInEditor = (int) (pHeight * ratio);
                                }
                                focusY = (float) (circleArea.centerY * 2 - imgHeightInEditor / 2) / (float) imgHeightInEditor - 0.5f;
                                focusX = (float) (circleArea.centerX * 2 - imgWidthInEditor / 2) / (float) imgWidthInEditor - 0.5f;
                                if (focusX > 1) {
                                    focusX = 1;
                                } else if (focusX < -1) {
                                    focusX = -1;
                                }
                                if (focusY > 1) {
                                    focusY = 1;
                                } else if (focusY < -1) {
                                    focusY = -1;
                                }
                                intentImage.putExtra("focusX", focusX);
                                intentImage.putExtra("focusY", focusY);

                            }

                            LocalBroadcastManager.getInstance(EditImageActivity.this).sendBroadcast(intentImage);
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        hideLoading();
                        showSnackbar(getString(R.string.save_image_failed));
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                hideLoading();
                if (e.getMessage() != null) {
                    showSnackbar(e.getMessage());
                }
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            ExifInterface exif;
            int rotation;
            int rotationInDegrees = 0;
            if (data != null && data.getData() != null) {
                try (InputStream inputStream = getContentResolver().openInputStream(data.getData())) {
                    assert inputStream != null;
                    exif = new ExifInterface(inputStream);
                    rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    rotationInDegrees = exifToDegrees(rotation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            switch (requestCode) {
                case CAMERA_REQUEST:
                    if (data != null && data.getExtras() != null) {
                        mPhotoEditor.clearAllViews();
                        Bitmap photo = (Bitmap) data.getExtras().get("data");
                        binding.photoEditorView.getSource().setImageBitmap(photo);
                        binding.photoEditorView.getSource().setRotation(rotationInDegrees);
                    }
                    break;
                case PICK_REQUEST:
                    if (data != null && data.getData() != null) {
                        try {
                            mPhotoEditor.clearAllViews();
                            Uri uri = data.getData();
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            binding.photoEditorView.getSource().setImageBitmap(bitmap);
                            binding.photoEditorView.getSource().setRotation(rotationInDegrees);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                    if (data != null && data.getData() != null) {
                        CropImageContractOptions cropImageContractOptions = new CropImageContractOptions(data.getData(), new CropImageOptions())
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setCropShape(CropImageView.CropShape.RECTANGLE)
                                .setAllowRotation(true)
                                .setAllowFlipping(true)
                                .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                                .setAllowCounterRotation(true)
                                .setImageSource(true, false)
                                .setScaleType(CropImageView.ScaleType.CENTER);
                        ActivityResultLauncher<CropImageContractOptions> cropImageContractOptionsActivityResultLauncher = registerForActivityResult(
                                new CropImageContract(),
                                result -> {
                                    if (result.isSuccessful()) {
                                        Uri resultUri = result.getUriContent();
                                        if (resultUri != null) {
                                            binding.photoEditorView.getSource().setImageURI(resultUri);
                                            if (uri != null && uri.getPath() != null) {
                                                File fdelete = new File(uri.getPath());
                                                if (fdelete.exists()) {
                                                    //noinspection ResultOfMethodCallIgnored
                                                    fdelete.delete();
                                                }
                                            }
                                            uri = resultUri;
                                        }
                                    } else {
                                        Log.e(Helper.TAG, "onActivityResult...Error CropImage: " + result.getError());
                                    }
                                });
                        cropImageContractOptionsActivityResultLauncher.launch(cropImageContractOptions);
                    }
                    break;
            }
        }
    }

    @Override
    public void onColorChanged(int colorCode) {
        mPhotoEditor.setShape(mShapeBuilder.withShapeColor(colorCode));
        binding.txtCurrentTool.setText(R.string.label_brush);
    }

    @Override
    public void onOpacityChanged(int opacity) {
        mPhotoEditor.setShape(mShapeBuilder.withShapeOpacity(opacity));
        binding.txtCurrentTool.setText(R.string.label_brush);
    }

    @Override
    public void onShapeSizeChanged(int shapeSize) {
        mPhotoEditor.setShape(mShapeBuilder.withShapeSize(shapeSize));
        binding.txtCurrentTool.setText(R.string.label_brush);
    }

    @Override
    public void onShapePicked(ShapeType shapeType) {
        mPhotoEditor.setShape(mShapeBuilder.withShapeType(shapeType));
    }

    @Override
    public void onEmojiClick(String emojiUnicode) {
        mPhotoEditor.addEmoji(emojiUnicode);
        binding.txtCurrentTool.setText(R.string.label_emoji);
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.msg_save_image));
        builder.setPositiveButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.setNegativeButton(R.string.discard, (dialog, which) -> finish());
        builder.create().show();

    }

    @Override
    public void onFilterSelected(PhotoFilter photoFilter) {
        mPhotoEditor.setFilterEffect(photoFilter);
    }

    @Override
    public void onToolSelected(ToolType toolType) {
        binding.focusCircle.setVisibility(View.GONE);
        switch (toolType) {
            case SHAPE:
                mPhotoEditor.setBrushDrawingMode(true);
                mShapeBuilder = new ShapeBuilder();
                mPhotoEditor.setShape(mShapeBuilder);
                binding.txtCurrentTool.setText(R.string.label_shape);
                showBottomSheetDialogFragment(mShapeBSFragment);
                break;
            case TEXT:
                TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this);
                textEditorDialogFragment.setOnTextEditorListener((inputText, colorCode) -> {
                    final TextStyleBuilder styleBuilder = new TextStyleBuilder();
                    styleBuilder.withTextColor(colorCode);

                    mPhotoEditor.addText(inputText, styleBuilder);
                    binding.txtCurrentTool.setText(R.string.label_text);
                });
                break;
            case ERASER:
                mPhotoEditor.brushEraser();
                binding.txtCurrentTool.setText(R.string.label_eraser_mode);
                break;
            case FILTER:
                binding.txtCurrentTool.setText(R.string.label_filter);
                showFilter(true);
                break;
            case EMOJI:
                showBottomSheetDialogFragment(mEmojiBSFragment);
                break;
            case BRUSH:
                mPhotoEditor.setBrushDrawingMode(true);
                binding.txtCurrentTool.setText(R.string.label_brush);
                mPropertiesBSFragment.show(getSupportFragmentManager(), mPropertiesBSFragment.getTag());
                break;
            case CROP:

                cropImageContractOptionsActivityResultLauncher.launch(cropImageContractOptions);
                break;
            case FOCUS:
                binding.focusCircle.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showBottomSheetDialogFragment(BottomSheetDialogFragment fragment) {
        if (fragment == null || fragment.isAdded()) {
            return;
        }
        fragment.show(getSupportFragmentManager(), fragment.getTag());
    }


    void showFilter(boolean isVisible) {
        mIsFilterVisible = isVisible;
        mConstraintSet.clone(binding.rootView);

        if (isVisible) {
            mConstraintSet.clear(binding.rvFilterView.getId(), ConstraintSet.START);
            mConstraintSet.connect(binding.rvFilterView.getId(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START);
            mConstraintSet.connect(binding.rvFilterView.getId(), ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END);
        } else {
            mConstraintSet.connect(binding.rvFilterView.getId(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.END);
            mConstraintSet.clear(binding.rvFilterView.getId(), ConstraintSet.END);
        }

        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(350);
        changeBounds.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
        TransitionManager.beginDelayedTransition(binding.rootView, changeBounds);

        mConstraintSet.applyTo(binding.rootView);
    }

    @Override
    public void onBackPressed() {
        if (mIsFilterVisible) {
            showFilter(false);
            binding.txtCurrentTool.setText(R.string.app_name);
        } else if (!mPhotoEditor.isCacheEmpty()) {
            showSaveDialog();
        } else {
            super.onBackPressed();
        }
    }
}
