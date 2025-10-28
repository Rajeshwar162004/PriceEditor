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

        // ============================================
        // PAINT STYLES
        // ============================================

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

        // ============================================
        // GET DATE/DAY/TIME
        // ============================================
        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        String dateStr = userDate.isEmpty() ? dateFmt.format(now.getTime()) : userDate;
        String dayStr = userDay.isEmpty() ? dayFmt.format(now.getTime()).toUpperCase() : userDay.toUpperCase();
        String timeStr = userTime.isEmpty() ? timeFmt.format(now.getTime()).toUpperCase() : userTime.toUpperCase();

        // ============================================
        // PERCENTAGE-BASED POSITIONING
        // Assuming template height is 1920 pixels in Paint
        // Adjust these based on your solvent template measurements
        // ============================================

        float dateY = 0.2900f * h;      // 489 / 1920
        float dayTimeY = 0.3343f * h;   // 533 / 1920
        float rateY = 0.3831f * h;      // 611 / 1920
        float deliveryY = 0.4218f * h;  // 677 / 1920
        float paymentY = 0.5156f * h;   // 780 / 1920

        // ============================================
        // DRAW TEXT AT EXACT POSITIONS
        // ============================================

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
