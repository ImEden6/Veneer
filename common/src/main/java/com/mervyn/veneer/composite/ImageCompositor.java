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
        for (LayerSpec layer : layers) {
            layer.transform().validate(layer.image().getWidth(), layer.image().getHeight());
        }

        NativeImage canvas = new NativeImage(width, height, true);
        compositeInto(canvas, 0, width, height, layers.stream()
                .map(layer -> new FrameLayerSpec(layer.image(), 0, layer.image().getWidth(), layer.image().getHeight(),
                        layer.blendMode(), layer.tintRgb(), layer.mask(), layer.invertMask(),
                        0, 0, layer.opacity(), layer.transform()))
                .toList());
        return canvas;
    }

    public static void compositeInto(NativeImage dst, int dstYOffset, int targetFrameW, int targetFrameH, List<FrameLayerSpec> layers) {
        for (FrameLayerSpec layer : layers) {
            ImageScaler.validate(layer.srcFrameW(), layer.srcFrameH(), targetFrameW, targetFrameH);
            layer.transform().validate(layer.srcFrameW(), layer.srcFrameH());
            if (layer.mask() != null) {
                ImageScaler.validate(layer.mask().getWidth(), layer.mask().getHeight(), targetFrameW, targetFrameH);
            }
        }

        for (FrameLayerSpec layer : layers) {
            NativeImage mask = layer.mask();
            Transform transform = layer.transform();
            for (int y = 0; y < targetFrameH; y++) {
                int dstY = dstYOffset + y;
                for (int x = 0; x < targetFrameW; x++) {
                    int srcX = transform.sourceX(x, y, targetFrameW, targetFrameH) * layer.srcFrameW() / targetFrameW;
                    int srcY = transform.sourceY(x, y, targetFrameW, targetFrameH) * layer.srcFrameH() / targetFrameH;
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
                    pixel = PixelMath.applyOpacity(pixel, layer.opacity());
                    dst.setPixelRGBA(x, dstY, layer.blendMode().blend(dst.getPixelRGBA(x, dstY), pixel));
                }
            }
        }
    }

    public static void compositePbrInto(NativeImage dst, int dstYOffset, int targetFrameW, int targetFrameH, PbrType pbrType,
                                        List<PbrFrameLayerSpec> layers) {
        for (PbrFrameLayerSpec layer : layers) {
            ImageScaler.validate(layer.albedoFrameW(), layer.albedoFrameH(), targetFrameW, targetFrameH);
            layer.transform().validate(layer.albedoFrameW(), layer.albedoFrameH());
            if (layer.mask() != null) {
                ImageScaler.validate(layer.mask().getWidth(), layer.mask().getHeight(), targetFrameW, targetFrameH);
            }
            if (layer.pbrImage() != null) {
                ImageScaler.validateResample(layer.pbrFrameW(), layer.pbrFrameH(), targetFrameW, targetFrameH);
                layer.transform().validate(layer.pbrFrameW(), layer.pbrFrameH());
            }
        }

        dst.fillRect(0, dstYOffset, targetFrameW, targetFrameH, pbrType.neutralAbgr());
        for (PbrFrameLayerSpec layer : layers) {
            NativeImage mask = layer.mask();
            NativeImage pbr = layer.pbrImage();
            Transform transform = layer.transform();
            boolean turnNormals = pbrType == PbrType.NORMAL && !transform.isIdentity();
            for (int y = 0; y < targetFrameH; y++) {
                float weight = layer.interpWeight255() / 255.0F;
                int dstY = dstYOffset + y;
                for (int x = 0; x < targetFrameW; x++) {
                    int u = transform.sourceX(x, y, targetFrameW, targetFrameH);
                    int v = transform.sourceY(x, y, targetFrameW, targetFrameH);
                    int albedoX = u * layer.albedoFrameW() / targetFrameW;
                    int albedoY = v * layer.albedoFrameH() / targetFrameH;
                    int albedo = layer.albedo().getPixelRGBA(albedoX, layer.albedoYOffset() + albedoY);
                    if (layer.interpWeight255() > 0) {
                        albedo = PixelMath.lerpAbgr(albedo,
                                layer.albedo().getPixelRGBA(albedoX, layer.nextAlbedoYOffset() + albedoY), weight);
                    }
                    if (mask != null) {
                        int maskPixel = mask.getPixelRGBA(x * mask.getWidth() / targetFrameW, y * mask.getHeight() / targetFrameH);
                        albedo = PixelMath.applyMask(albedo, maskPixel, layer.invertMask());
                    }
                    albedo = PixelMath.applyOpacity(albedo, layer.opacity());
                    int source = pbrType.neutralAbgr();
                    if (pbr != null) {
                        int pbrX = u * layer.pbrFrameW() / targetFrameW;
                        int pbrY = v * layer.pbrFrameH() / targetFrameH;
                        source = pbr.getPixelRGBA(pbrX, layer.pbrYOffset() + pbrY);
                        if (layer.interpWeight255() > 0) {
                            source = pbrType.lerp(source, pbr.getPixelRGBA(pbrX, layer.nextPbrYOffset() + pbrY), weight);
                        }
                        if (turnNormals) {
                            source = PbrMath.transformNormal(source, transform.rotate(), transform.flipX(), transform.flipY());
                        }
                    }
                    dst.setPixelRGBA(x, dstY, pbrType.blend(dst.getPixelRGBA(x, dstY), source, PixelMath.alpha(albedo)));
                }
            }
        }
    }
}
