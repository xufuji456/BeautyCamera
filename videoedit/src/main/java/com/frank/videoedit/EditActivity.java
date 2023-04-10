package com.frank.videoedit;

import static com.google.android.exoplayer2.util.Assertions.checkNotNull;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.List;

public class EditActivity extends AppCompatActivity {

    public static final String AUDIO_MIME_TYPE = "audio_mime_type";
    public static final String VIDEO_MIME_TYPE = "video_mime_type";
    public static final String RESOLUTION_HEIGHT = "resolution_height";
    public static final String ROTATE_DEGREES = "rotate_degrees";
    public static final String ENABLE_FALLBACK = "enable_fallback";
    public static final String ENABLE_DEBUG_PREVIEW = "enable_debug_preview";
    public static final String ENABLE_REQUEST_SDR_TONE_MAPPING = "enable_request_sdr_tone_mapping";
    public static final String FORCE_INTERPRET_HDR_VIDEO_AS_SDR = "force_interpret_hdr_video_as_sdr";
    public static final String ENABLE_HDR_EDITING = "enable_hdr_editing";
    public static final String DEMO_EFFECTS_SELECTIONS = "demo_effects_selections";
    public static final String PERIODIC_VIGNETTE_CENTER_X = "periodic_vignette_center_x";
    public static final String PERIODIC_VIGNETTE_CENTER_Y = "periodic_vignette_center_y";
    public static final String PERIODIC_VIGNETTE_INNER_RADIUS = "periodic_vignette_inner_radius";
    public static final String PERIODIC_VIGNETTE_OUTER_RADIUS = "periodic_vignette_outer_radius";
    public static final String CONTRAST_VALUE = "contrast_value";
    public static final String HSL_ADJUSTMENTS_HUE = "hsl_adjustments_hue";
    public static final String HSL_ADJUSTMENTS_SATURATION = "hsl_adjustments_saturation";
    public static final String HSL_ADJUSTMENTS_LIGHTNESS = "hsl_adjustments_lightness";
    public static final int FILE_PERMISSION_REQUEST_CODE = 1;
    private static final String[] PRESET_FILE_URIS = {
            "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4",
            "https://storage.googleapis.com/exoplayer-test-media-0/android-block-1080-hevc.mp4",
            "https://storage.googleapis.com/exoplayer-test-media-1/mp4/portrait_4k60.mp4",
            "https://storage.googleapis.com/exoplayer-test-media-1/mp4/8k24fps_4s.mp4",
            "https://storage.googleapis.com/exoplayer-test-media-1/mp4/1920w_1080h_4s.mp4",
            "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
            "https://storage.googleapis.com/exoplayer-test-media-1/mp4/portrait_avc_aac.mp4",
            "https://storage.googleapis.com/exoplayer-test-media-1/mp4/portrait_rotated_avc_aac.mp4",
            "https://storage.googleapis.com/exoplayer-test-media-1/mp4/samsung-s21-hdr-hdr10.mp4",
    };
    private static final String[] PRESET_FILE_URI_DESCRIPTIONS = { // same order as PRESET_FILE_URIS
            "720p H264 video and AAC audio",
            "1080p H265 video and AAC audio",
            "360p H264 video and AAC audio",
            "360p VP8 video and Vorbis audio",
            "4K H264 video and AAC audio (portrait, no B-frames)",
            "8k H265 video and AAC audio",
            "Short 1080p H265 video and AAC audio",
            "Long 180p H264 video and AAC audio",
            "H264 video and AAC audio (portrait, H > W, 0\u00B0)",
            "H264 video and AAC audio (portrait, H < W, 90\u00B0)",
            "SEF slow motion with 240 fps",
            "480p DASH (non-square pixels)",
            "HDR (HDR10) H265 limited range video (encoding may fail)",
    };
    private static final String[] DEMO_EFFECTS = {
            "HSL Adjustments",
            "Contrast",
            "Periodic vignette",
            "Zoom in start",
            "Overlay logo & timer",
    };

    private static final int HSL_ADJUSTMENT_INDEX    = 0;
    private static final int CONTRAST_INDEX          = 1;
    private static final int PERIODIC_VIGNETTE_INDEX = 2;
    private static final String SAME_AS_INPUT_OPTION = "same as input";
    private static final float HALF_DIAGONAL = 1f / (float) Math.sqrt(2);

    private ActivityResultLauncher<Intent> localFilePickerLauncher;
    private Button selectPresetFileButton;
    private Button selectLocalFileButton;
    private TextView selectedFileTextView;
    private Spinner audioMimeSpinner;
    private Spinner videoMimeSpinner;
    private Spinner resolutionHeightSpinner;
    private CheckBox enableFallbackCheckBox;
    private CheckBox enableDebugPreviewCheckBox;
    private CheckBox enableRequestSdrToneMappingCheckBox;
    private CheckBox forceInterpretHdrVideoAsSdrCheckBox;
    private CheckBox enableHdrEditingCheckBox;
    private Button selectDemoEffectsButton;
    private boolean[] demoEffectsSelections;
    private @Nullable Uri localFileUri;
    private int inputUriPosition;
    private float contrastValue;
    private float hueAdjustment;
    private float saturationAdjustment;
    private float lightnessAdjustment;
    private float periodicVignetteCenterX;
    private float periodicVignetteCenterY;
    private float periodicVignetteInnerRadius;
    private float periodicVignetteOuterRadius;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        findViewById(R.id.transform_button).setOnClickListener(this::startTransformation);

        selectedFileTextView = findViewById(R.id.selected_file_text_view);
        selectedFileTextView.setText(PRESET_FILE_URI_DESCRIPTIONS[inputUriPosition]);

        selectPresetFileButton = findViewById(R.id.select_preset_file_button);
        selectPresetFileButton.setOnClickListener(this::selectPresetFile);

        selectLocalFileButton = findViewById(R.id.select_local_file_button);
        selectLocalFileButton.setOnClickListener(this::selectLocalFile);

        ArrayAdapter<String> audioMimeAdapter =
                new ArrayAdapter<>(/* context= */ this, R.layout.spinner_item);
        audioMimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        audioMimeSpinner = findViewById(R.id.audio_mime_spinner);
        audioMimeSpinner.setAdapter(audioMimeAdapter);
        audioMimeAdapter.addAll(
                SAME_AS_INPUT_OPTION, MimeTypes.AUDIO_AAC, MimeTypes.AUDIO_AMR_NB, MimeTypes.AUDIO_AMR_WB);

        ArrayAdapter<String> videoMimeAdapter =
                new ArrayAdapter<>(/* context= */ this, R.layout.spinner_item);
        videoMimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        videoMimeSpinner = findViewById(R.id.video_mime_spinner);
        videoMimeSpinner.setAdapter(videoMimeAdapter);
        videoMimeAdapter.addAll(
                SAME_AS_INPUT_OPTION, MimeTypes.VIDEO_H263, MimeTypes.VIDEO_H264, MimeTypes.VIDEO_MP4V);
        if (Util.SDK_INT >= 24) {
            videoMimeAdapter.add(MimeTypes.VIDEO_H265);
        }

        ArrayAdapter<String> resolutionHeightAdapter =
                new ArrayAdapter<>(/* context= */ this, R.layout.spinner_item);
        resolutionHeightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionHeightSpinner = findViewById(R.id.resolution_height_spinner);
        resolutionHeightSpinner.setAdapter(resolutionHeightAdapter);
        resolutionHeightAdapter.addAll(
                SAME_AS_INPUT_OPTION, "144", "240", "360", "480", "720", "1080", "1440", "2160");

        enableFallbackCheckBox = findViewById(R.id.enable_fallback_checkbox);
        enableDebugPreviewCheckBox = findViewById(R.id.enable_debug_preview_checkbox);
        enableRequestSdrToneMappingCheckBox = findViewById(R.id.request_sdr_tone_mapping_checkbox);
        enableRequestSdrToneMappingCheckBox.setEnabled(isRequestSdrToneMappingSupported());
        findViewById(R.id.request_sdr_tone_mapping).setEnabled(isRequestSdrToneMappingSupported());
        forceInterpretHdrVideoAsSdrCheckBox =
                findViewById(R.id.force_interpret_hdr_video_as_sdr_checkbox);
        enableHdrEditingCheckBox = findViewById(R.id.hdr_editing_checkbox);

        demoEffectsSelections = new boolean[DEMO_EFFECTS.length];
        selectDemoEffectsButton = findViewById(R.id.select_demo_effects_button);
        selectDemoEffectsButton.setOnClickListener(this::selectDemoEffects);

        localFilePickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        this::localFilePickerLauncherResult);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FILE_PERMISSION_REQUEST_CODE
                && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchLocalFilePicker();
        } else {
            Toast.makeText(
                            getApplicationContext(), getString(R.string.permission_denied), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        @Nullable Uri intentUri = getIntent().getData();
        if (intentUri != null) {
            checkNotNull(selectPresetFileButton).setEnabled(false);
            checkNotNull(selectLocalFileButton).setEnabled(false);
            checkNotNull(selectedFileTextView).setText(intentUri.toString());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void startTransformation(View view) {
        Intent transformerIntent = new Intent(/* packageContext= */ this, TransformerActivity.class);
        Bundle bundle = new Bundle();
        String selectedAudioMimeType = String.valueOf(audioMimeSpinner.getSelectedItem());
        if (!SAME_AS_INPUT_OPTION.equals(selectedAudioMimeType)) {
            bundle.putString(AUDIO_MIME_TYPE, selectedAudioMimeType);
        }
        String selectedVideoMimeType = String.valueOf(videoMimeSpinner.getSelectedItem());
        if (!SAME_AS_INPUT_OPTION.equals(selectedVideoMimeType)) {
            bundle.putString(VIDEO_MIME_TYPE, selectedVideoMimeType);
        }
        String selectedResolutionHeight = String.valueOf(resolutionHeightSpinner.getSelectedItem());
        if (!SAME_AS_INPUT_OPTION.equals(selectedResolutionHeight)) {
            bundle.putInt(RESOLUTION_HEIGHT, Integer.parseInt(selectedResolutionHeight));
        }
        bundle.putFloat(ROTATE_DEGREES, 0.0f);
        bundle.putBoolean(ENABLE_FALLBACK, enableFallbackCheckBox.isChecked());
        bundle.putBoolean(ENABLE_DEBUG_PREVIEW, enableDebugPreviewCheckBox.isChecked());
        bundle.putBoolean(
                ENABLE_REQUEST_SDR_TONE_MAPPING, enableRequestSdrToneMappingCheckBox.isChecked());
        bundle.putBoolean(
                FORCE_INTERPRET_HDR_VIDEO_AS_SDR, forceInterpretHdrVideoAsSdrCheckBox.isChecked());
        bundle.putBoolean(ENABLE_HDR_EDITING, enableHdrEditingCheckBox.isChecked());
        bundle.putBooleanArray(DEMO_EFFECTS_SELECTIONS, demoEffectsSelections);
        bundle.putFloat(CONTRAST_VALUE, contrastValue);
        bundle.putFloat(HSL_ADJUSTMENTS_HUE, hueAdjustment);
        bundle.putFloat(HSL_ADJUSTMENTS_SATURATION, saturationAdjustment);
        bundle.putFloat(HSL_ADJUSTMENTS_LIGHTNESS, lightnessAdjustment);
        bundle.putFloat(PERIODIC_VIGNETTE_CENTER_X, periodicVignetteCenterX);
        bundle.putFloat(PERIODIC_VIGNETTE_CENTER_Y, periodicVignetteCenterY);
        bundle.putFloat(PERIODIC_VIGNETTE_INNER_RADIUS, periodicVignetteInnerRadius);
        bundle.putFloat(PERIODIC_VIGNETTE_OUTER_RADIUS, periodicVignetteOuterRadius);
        transformerIntent.putExtras(bundle);

        @Nullable Uri intentUri;
        if (getIntent().getData() != null) {
            intentUri = getIntent().getData();
        } else if (localFileUri != null) {
            intentUri = localFileUri;
        } else {
            intentUri = Uri.parse(PRESET_FILE_URIS[inputUriPosition]);
        }
        transformerIntent.setData(intentUri);

        startActivity(transformerIntent);
    }

    private void selectPresetFile(View view) {
        new AlertDialog.Builder(/* context= */ this)
                .setTitle(R.string.select_preset_file_title)
                .setSingleChoiceItems(
                        PRESET_FILE_URI_DESCRIPTIONS, inputUriPosition, this::selectPresetFileInDialog)
                .setPositiveButton(android.R.string.ok, /* listener= */ null)
                .create()
                .show();
    }

    private void selectPresetFileInDialog(DialogInterface dialog, int which) {
        inputUriPosition = which;
        localFileUri = null;
        selectedFileTextView.setText(PRESET_FILE_URI_DESCRIPTIONS[inputUriPosition]);
    }

    private void selectLocalFile(View view) {
        int permissionStatus =
                ActivityCompat.checkSelfPermission(
                        EditActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            String[] neededPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(
                    EditActivity.this, neededPermissions, FILE_PERMISSION_REQUEST_CODE);
        } else {
            launchLocalFilePicker();
        }
    }

    private void launchLocalFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        checkNotNull(localFilePickerLauncher).launch(intent);
    }

    private void localFilePickerLauncherResult(ActivityResult result) {
        Intent data = result.getData();
        if (data != null) {
            localFileUri = checkNotNull(data.getData());
            selectedFileTextView.setText(localFileUri.toString());
        }
    }

    private void selectDemoEffects(View view) {
        new AlertDialog.Builder(/* context= */ this)
                .setTitle(R.string.select_demo_effects)
                .setMultiChoiceItems(
                        DEMO_EFFECTS, checkNotNull(demoEffectsSelections), this::selectDemoEffect)
                .setPositiveButton(android.R.string.ok, /* listener= */ null)
                .create()
                .show();
    }

    private void selectDemoEffect(DialogInterface dialog, int which, boolean isChecked) {
        demoEffectsSelections[which] = isChecked;
        if (!isChecked) {
            return;
        }

        switch (which) {
            case CONTRAST_INDEX:
                controlContrastSettings();
                break;
            case HSL_ADJUSTMENT_INDEX:
                controlHslAdjustmentSettings();
                break;
            case PERIODIC_VIGNETTE_INDEX:
                controlPeriodicVignetteSettings();
                break;
            default:
                break;
        }
    }

    private void controlContrastSettings() {
        View dialogView = getLayoutInflater().inflate(R.layout.contrast_options, /* root= */ null);
        Slider contrastSlider = checkNotNull(dialogView.findViewById(R.id.contrast_slider));
        new AlertDialog.Builder(/* context= */ this)
                .setView(dialogView)
                .setPositiveButton(
                        android.R.string.ok,
                        (DialogInterface dialogInterface, int i) -> contrastValue = contrastSlider.getValue())
                .create()
                .show();
    }

    private void controlHslAdjustmentSettings() {
        View dialogView =
                getLayoutInflater().inflate(R.layout.hsl_adjust_options, /* root= */ null);
        Slider hueAdjustmentSlider = checkNotNull(dialogView.findViewById(R.id.hsl_adjustments_hue));
        Slider saturationAdjustmentSlider =
                checkNotNull(dialogView.findViewById(R.id.hsl_adjustments_saturation));
        Slider lightnessAdjustmentSlider =
                checkNotNull(dialogView.findViewById(R.id.hsl_adjustment_lightness));
        new AlertDialog.Builder(/* context= */ this)
                .setTitle(R.string.hsl_adjustment_options)
                .setView(dialogView)
                .setPositiveButton(
                        android.R.string.ok,
                        (DialogInterface dialogInterface, int i) -> {
                            hueAdjustment = hueAdjustmentSlider.getValue();
                            saturationAdjustment = saturationAdjustmentSlider.getValue();
                            lightnessAdjustment = lightnessAdjustmentSlider.getValue();
                        })
                .create()
                .show();
    }

    private void controlPeriodicVignetteSettings() {
        View dialogView =
                getLayoutInflater().inflate(R.layout.periodic_vignette_options, /* root= */ null);
        Slider centerXSlider =
                checkNotNull(dialogView.findViewById(R.id.periodic_vignette_center_x_slider));
        Slider centerYSlider =
                checkNotNull(dialogView.findViewById(R.id.periodic_vignette_center_y_slider));
        RangeSlider radiusRangeSlider =
                checkNotNull(dialogView.findViewById(R.id.periodic_vignette_radius_range_slider));
        radiusRangeSlider.setValues(0f, HALF_DIAGONAL);
        new AlertDialog.Builder(/* context= */ this)
                .setTitle(R.string.periodic_vignette_options)
                .setView(dialogView)
                .setPositiveButton(
                        android.R.string.ok,
                        (DialogInterface dialogInterface, int i) -> {
                            periodicVignetteCenterX = centerXSlider.getValue();
                            periodicVignetteCenterY = centerYSlider.getValue();
                            List<Float> radiusRange = radiusRangeSlider.getValues();
                            periodicVignetteInnerRadius = radiusRange.get(0);
                            periodicVignetteOuterRadius = radiusRange.get(1);
                        })
                .create()
                .show();
    }

    private static boolean isRequestSdrToneMappingSupported() {
        return Util.SDK_INT >= 31;
    }
}