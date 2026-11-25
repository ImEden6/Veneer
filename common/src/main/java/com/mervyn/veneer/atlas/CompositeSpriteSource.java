package com.mervyn.veneer.atlas;

import com.mervyn.veneer.VeneerConstants;
import com.mervyn.veneer.composite.AnimationMerger;
import com.mervyn.veneer.composite.FrameLayerSpec;
import com.mervyn.veneer.composite.ImageCompositor;
import com.mervyn.veneer.composite.PbrFrameLayerSpec;
import com.mervyn.veneer.composite.PbrType;
import com.mervyn.veneer.pbr.VeneerPbrRegistry;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CompositeSpriteSource(ResourceLocation sprite, List<LayerDefinition> layers, int metadataSource,
                                    int maxFrames, boolean pbr, InterpolationMode interpolation,
                                    int interpolationStep) implements SpriteSource {
    public static final MapCodec<CompositeSpriteSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("sprite").forGetter(CompositeSpriteSource::sprite),
            ExtraCodecs.nonEmptyList(LayerDefinition.CODEC.listOf()).fieldOf("layers").forGetter(CompositeSpriteSource::layers),
            Codec.INT.optionalFieldOf("metadata_source", 0).forGetter(CompositeSpriteSource::metadataSource),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_frames", 64).forGetter(CompositeSpriteSource::maxFrames),
            Codec.BOOL.optionalFieldOf("pbr", true).forGetter(CompositeSpriteSource::pbr),
            InterpolationMode.CODEC.optionalFieldOf("interpolation", InterpolationMode.AUTO)
                    .forGetter(CompositeSpriteSource::interpolation),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("interpolation_step", 1).forGetter(CompositeSpriteSource::interpolationStep)
    ).apply(instance, CompositeSpriteSource::new));

    public static SpriteSourceType TYPE;

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        List<Input> inputs = inputs(resourceManager);
        for (PbrType type : PbrType.values()) {
            ResourceLocation path = TEXTURE_ID_CONVERTER.idToFile(withSuffix(sprite, type.suffix()));
            if (pbr && VeneerPbrRegistry.resolve(() -> companions(resourceManager, inputs, type)).stream().anyMatch(Optional::isPresent)) {
                VeneerPbrRegistry.register(path, new VeneerPbrRegistry.PbrRecipe(this, type));
            } else {
                VeneerPbrRegistry.remove(path);
            }
        }
        output.add(sprite, loader -> stitch(inputs));
    }

    private List<Input> inputs(ResourceManager resourceManager) {
        return layers.stream()
                .map(layer -> new Input(layer,
                        resourceManager.getResource(TEXTURE_ID_CONVERTER.idToFile(layer.texture())),
                        layer.mask().flatMap(mask -> resourceManager.getResource(TEXTURE_ID_CONVERTER.idToFile(mask)))))
                .toList();
    }

    private static List<Optional<Resource>> companions(ResourceManager resourceManager, List<Input> inputs, PbrType type) {
        return inputs.stream()
                .map(input -> input.texture().flatMap(texture -> resourceManager.getResource(
                        TEXTURE_ID_CONVERTER.idToFile(withSuffix(input.layer().texture(), type.suffix())))))
                .toList();
    }

    private static ResourceLocation withSuffix(ResourceLocation id, String suffix) {
        return id.withPath(id.getPath() + suffix);
    }

    @Nullable
    private SpriteContents stitch(List<Input> inputs) {
        List<NativeImage> opened = new ArrayList<>();
        NativeImage result = null;
        try {
            List<Source> sources = loadAll(inputs, opened);
            if (sources == null) {
                return null;
            }
            int width = 0;
            int height = 0;
            for (Source source : sources) {
                width = Math.max(width, source.width());
                height = Math.max(height, source.height());
            }

            AnimationMerger.Result merged = merge(sources);
            int frameCount = merged.uniqueFrames().size();
            result = new NativeImage(width, height * frameCount, true);
            for (int frame = 0; frame < frameCount; frame++) {
                List<AnimationMerger.LayerSample> samples = merged.uniqueFrames().get(frame);
                List<FrameLayerSpec> specs = new ArrayList<>();
                for (int i = 0; i < sources.size(); i++) {
                    Source source = sources.get(i);
                    LayerDefinition layer = source.layer();
                    AnimationMerger.LayerSample sample = samples.get(i);
                    specs.add(new FrameLayerSpec(source.image(), sample.frameIndex() * source.size().height(),
                            source.size().width(), source.size().height(),
                            layer.blend(), layer.tint(), source.mask(), layer.invertMask(),
                            sample.nextFrameIndex() * source.size().height(), sample.weight255()));
                }
                ImageCompositor.compositeInto(result, frame * height, width, height, specs);
            }

            Source base = sources.stream()
                    .filter(source -> source.index() == metadataSource)
                    .findFirst()
                    .orElse(sources.get(0));
            AnimationMetadataSection animation = merged.isAnimated() ? AnimationMerger.toMetadata(merged, width, height) : null;
            SpriteContents contents = new SpriteContents(sprite, new FrameSize(width, height), result,
                    withAnimation(base.metadata(), animation));
            result = null;
            VeneerConstants.LOGGER.info("Composited {} at {}x{} with {} frame(s)", sprite, width, height, frameCount);
            return contents;
        } catch (Exception e) {
            VeneerConstants.LOGGER.error("Failed to composite sprite {}", sprite, e);
            return null;
        } finally {
            if (result != null) {
                result.close();
            }
            opened.forEach(NativeImage::close);
        }
    }

    @Nullable
    public PbrImage compositePbr(ResourceManager resourceManager, PbrType type) {
        ResourceLocation name = withSuffix(sprite, type.suffix());
        List<Input> inputs = inputs(resourceManager);
        List<Optional<Resource>> companions = companions(resourceManager, inputs, type);
        List<NativeImage> opened = new ArrayList<>();
        NativeImage result = null;
        try {
            List<Source> sources = loadAll(inputs, opened);
            if (sources == null) {
                return null;
            }
            List<Companion> loaded = new ArrayList<>();
            int width = 0;
            int height = 0;
            for (int i = 0; i < sources.size(); i++) {
                Source source = sources.get(i);
                width = Math.max(width, source.width());
                height = Math.max(height, source.height());
                Companion companion = null;
                Optional<Resource> resource = companions.get(source.index());
                if (resource.isPresent()) {
                    companion = loadCompanion(resource.get(), source, opened);
                    if (companion == null) {
                        VeneerConstants.LOGGER.error("Sprite {}: {} does not divide into frames of its size",
                                name, withSuffix(source.layer().texture(), type.suffix()));
                        return null;
                    }
                    width = Math.max(width, companion.size().width());
                    height = Math.max(height, companion.size().height());
                }
                loaded.add(companion);
            }

            AnimationMerger.Result merged = merge(sources);
            int frameCount = merged.uniqueFrames().size();
            result = new NativeImage(width, height * frameCount, true);
            for (int frame = 0; frame < frameCount; frame++) {
                List<AnimationMerger.LayerSample> samples = merged.uniqueFrames().get(frame);
                List<PbrFrameLayerSpec> specs = new ArrayList<>();
                for (int i = 0; i < sources.size(); i++) {
                    Source source = sources.get(i);
                    Companion companion = loaded.get(i);
                    AnimationMerger.LayerSample sample = samples.get(i);
                    specs.add(new PbrFrameLayerSpec(source.image(), sample.frameIndex() * source.size().height(),
                            source.size().width(), source.size().height(),
                            source.mask(), source.layer().invertMask(),
                            companion == null ? null : companion.image(),
                            companion == null ? 0 : companion.yOffset(sample.frameIndex()),
                            companion == null ? 0 : companion.size().width(),
                            companion == null ? 0 : companion.size().height(),
                            sample.nextFrameIndex() * source.size().height(),
                            companion == null ? 0 : companion.yOffset(sample.nextFrameIndex()),
                            sample.weight255()));
                }
                ImageCompositor.compositePbrInto(result, frame * height, width, height, type, specs);
            }

            // vanilla's runtime lerp mixes G and B, which are discrete material flags in LabPBR specular
            AnimationMerger.Result output = type == PbrType.SPECULAR ? merged.stepped() : merged;
            AnimationMetadataSection animation = output.isAnimated() ? AnimationMerger.toMetadata(output, width, height) : null;
            PbrImage composite = new PbrImage(result, animation);
            result = null;
            VeneerConstants.LOGGER.info("Composited {} at {}x{} with {} frame(s)", name, width, height, frameCount);
            return composite;
        } catch (Exception e) {
            VeneerConstants.LOGGER.error("Failed to composite sprite {}", name, e);
            return null;
        } finally {
            if (result != null) {
                result.close();
            }
            opened.forEach(NativeImage::close);
        }
    }

    private AnimationMerger.Result merge(List<Source> sources) {
        return AnimationMerger.merge(sources.stream().map(Source::timeline).toList(), maxFrames, interpolation, interpolationStep);
    }

    @Nullable
    private static Companion loadCompanion(Resource resource, Source albedo, List<NativeImage> opened) throws IOException {
        AnimationMetadataSection animation = resource.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElse(null);
        NativeImage image = read(resource, opened);
        FrameSize size;
        if (animation != null) {
            size = animation.calculateFrameSize(image.getWidth(), image.getHeight());
        } else if (albedo.timeline().frames().size() > 1) {
            int frameHeight = image.getWidth() * albedo.size().height() / albedo.size().width();
            size = new FrameSize(image.getWidth(), frameHeight);
        } else {
            size = new FrameSize(image.getWidth(), image.getHeight());
        }
        boolean strip = size.width() == image.getWidth() && size.height() > 0 && image.getHeight() % size.height() == 0
                && image.getHeight() >= size.height();
        return strip ? new Companion(image, size) : null;
    }

    @Nullable
    private List<Source> loadAll(List<Input> inputs, List<NativeImage> opened) throws IOException {
        List<Source> sources = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            Input input = inputs.get(i);
            if (input.texture().isEmpty() && input.layer().optional()) {
                VeneerConstants.LOGGER.debug("Sprite {}: skipping optional layer {}", sprite, input.layer().texture());
                continue;
            }
            Source source = load(i, input, opened);
            if (source == null) {
                return null;
            }
            sources.add(source);
        }
        if (sources.isEmpty()) {
            VeneerConstants.LOGGER.error("Sprite {}: every layer was skipped", sprite);
            return null;
        }
        return sources;
    }

    @Nullable
    private Source load(int index, Input input, List<NativeImage> opened) throws IOException {
        LayerDefinition layer = input.layer();
        if (input.texture().isEmpty()) {
            VeneerConstants.LOGGER.error("Sprite {}: missing texture {}", sprite, layer.texture());
            return null;
        }
        if (layer.mask().isPresent() && input.mask().isEmpty()) {
            VeneerConstants.LOGGER.error("Sprite {}: missing mask {}", sprite, layer.mask().get());
            return null;
        }

        Resource texture = input.texture().get();
        ResourceMetadata metadata = texture.metadata();
        NativeImage image = read(texture, opened);
        NativeImage mask = input.mask().isPresent() ? read(input.mask().get(), opened) : null;

        AnimationMetadataSection animation = metadata.getSection(AnimationMetadataSection.SERIALIZER).orElse(null);
        if (animation == null) {
            return new Source(index, layer, image, mask, metadata, new FrameSize(image.getWidth(), image.getHeight()),
                    new AnimationMerger.Timeline(List.of(new AnimationMerger.Frame(0, 1)), false));
        }

        FrameSize size = animation.calculateFrameSize(image.getWidth(), image.getHeight());
        if (size.width() != image.getWidth() || image.getHeight() % size.height() != 0) {
            VeneerConstants.LOGGER.error("Sprite {}: {} is {}x{}, not a vertical strip of {}x{} frames",
                    sprite, layer.texture(), image.getWidth(), image.getHeight(), size.width(), size.height());
            return null;
        }
        List<AnimationMerger.Frame> frames = frames(animation, image.getHeight() / size.height(), layer.texture());
        return new Source(index, layer, image, mask, metadata, size,
                new AnimationMerger.Timeline(frames, animation.isInterpolatedFrames() && frames.size() > 1));
    }

    private List<AnimationMerger.Frame> frames(AnimationMetadataSection animation, int frameCount, ResourceLocation texture) {
        List<AnimationMerger.Frame> listed = new ArrayList<>();
        animation.forEachFrame((index, time) -> listed.add(new AnimationMerger.Frame(index, time)));
        if (listed.isEmpty()) {
            for (int i = 0; i < frameCount; i++) {
                listed.add(new AnimationMerger.Frame(i, animation.getDefaultFrameTime()));
            }
        }

        List<AnimationMerger.Frame> valid = listed.stream()
                .filter(frame -> frame.index() >= 0 && frame.index() < frameCount && frame.duration() > 0)
                .toList();
        if (valid.size() != listed.size()) {
            VeneerConstants.LOGGER.warn("Sprite {}: skipping invalid animation frames in {}", sprite, texture);
        }
        return valid.isEmpty() ? List.of(new AnimationMerger.Frame(0, 1)) : valid;
    }

    private static NativeImage read(Resource resource, List<NativeImage> opened) throws IOException {
        try (InputStream stream = resource.open()) {
            NativeImage image = NativeImage.read(stream);
            opened.add(image);
            return image;
        }
    }

    private static ResourceMetadata withAnimation(ResourceMetadata base, @Nullable AnimationMetadataSection animation) {
        return new ResourceMetadata() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> getSection(MetadataSectionSerializer<T> serializer) {
                if (serializer == AnimationMetadataSection.SERIALIZER) {
                    return Optional.ofNullable((T) animation);
                }
                return base.getSection(serializer);
            }
        };
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }

    public record PbrImage(NativeImage image, @Nullable AnimationMetadataSection animation) {
    }

    private record Input(LayerDefinition layer, Optional<Resource> texture, Optional<Resource> mask) {
    }

    private record Source(int index, LayerDefinition layer, NativeImage image, @Nullable NativeImage mask,
                          ResourceMetadata metadata, FrameSize size, AnimationMerger.Timeline timeline) {
        int width() {
            return mask == null ? size.width() : Math.max(size.width(), mask.getWidth());
        }

        int height() {
            return mask == null ? size.height() : Math.max(size.height(), mask.getHeight());
        }
    }

    private record Companion(NativeImage image, FrameSize size) {
        int yOffset(int frameIndex) {
            return frameIndex % (image.getHeight() / size.height()) * size.height();
        }
    }
}
