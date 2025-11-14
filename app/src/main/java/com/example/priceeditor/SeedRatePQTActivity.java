package com.example.priceeditor;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

public class SeedRatePQTActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private TextInputEditText etRate, etDate, etDay, etTime;
    private TextInputLayout tilRate;
    private MaterialCardView loadingCard;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton btnPreview, btnExport, btnClear, btnAutoFill;

    private Bitmap templateBitmap;
    private Calendar selectedCalendar;
    private boolean isPreviewGenerated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seed_rate_pqtactivity);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupTemplate();
        setupInputListeners();
        setupButtonListeners();
        setupDateTimePickers();
        autoFillCurrentDateTime();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Soya Seed Rate Generator");
        }

        etRate = findViewById(R.id.etRate);
        etDate = findViewById(R.id.etDate);
        etDay = findViewById(R.id.etDay);
        etTime = findViewById(R.id.etTime);
        tilRate = findViewById(R.id.tilRate);

        btnPreview = findViewById(R.id.btnPreview);
        btnExport = findViewById(R.id.btnExport);
        btnClear = findViewById(R.id.btnClear);
        btnAutoFill = findViewById(R.id.btnAutoFill);

        imagePreview = findViewById(R.id.imagePreview);
        loadingCard = findViewById(R.id.loadingCard);
        progressIndicator = findViewById(R.id.progressIndicator);

        selectedCalendar = Calendar.getInstance();
        updateProgress(0);
        btnExport.setEnabled(false);
    }

    private void setupTemplate() {
        templateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.new_templet);
        imagePreview.setImageBitmap(templateBitmap);
        imagePreview.setContentDescription("Soya Seed Rate Template Preview");
    }

    private void setupInputListeners() {
        etRate.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String rate = s.toString().trim();
                if (!rate.isEmpty() && !rate.matches("\\d+")) {
                    tilRate.setError("Enter a valid number");
                } else {
                    tilRate.setError(null);
                    validateForm();
                }
            }
        });

        etDay.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String day = s.toString().trim();
                if (!day.isEmpty() && !day.matches("[a-zA-Z ]+")) {
                    etDay.setError("Enter only letters");
                } else {
                    etDay.setError(null);
                }
            }
        });

        // Date auto-format
        etDate.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private final String ddmmyyyy = "DDMMYYYY";
            private final Calendar cal = Calendar.getInstance();
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d.]", "");
                    String cleanC = current.replaceAll("[^\\d.]", "");
                    int cl = clean.length(); int sel = cl;
                    for (int i=2;i<=cl && i<6;i+=2) sel++;
                    if (clean.equals(cleanC)) sel--;
                    if (clean.length()<8){
                        clean = clean + ddmmyyyy.substring(clean.length());
                    } else {
                        int day = Integer.parseInt(clean.substring(0,2));
                        int mon = Integer.parseInt(clean.substring(2,4));
                        int year = Integer.parseInt(clean.substring(4,8));
                        mon = Math.max(1, Math.min(12, mon));
                        cal.set(Calendar.MONTH, mon-1);
                        year = Math.max(1900, Math.min(2100, year));
                        cal.set(Calendar.YEAR, year);
                        day = Math.min(day, cal.getActualMaximum(Calendar.DATE));
                        clean = String.format(Locale.US, "%02d%02d%04d", day, mon, year);
                    }
                    clean = String.format(Locale.US, "%s/%s/%s", clean.substring(0,2), clean.substring(2,4), clean.substring(4,8));
                    current = clean;
                    etDate.setText(current);
                    etDate.setSelection(Math.min(sel, current.length()));
                }
            }
            @Override public void afterTextChanged(Editable s){}
        });

        // TextWatcher for etTime removed - time picker handles it correctly
    }

    private void setupDateTimePickers() {
        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    selectedCalendar.set(Calendar.YEAR, y);
                    selectedCalendar.set(Calendar.MONTH, m);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, d);
                    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat dayf = new SimpleDateFormat("EEEE", Locale.US);
                    etDate.setText(df.format(selectedCalendar.getTime()));
                    etDay.setText(dayf.format(selectedCalendar.getTime()).toUpperCase(Locale.US));
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        );
        dlg.show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        TimePickerDialog dlg = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minute);
                    etTime.setText(formatTime(hourOfDay, minute));
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                false
        );
        dlg.show();
    }

    // Convert 24h -> 12h with correct AM/PM
    private String formatTime(int hourOfDay, int minute) {
        boolean pm = hourOfDay >= 12;
        int hour12 = hourOfDay % 12;
        if (hour12 == 0) hour12 = 12;
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, pm ? "PM" : "AM");
    }

    // Current time formatted via HOUR_OF_DAY (no SimpleDateFormat "a")
    private String nowTimeString() {
        Calendar now = Calendar.getInstance();
        return formatTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
    }

    private void setupButtonListeners() {
        btnPreview.setOnClickListener(v -> generatePreview());
        btnExport.setOnClickListener(v -> exportImage());
        btnClear.setOnClickListener(v -> clearForm());
        btnAutoFill.setOnClickListener(v -> autoFillCurrentDateTime());
    }

    private void validateForm() {
        boolean isValid = !etRate.getText().toString().trim().isEmpty();
        btnPreview.setEnabled(isValid);
        updateProgress(isValid ? 100 : 0);
    }

    private void updateProgress(int progress) {
        if (progressIndicator != null) {
            progressIndicator.setProgress(progress);
            progressIndicator.setVisibility(progress > 0 ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void generatePreview() {
        if (!validateRequiredFields()) {
            showSnackbar("Please fill seed rate field");
            return;
        }

        Bitmap rendered = renderImage(
                templateBitmap,
                etRate.getText().toString().trim(),
                etDate.getText().toString().trim(),
                etDay.getText().toString().trim(),
                etTime.getText().toString().trim()
        );

        imagePreview.setImageBitmap(rendered);
        isPreviewGenerated = true;
        btnExport.setEnabled(true);
        showSnackbar("Preview generated successfully");
    }

    private void exportImage() {
        if (!isPreviewGenerated) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Generate Preview First")
                    .setMessage("Please generate a preview before exporting to ensure the image looks correct.")
                    .setPositiveButton("Generate & Export", (d, w) -> {
                        generatePreview();
                        new Handler(Looper.getMainLooper()).postDelayed(this::performExport, 500);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        performExport();
    }

    private void performExport() {
        showLoading(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Bitmap rendered = renderImage(
                        templateBitmap,
                        etRate.getText().toString().trim(),
                        etDate.getText().toString().trim(),
                        etDay.getText().toString().trim(),
                        etTime.getText().toString().trim()
                );
                Uri uri = saveToGallery(this, rendered, "soya_seed_rate_pqt");
                new Handler(Looper.getMainLooper()).post(() -> {
                    showLoading(false);
                    showExportSuccessDialog(uri);
                });
            } catch (IOException e) {
                Log.e("ExportError", "Error saving image", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    showLoading(false);
                    showSnackbar("Error saving image: " + e.getMessage());
                });
            }
        });
    }

    private void clearForm() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear Form")
                .setMessage("Are you sure you want to clear all fields?")
                .setPositiveButton("Clear", (d, w) -> {
                    etRate.setText("");
                    etDate.setText("");
                    etDay.setText("");
                    etTime.setText("");
                    imagePreview.setImageBitmap(templateBitmap);
                    isPreviewGenerated = false;
                    btnExport.setEnabled(false);
                    validateForm();
                    showSnackbar("Form cleared");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void autoFillCurrentDateTime() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat dayf = new SimpleDateFormat("EEEE", Locale.US);
        etDate.setText(df.format(now.getTime()));
        etDay.setText(dayf.format(now.getTime()).toUpperCase(Locale.US));
        etTime.setText(nowTimeString()); // deterministic AM/PM
        showSnackbar("Current date/time filled");
    }

    private boolean validateRequiredFields() {
        if (etRate.getText().toString().trim().isEmpty()) {
            tilRate.setError("Seed rate is required");
            return false;
        } else {
            tilRate.setError(null);
            return true;
        }
    }

    private void showLoading(boolean show) {
        if (loadingCard != null) loadingCard.setVisibility(show ? View.VISIBLE : View.GONE);
        btnExport.setEnabled(!show);
        btnPreview.setEnabled(!show);
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.primary_green))
                .show();
    }

    private void showExportSuccessDialog(Uri uri) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Export Successful")
                .setMessage("Image has been saved to your gallery.")
                .setPositiveButton("View", (d, w) -> {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "image/png");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(i);
                })
                .setNegativeButton("OK", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private Bitmap renderImage(Bitmap base, String rate, String userDate, String userDay, String userTime) {
        Bitmap bmp = base.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);

        float w = bmp.getWidth();
        float h = bmp.getHeight();
        float centerX = w / 2;

        Paint ratePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ratePaint.setColor(Color.BLACK);
        ratePaint.setTextSize(w * 0.0475f);
        ratePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        ratePaint.setTextAlign(Paint.Align.LEFT);

        Paint brownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        brownPaint.setColor(Color.parseColor("#8B4513"));
        brownPaint.setTextSize(w * 0.032f);
        brownPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        brownPaint.setTextAlign(Paint.Align.CENTER);

        float dateY = 0.2190f * h;
        float dayTimeY = 0.2550f * h;
        float rateY = 0.295f * h;
        float rateX = 0.495f * w;

        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEEE", Locale.US);

        String dateStr = userDate.isEmpty() ? dateFmt.format(now.getTime()) : userDate;
        String dayStr = userDay.isEmpty() ? dayFmt.format(now.getTime()).toUpperCase(Locale.US) : userDay.toUpperCase(Locale.US);
        String timeStr = userTime.isEmpty()
                ? formatTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
                : userTime.toUpperCase(Locale.US);

        canvas.drawText("DATE :- " + dateStr, centerX, dateY, brownPaint);
        canvas.drawText("DAY :- " + dayStr + "  TIME :- " + timeStr, centerX, dayTimeY, brownPaint);
        canvas.drawText((rate.isEmpty() ? "" : rate) + "/-", rateX, rateY, ratePaint);

        return bmp;
    }

    private Uri saveToGallery(Context ctx, Bitmap bmp, String prefix) throws IOException {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, prefix + "_" + System.currentTimeMillis() + ".png");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PriceEditor");
        v.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (uri == null) throw new IOException("Failed to create file in MediaStore");

        try (OutputStream out = ctx.getContentResolver().openOutputStream(uri)) {
            if (out != null) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            } else {
                throw new IOException("OutputStream is null");
            }
        }

        v.clear();
        v.put(MediaStore.Images.Media.IS_PENDING, 0);
        ctx.getContentResolver().update(uri, v, null, null);

        return uri;
    }

    // Tiny utility to reduce boilerplate
    private abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
    }
}