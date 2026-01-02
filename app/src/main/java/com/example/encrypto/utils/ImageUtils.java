package com.example.encrypto.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    private static final int MAX_DIMENSION = 1080;
    private static final int COMPRESSION_QUALITY = 80;

    /**
     * Helper to get optimized byte array directly from a Gallery URI.
     * This handles decoding, resizing, and compressing in one step.
     */
    public static byte[] getBytesFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (original == null) return null;

            Bitmap resized = resizeBitmap(original, MAX_DIMENSION);

            return bitmapToJpegBytes(resized, COMPRESSION_QUALITY);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Compresses a bitmap to JPEG byte array.
     */
    public static byte[] bitmapToJpegBytes(Bitmap bmp, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    /**
     * Resizes a bitmap maintaining aspect ratio.
     */
    private static Bitmap resizeBitmap(Bitmap source, int maxLength) {
        try {
            if (source.getHeight() <= maxLength && source.getWidth() <= maxLength) {
                return source;
            }

            float aspectRatio = (float) source.getWidth() / (float) source.getHeight();
            int width = maxLength;
            int height = maxLength;

            if (aspectRatio > 1) {
                // Landscape
                height = (int) (width / aspectRatio);
            } else {
                // Portrait
                width = (int) (height * aspectRatio);
            }

            return Bitmap.createScaledBitmap(source, width, height, true);
        } catch (Exception e) {
            return source;
        }
    }
}