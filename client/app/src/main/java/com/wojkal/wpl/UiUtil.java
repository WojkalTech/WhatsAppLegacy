package com.wojkal.wpl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

import java.util.Locale;


public class UiUtil {

    public static int dp(Context c, float value) {
        return (int) (value * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    
    public static GradientDrawable roundRect(int fill, int radiusPx, int strokeColor, int strokePx) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fill);
        d.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            d.setStroke(strokePx, strokeColor);
        }
        return d;
    }

    
    public static StateListDrawable pressableRoundRect(int normal, int pressed, int radiusPx) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, roundRect(pressed, radiusPx, 0, 0));
        s.addState(new int[]{}, roundRect(normal, radiusPx, 0, 0));
        return s;
    }

    
    public static Bitmap circle(Bitmap src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        int left = (src.getWidth() - size) / 2;
        int top = (src.getHeight() - size) / 2;
        canvas.drawBitmap(src,
                new Rect(left, top, left + size, top + size),
                new Rect(0, 0, size, size),
                paint);
        return output;
    }

    
    public static Bitmap placeholder(String name, int size) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor("#B0BEC5"));
        c.drawCircle(size / 2f, size / 2f, size / 2f, p);

        String letter = "?";
        String t = name == null ? "" : name.trim();
        if (t.length() > 0 && Character.isLetterOrDigit(t.charAt(0))) {
            letter = t.substring(0, 1).toUpperCase(Locale.getDefault());
        }
        p.setColor(Color.WHITE);
        p.setTextSize(size * 0.45f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(letter, size / 2f, size / 2f - (p.descent() + p.ascent()) / 2f, p);
        return bmp;
    }

    
    public static Bitmap decodeSampled(byte[] data, int targetSize) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        int sample = 1;
        while (o.outWidth / (sample * 2) >= targetSize) {
            sample *= 2;
        }
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(data, 0, data.length, o);
    }

    
    public static Bitmap searchIcon(int size, int color) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(size * 0.11f);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawCircle(size * 0.42f, size * 0.42f, size * 0.28f, p);
        c.drawLine(size * 0.63f, size * 0.63f, size * 0.88f, size * 0.88f, p);
        return bmp;
    }

    
    public static Bitmap backArrow(int size, int color) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(size * 0.10f);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(size * 0.85f, size * 0.50f, size * 0.25f, size * 0.50f, p);
        c.drawLine(size * 0.25f, size * 0.50f, size * 0.55f, size * 0.20f, p);
        c.drawLine(size * 0.25f, size * 0.50f, size * 0.55f, size * 0.80f, p);
        return bmp;
    }
}
