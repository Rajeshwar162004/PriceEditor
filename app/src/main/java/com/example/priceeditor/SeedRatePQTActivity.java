package com.example.priceeditor;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SeedRatePQTActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private EditText etRate, etDate, etTime;
    private RadioGroup rgAmPm;
    private Bitmap templateBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seed_rate_pqtactivity);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        imagePreview = findViewById(R.id.imagePreview);
        etRate = findViewById(R.id.etRate);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        rgAmPm = findViewById(R.id.rgAmPm);
        Button btnPreview = findViewById(R.id.btnPreview);
        Button btnExport = findViewById(R.id.btnExport);

        // Load template
        templateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.template);
        imagePreview.setImageBitmap(templateBitmap);

        // Set default AM/PM based on current time
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        if (currentHour < 12) {
            rgAmPm.check(R.id.rbAM);
        } else {
            rgAmPm.check(R.id.rbPM);
        }

        // Add validation for Rate field (only numbers)
        etRate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String rate = s.toString().trim();
                if (!rate.isEmpty() && !rate.matches("\\d+")) {
                    etRate.setError("Enter a valid number");
                } else {
                    etRate.setError(null);
                }
            }
        });

        // Add auto-formatting mask for Date field (dd-MM-yyyy)
        etDate.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private String ddmmyyyy = "DDMMYYYY";
            private Calendar cal = Calendar.getInstance();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d.]", "");
                    String cleanC = current.replaceAll("[^\\d.]", "");

                    int cl = clean.length();
                    int sel = cl;
                    for (int i = 2; i <= cl && i < 6; i += 2) {
                        sel++;
                    }
                    if (clean.equals(cleanC)) sel--;

                    if (clean.length() < 8) {
                        clean = clean + ddmmyyyy.substring(clean.length());
                    } else {
                        int day = Integer.parseInt(clean.substring(0, 2));
                        int mon = Integer.parseInt(clean.substring(2, 4));
                        int year = Integer.parseInt(clean.substring(4, 8));

                        mon = mon < 1 ? 1 : mon > 12 ? 12 : mon;
                        cal.set(Calendar.MONTH, mon - 1);
                        year = (year < 1900) ? 1900 : (year > 2100) ? 2100 : year;
                        cal.set(Calendar.YEAR, year);
                        day = (day > cal.getActualMaximum(Calendar.DATE)) ? cal.getActualMaximum(Calendar.DATE) : day;
                        clean = String.format("%02d%02d%04d", day, mon, year);
                    }

                    clean = String.format("%s-%s-%s", clean.substring(0, 2),
                            clean.substring(2, 4),
                            clean.substring(4, 8));

                    sel = sel < 0 ? 0 : sel;
                    current = clean;
                    etDate.setText(current);
                    etDate.setSelection(sel < current.length() ? sel : current.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Improved time field formatting - waits for user to complete input
        etTime.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFormatting) {
                    return;
                }

                String input = s.toString();
                if (!input.equals(current)) {
                    String digitsOnly = input.replaceAll("[^\\d]", "");

                    if (digitsOnly.length() == 0) {
                        current = "";
                        return;
                    }

                    StringBuilder formatted = new StringBuilder();
                    int length = digitsOnly.length();

                    if (length <= 2) {
                        // Just typing hours - don't format yet
                        formatted.append(digitsOnly);
                    } else if (length == 3) {
                        // Has moved to minutes - format hours and add colon
                        int hours = Integer.parseInt(digitsOnly.substring(0, 2));
                        if (hours == 0) {
                            hours = 1;
                        } else if (hours > 12) {
                            hours = 12;
                        }
                        formatted.append(String.format(Locale.getDefault(), "%02d", hours));
                        formatted.append(":");
                        formatted.append(digitsOnly.charAt(2));
                    } else {
                        // Full time entered
                        int hours = Integer.parseInt(digitsOnly.substring(0, 2));
                        if (hours == 0) {
                            hours = 1;
                        } else if (hours > 12) {
                            hours = 12;
                        }
                        formatted.append(String.format(Locale.getDefault(), "%02d", hours));
                        formatted.append(":");

                        int minutes = Integer.parseInt(digitsOnly.substring(2, Math.min(4, length)));
                        if (minutes > 59) {
                            minutes = 59;
                        }
                        formatted.append(String.format(Locale.getDefault(), "%02d", minutes));
                    }

                    current = formatted.toString();
                    isFormatting = true;
                    etTime.setText(current);
                    etTime.setSelection(current.length());
                    isFormatting = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Validate complete time
                String timeStr = s.toString();
                if (timeStr.length() == 5) {
                    String[] parts = timeStr.split(":");
                    if (parts.length == 2) {
                        try {
                            int hour = Integer.parseInt(parts[0]);
                            int minute = Integer.parseInt(parts[1]);

                            if (hour < 1 || hour > 12) {
                                etTime.setError("Hour must be 01-12");
                            } else if (minute < 0 || minute > 59) {
                                etTime.setError("Minute must be 00-59");
                            } else {
                                etTime.setError(null);
                            }
                        } catch (NumberFormatException e) {
                            etTime.setError("Invalid time format");
                        }
                    }
                }
            }
        });

        btnPreview.setOnClickListener(v -> {
            String amPm = getSelectedAmPm();
            Bitmap rendered = renderImage(
                    templateBitmap,
                    etRate.getText().toString().trim(),
                    etDate.getText().toString().trim(),
                    etTime.getText().toString().trim(),
                    amPm
            );
            imagePreview.setImageBitmap(rendered);
        });

        btnExport.setOnClickListener(v -> {
            String amPm = getSelectedAmPm();
            Bitmap rendered = renderImage(
                    templateBitmap,
                    etRate.getText().toString().trim(),
                    etDate.getText().toString().trim(),
                    etTime.getText().toString().trim(),
                    amPm
            );
            try {
                Uri uri = saveToGallery(this, rendered, "soya_pqt");
                Toast.makeText(this, "Saved to Gallery: " + uri.getLastPathSegment(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e("ExportError", "Error saving image", e);
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSelectedAmPm() {
        int selectedId = rgAmPm.getCheckedRadioButtonId();
        if (selectedId == R.id.rbAM) {
            return "AM";
        } else if (selectedId == R.id.rbPM) {
            return "PM";
        }
        // Default based on current time
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) < 12 ? "AM" : "PM";
    }

    private Bitmap renderImage(Bitmap base, String rate, String userDate, String userTime, String amPm) {
        Bitmap bmp = base.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);

        float w = bmp.getWidth();
        float h = bmp.getHeight();

        Paint ratePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ratePaint.setColor(Color.BLACK);
        ratePaint.setTextSize(w * 0.050f);
        ratePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        datePaint.setColor(Color.parseColor("#A52A2A"));
        datePaint.setTextSize(w * 0.035f);
        datePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(Color.parseColor("#A52A2A"));
        timePaint.setTextSize(w * 0.035f);
        timePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        float rateX = 0.495f * w, rateY = 0.295f * h;
        float dateX = 0.195f * w, dateY = 0.22f * h;
        float timeX = 0.60f * w, timeY = 0.22f * h;

        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        String dateStr = userDate.isEmpty() ? dateFmt.format(now.getTime()) : userDate;
        String timeStr;

        if (userTime.isEmpty()) {
            timeStr = timeFmt.format(now.getTime());
        } else {
            timeStr = userTime + " " + amPm;
        }

        ratePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText((rate.isEmpty() ? "" : rate) + "/-", rateX, rateY, ratePaint);

        datePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("DATE: " + dateStr, dateX, dateY, datePaint);

        timePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("TIME: " + timeStr, timeX, timeY, timePaint);

        return bmp;
    }

    private Uri saveToGallery(Context ctx, Bitmap bmp, String prefix) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, prefix + "_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PriceEditor");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Failed to create file in MediaStore");

        try (OutputStream out = ctx.getContentResolver().openOutputStream(uri)) {
            if (out != null) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            } else {
                throw new IOException("OutputStream is null");
            }
        }

        values.clear();
        values.put(MediaStore.Images.Media.IS_PENDING, 0);
        ctx.getContentResolver().update(uri, values, null, null);

        return uri;
    }
}
