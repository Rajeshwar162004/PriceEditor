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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SolventOilActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private EditText etOilRate, etPerKg, etDate, etDay, etTime;
    private Bitmap templateBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_solvent_oil);

        // Set action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Soya Solvent Oil");
        }

        imagePreview = findViewById(R.id.imagePreview);
        etOilRate = findViewById(R.id.etOilRate);
        etPerKg = findViewById(R.id.etPerKg);
        etDate = findViewById(R.id.etDate);
        etDay = findViewById(R.id.etDay);
        etTime = findViewById(R.id.etTime);
        Button btnPreview = findViewById(R.id.btnPreview);
        Button btnExport = findViewById(R.id.btnExport);

        templateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.solvent);
        imagePreview.setImageBitmap(templateBitmap);

        // Add validation for Oil Rate field (only numbers)
        etOilRate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String rate = s.toString().trim();
                if (!rate.isEmpty() && !rate.matches("\\d+")) {
                    etOilRate.setError("Enter a valid number");
                } else {
                    etOilRate.setError(null);
                }
            }
        });

        // Add auto-formatting mask for Date field (dd/MM/yyyy)
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

                    clean = String.format("%s/%s/%s", clean.substring(0, 2),
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

        // Add validation for Day field (only alphabets and spaces)
        etDay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String day = s.toString().trim();
                if (!day.isEmpty() && !day.matches("[a-zA-Z ]+")) {
                    etDay.setError("Enter only letters");
                } else {
                    etDay.setError(null);
                }
            }
        });

        // Add auto-formatting mask for Time field (12-hour format hh:mm AM/PM)
        etTime.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFormatting) return;

                String input = s.toString();
                if (input.equals(current)) return;

                isFormatting = true;

                String clean = input.replaceAll("[^\\d]", "");

                if (clean.length() == 0) {
                    current = "";
                    etTime.setText("");
                    isFormatting = false;
                    return;
                }

                StringBuilder formatted = new StringBuilder();
                int len = Math.min(clean.length(), 4);

                for (int i = 0; i < len; i++) {
                    if (i == 2) {
                        formatted.append(":");
                    }
                    formatted.append(clean.charAt(i));
                }

                if (clean.length() >= 4) {
                    int hh = Integer.parseInt(clean.substring(0, 2));
                    int mm = Integer.parseInt(clean.substring(2, 4));

                    if (hh < 1) hh = 1;
                    if (hh > 12) hh = 12;
                    if (mm > 59) mm = 59;

                    formatted = new StringBuilder(String.format("%02d:%02d", hh, mm));

                    if (hh >= 1 && hh <= 11) {
                        formatted.append(" AM");
                    } else {
                        formatted.append(" PM");
                    }
                }

                current = formatted.toString();
                etTime.setText(current);
                etTime.setSelection(current.length());

                isFormatting = false;
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        btnPreview.setOnClickListener(v -> {
            Bitmap rendered = renderImage(
                    templateBitmap,
                    etOilRate.getText().toString().trim(),
                    etPerKg.getText().toString().trim(),
                    etDate.getText().toString().trim(),
                    etDay.getText().toString().trim(),
                    etTime.getText().toString().trim()
            );
            imagePreview.setImageBitmap(rendered);
        });

        btnExport.setOnClickListener(v -> {
            Bitmap rendered = renderImage(
                    templateBitmap,
                    etOilRate.getText().toString().trim(),
                    etPerKg.getText().toString().trim(),
                    etDate.getText().toString().trim(),
                    etDay.getText().toString().trim(),
                    etTime.getText().toString().trim()
            );
            try {
                Uri uri = saveToGallery(this, rendered, "solvent_oil");
                Toast.makeText(this, "Saved to Gallery: " + uri.getLastPathSegment(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e("ExportError", "Error saving image", e);
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap renderImage(Bitmap base, String oilRate, String perKg, String userDate, String userDay, String userTime) {
        Bitmap bmp = base.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);

        float w = bmp.getWidth();
        float h = bmp.getHeight();
        float centerX = w / 2;

        Paint blackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackPaint.setColor(Color.parseColor("#1C1C1C"));
        blackPaint.setTextSize(w * 0.038f);
        blackPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        blackPaint.setTextAlign(Paint.Align.CENTER);

        Paint brownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        brownPaint.setColor(Color.parseColor("#8B4513"));
        brownPaint.setTextSize(w * 0.032f);
        brownPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        brownPaint.setTextAlign(Paint.Align.CENTER);

        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        String dateStr = userDate.isEmpty() ? dateFmt.format(now.getTime()) : userDate;
        String dayStr = userDay.isEmpty() ? dayFmt.format(now.getTime()).toUpperCase() : userDay.toUpperCase();
        String timeStr = userTime.isEmpty() ? timeFmt.format(now.getTime()).toUpperCase() : userTime.toUpperCase();

        float dateY = 0.2900f * h;
        float dayTimeY = 0.3343f * h;
        float rateY = 0.3831f * h;
        float deliveryY = 0.4218f * h;
        float paymentY = 0.5156f * h;

        canvas.drawText("DATE :- " + dateStr, centerX, dateY, brownPaint);
        canvas.drawText("DAY :- " + dayStr + "  TIME :- " + timeStr, centerX, dayTimeY, brownPaint);

        String rateText = "SOYA SOLVENT OIL RATE - " + (oilRate.isEmpty() ? "" : oilRate + "/- Per " + perKg + " +GST");
        canvas.drawText(rateText, centerX, rateY, blackPaint);

        canvas.drawText("DELIVERY PERIOD - WAR TO WAR", centerX, deliveryY, blackPaint);
        canvas.drawText("PAYMENT CONDITION : EX FACTORY ADVANCE 100%", centerX, paymentY, blackPaint);

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
