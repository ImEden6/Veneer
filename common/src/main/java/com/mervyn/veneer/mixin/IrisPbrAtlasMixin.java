package com.mervyn.veneer.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

// iris ticks its pbr atlas on its own clock and never calls its syncAnimation, so _n/_s drift off the albedo frame
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pbr.texture.PBRAtlasTexture")
public class IrisPbrAtlasMixin {
    @Shadow
    @Final
    protected TextureAtlas atlasTexture;

    @Shadow
    protected Map<Object, TextureAtlasSprite> texturesByName;

    @Shadow
    private List<SpriteContents.AnimationState> animatedTexturesStates;

    private static final MethodHandle ANIMATED_TEXTURE;
    private static final MethodHandle ANIMATION_INFO;
    private static final MethodHandle FRAMES;
    private static final MethodHandle TIME;

    static {
        try {
            Class<?> animated = Class.forName("net.minecraft.client.renderer.texture.SpriteContents$AnimatedTexture");
            Class<?> frameInfo = Class.forName("net.minecraft.client.renderer.texture.SpriteContents$FrameInfo");
            ANIMATED_TEXTURE = getter(SpriteContents.class, "animatedTexture", animated);
            ANIMATION_INFO = getter(SpriteContents.AnimationState.class, "animationInfo", animated);
            FRAMES = getter(animated, "frames", List.class);
            TIME = getter(frameInfo, "time", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle getter(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException {
        return MethodHandles.privateLookupIn(owner, MethodHandles.lookup()).findGetter(owner, name, type).asType(
                MethodType.methodType(type.isPrimitive() ? type : Object.class, Object.class));
    }

    @Inject(method = "cycleAnimationFrames", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;", ordinal = 0))
    private void veneer$followAlbedo(CallbackInfo ci) {
        Map<Object, SpriteContents.AnimationState> albedo = byAnimation(((TextureAtlasAccessor) atlasTexture).veneer$getAnimatedTexturesStates());
        Map<Object, SpriteContents.AnimationState> pbr = byAnimation(animatedTexturesStates);
        for (TextureAtlasSprite sprite : texturesByName.values()) {
            SpriteContents.AnimationState state = pbr.get(invoke(ANIMATED_TEXTURE, sprite.contents()));
            TextureAtlasSprite base = ((IrisPbrSpriteAccessor) sprite).veneer$getBaseSprite();
            SpriteContents.AnimationState baseState = base == null ? null
                    : albedo.get(invoke(ANIMATED_TEXTURE, base.contents()));
            if (state != null && baseState != null) {
                follow((AnimationStateAccessor) (Object) baseState, (AnimationStateAccessor) (Object) state);
            }
        }
    }

    private static Map<Object, SpriteContents.AnimationState> byAnimation(List<SpriteContents.AnimationState> states) {
        Map<Object, SpriteContents.AnimationState> map = new IdentityHashMap<>();
        for (SpriteContents.AnimationState state : states) {
            map.put(invoke(ANIMATION_INFO, state), state);
        }
        return map;
    }

    private static void follow(AnimationStateAccessor base, AnimationStateAccessor state) {
        List<?> baseFrames = frames(base);
        int elapsed = base.veneer$getSubFrame();
        for (int i = 0; i < base.veneer$getFrame(); i++) {
            elapsed += time(baseFrames.get(i));
        }

        List<?> frames = frames(state);
        int total = 0;
        for (Object frame : frames) {
            total += time(frame);
        }
        elapsed %= total;
        int frame = 0;
        while (elapsed >= time(frames.get(frame))) {
            elapsed -= time(frames.get(frame));
            frame++;
        }

        if (frame != state.veneer$getFrame()) {
            state.veneer$setDirty(true);
        }
        state.veneer$setFrame(frame);
        state.veneer$setSubFrame(elapsed);
    }

    private static List<?> frames(AnimationStateAccessor state) {
        return (List<?>) invoke(FRAMES, invoke(ANIMATION_INFO, state));
    }

    private static int time(Object frame) {
        try {
            return (int) TIME.invokeExact(frame);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object invoke(MethodHandle getter, Object target) {
        try {
            return (Object) getter.invokeExact(target);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
