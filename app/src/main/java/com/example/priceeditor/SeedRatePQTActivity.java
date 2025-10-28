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

public class SeedRatePQTActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private EditText etRate, etDate, etTime;
    private Bitmap templateBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seed_rate_pqtactivity);

        imagePreview = findViewById(R.id.imagePreview);
        etRate = findViewById(R.id.etRate);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        Button btnPreview = findViewById(R.id.btnPreview);
        Button btnExport = findViewById(R.id.btnExport);

        // Load template (your existing PQT template)
        templateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.template);
        imagePreview.setImageBitmap(templateBitmap);

        btnPreview.setOnClickListener(v -> {
            Bitmap rendered = renderImage(
                    templateBitmap,
                    etRate.getText().toString().trim(),
                    etDate.getText().toString().trim(),
                    etTime.getText().toString().trim()
            );
            imagePreview.setImageBitmap(rendered);
        });

        btnExport.setOnClickListener(v -> {
            Bitmap rendered = renderImage(
                    templateBitmap,
                    etRate.getText().toString().trim(),
                    etDate.getText().toString().trim(),
                    etTime.getText().toString().trim()
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

    private Bitmap renderImage(Bitmap base, String rate, String userDate, String userTime) {
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

        // Adjust these positions for your template
        float rateX = 0.495f * w, rateY = 0.295f * h;
        float dateX = 0.195f * w, dateY = 0.22f * h;
        float timeX = 0.60f * w, timeY = 0.22f * h;

        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

        String dateStr = userDate.isEmpty() ? dateFmt.format(now.getTime()) : userDate;
        String timeStr = userTime.isEmpty() ? timeFmt.format(now.getTime()) : userTime;

        ratePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText((rate.isEmpty() ? "" : rate) + "/-", rateX, rateY, ratePaint);

        datePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("DATE: " + dateStr, dateX, dateY, datePaint);

        timePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("TIME:"+ timeStr, timeX, timeY, timePaint);

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
