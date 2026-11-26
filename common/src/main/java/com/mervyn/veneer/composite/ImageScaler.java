package com.mervyn.veneer.composite;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.Mth;

public final class ImageScaler {
    private ImageScaler() {
    }

    public static NativeImage upscaleNearest(NativeImage source, int targetWidth, int targetHeight) {
        validate(source.getWidth(), source.getHeight(), targetWidth, targetHeight);
        NativeImage result = new NativeImage(targetWidth, targetHeight, false);
        for (int y = 0; y < targetHeight; y++) {
            int sourceY = y * source.getHeight() / targetHeight;
            for (int x = 0; x < targetWidth; x++) {
                result.setPixel(x, y, source.getPixel(x * source.getWidth() / targetWidth, sourceY));
            }
        }
        return result;
    }

    static void validate(int width, int height, int targetWidth, int targetHeight) {
        validateResample(width, height, targetWidth, targetHeight);
        if (targetWidth < width || targetHeight < height) {
            throw new IllegalArgumentException("Cannot scale " + width + "x" + height + " to " + targetWidth + "x" + targetHeight
                    + ": downscaling is not supported");
        }
    }

    static void validateResample(int width, int height, int targetWidth, int targetHeight) {
        String sizes = width + "x" + height + " to " + targetWidth + "x" + targetHeight;
        if (!Mth.isPowerOfTwo(width) || !Mth.isPowerOfTwo(height)
                || !Mth.isPowerOfTwo(targetWidth) || !Mth.isPowerOfTwo(targetHeight)) {
            throw new IllegalArgumentException("Cannot scale " + sizes + ": dimensions must be powers of two");
        }
        if ((long) width * targetHeight != (long) height * targetWidth) {
            throw new IllegalArgumentException("Cannot scale " + sizes + ": aspect ratios differ");
        }
    }
}
