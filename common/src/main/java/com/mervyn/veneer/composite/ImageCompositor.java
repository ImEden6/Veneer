package com.mervyn.veneer.composite;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.ArrayList;
import java.util.List;

public final class ImageCompositor {
    private ImageCompositor() {
    }

    public static NativeImage composite(List<LayerSpec> layers) {
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("Cannot composite an empty layer list");
        }

        List<NativeImage> inputs = new ArrayList<>();
        for (LayerSpec layer : layers) {
            inputs.add(layer.image());
            if (layer.mask() != null) {
                inputs.add(layer.mask());
            }
        }
        int width = inputs.stream().mapToInt(NativeImage::getWidth).max().getAsInt();
        int height = inputs.stream().mapToInt(NativeImage::getHeight).max().getAsInt();
        for (NativeImage input : inputs) {
            ImageScaler.validate(input.getWidth(), input.getHeight(), width, height);
        }

        NativeImage canvas = new NativeImage(width, height, true);
        compositeInto(canvas, 0, width, height, layers.stream()
                .map(layer -> new FrameLayerSpec(layer.image(), 0, layer.image().getWidth(), layer.image().getHeight(),
                        layer.blendMode(), layer.tintRgb(), layer.mask(), layer.invertMask()))
                .toList());
        return canvas;
    }

    public static void compositeInto(NativeImage dst, int dstYOffset, int targetFrameW, int targetFrameH, List<FrameLayerSpec> layers) {
        for (FrameLayerSpec layer : layers) {
            ImageScaler.validate(layer.srcFrameW(), layer.srcFrameH(), targetFrameW, targetFrameH);
            if (layer.mask() != null) {
                ImageScaler.validate(layer.mask().getWidth(), layer.mask().getHeight(), targetFrameW, targetFrameH);
            }
        }

        for (FrameLayerSpec layer : layers) {
            NativeImage mask = layer.mask();
            for (int y = 0; y < targetFrameH; y++) {
                int srcY = y * layer.srcFrameH() / targetFrameH;
                int dstY = dstYOffset + y;
                for (int x = 0; x < targetFrameW; x++) {
                    int srcX = x * layer.srcFrameW() / targetFrameW;
                    int pixel = layer.image().getPixelRGBA(srcX, layer.srcYOffset() + srcY);
                    if (layer.interpWeight255() > 0) {
                        pixel = PixelMath.lerpAbgr(pixel, layer.image().getPixelRGBA(srcX, layer.nextSrcYOffset() + srcY),
                                layer.interpWeight255() / 255.0F);
                    }
                    if (layer.tintRgb().isPresent()) {
                        pixel = PixelMath.applyTint(pixel, layer.tintRgb().getAsInt());
                    }
                    if (mask != null) {
                        int maskPixel = mask.getPixelRGBA(x * mask.getWidth() / targetFrameW, y * mask.getHeight() / targetFrameH);
                        pixel = PixelMath.applyMask(pixel, maskPixel, layer.invertMask());
                    }
                    dst.setPixelRGBA(x, dstY, layer.blendMode().blend(dst.getPixelRGBA(x, dstY), pixel));
                }
            }
        }
    }

    public static void compositePbrInto(NativeImage dst, int dstYOffset, int targetFrameW, int targetFrameH, PbrType pbrType,
                                        List<PbrFrameLayerSpec> layers) {
        for (PbrFrameLayerSpec layer : layers) {
            ImageScaler.validate(layer.albedoFrameW(), layer.albedoFrameH(), targetFrameW, targetFrameH);
            if (layer.mask() != null) {
                ImageScaler.validate(layer.mask().getWidth(), layer.mask().getHeight(), targetFrameW, targetFrameH);
            }
            if (layer.pbrImage() != null) {
                ImageScaler.validate(layer.pbrFrameW(), layer.pbrFrameH(), targetFrameW, targetFrameH);
            }
        }

        dst.fillRect(0, dstYOffset, targetFrameW, targetFrameH, pbrType.neutralAbgr());
        for (PbrFrameLayerSpec layer : layers) {
            NativeImage mask = layer.mask();
            NativeImage pbr = layer.pbrImage();
            for (int y = 0; y < targetFrameH; y++) {
                int albedoY = y * layer.albedoFrameH() / targetFrameH;
                int pbrY = y * layer.pbrFrameH() / targetFrameH;
                float weight = layer.interpWeight255() / 255.0F;
                int dstY = dstYOffset + y;
                for (int x = 0; x < targetFrameW; x++) {
                    int albedoX = x * layer.albedoFrameW() / targetFrameW;
                    int albedo = layer.albedo().getPixelRGBA(albedoX, layer.albedoYOffset() + albedoY);
                    if (layer.interpWeight255() > 0) {
                        albedo = PixelMath.lerpAbgr(albedo,
                                layer.albedo().getPixelRGBA(albedoX, layer.nextAlbedoYOffset() + albedoY), weight);
                    }
                    if (mask != null) {
                        int maskPixel = mask.getPixelRGBA(x * mask.getWidth() / targetFrameW, y * mask.getHeight() / targetFrameH);
                        albedo = PixelMath.applyMask(albedo, maskPixel, layer.invertMask());
                    }
                    int source = pbrType.neutralAbgr();
                    if (pbr != null) {
                        int pbrX = x * layer.pbrFrameW() / targetFrameW;
                        source = pbr.getPixelRGBA(pbrX, layer.pbrYOffset() + pbrY);
                        if (layer.interpWeight255() > 0) {
                            source = pbrType.lerp(source, pbr.getPixelRGBA(pbrX, layer.nextPbrYOffset() + pbrY), weight);
                        }
                    }
                    dst.setPixelRGBA(x, dstY, pbrType.blend(dst.getPixelRGBA(x, dstY), source, PixelMath.alpha(albedo)));
                }
            }
        }
    }
}
