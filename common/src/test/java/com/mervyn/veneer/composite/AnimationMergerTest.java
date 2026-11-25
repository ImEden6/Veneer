package com.mervyn.veneer.composite;

import com.mervyn.veneer.atlas.InterpolationMode;
import com.mervyn.veneer.composite.AnimationMerger.Frame;
import com.mervyn.veneer.composite.AnimationMerger.LayerSample;
import com.mervyn.veneer.composite.AnimationMerger.MergedFrame;
import com.mervyn.veneer.composite.AnimationMerger.Result;
import com.mervyn.veneer.composite.AnimationMerger.Timeline;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationMergerTest {
    private static final List<Frame> STILL = List.of(new Frame(0, 1));

    private static List<Frame> strip(int count, int duration) {
        return IntStream.range(0, count).mapToObj(i -> new Frame(i, duration)).toList();
    }

    private static Result discrete(List<List<Frame>> layers, int maxFrames) {
        return AnimationMerger.merge(layers.stream().map(frames -> new Timeline(frames, false)).toList(),
                maxFrames, InterpolationMode.AUTO, 1);
    }

    private static List<List<Integer>> indices(Result result) {
        return result.uniqueFrames().stream()
                .map(tuple -> tuple.stream().map(LayerSample::frameIndex).toList())
                .toList();
    }

    private static Timeline smooth(List<Frame> frames) {
        return new Timeline(frames, true);
    }

    private static Result auto(int step, Timeline... layers) {
        return AnimationMerger.merge(List.of(layers), 64, InterpolationMode.AUTO, step);
    }

    @Test
    void staticLayersStayStatic() {
        Result result = discrete(List.of(STILL, STILL), 64);
        assertFalse(result.isAnimated());
        assertEquals(List.of(List.of(0, 0)), indices(result));
    }

    @Test
    void staticUnderAnimationKeepsSchedule() {
        Result result = discrete(List.of(STILL, strip(3, 2)), 64);
        assertEquals(List.of(List.of(0, 0), List.of(0, 1), List.of(0, 2)), indices(result));
        assertEquals(List.of(new MergedFrame(0, 2), new MergedFrame(1, 2), new MergedFrame(2, 2)), result.schedule());
    }

    @Test
    void twoAndThreeTickLayersUseLcm() {
        Result result = discrete(List.of(
                List.of(new Frame(0, 1), new Frame(1, 1)),
                List.of(new Frame(0, 2), new Frame(1, 1))), 64);
        assertEquals(List.of(List.of(0, 0), List.of(1, 0), List.of(0, 1), List.of(1, 1)), indices(result));
        assertEquals(List.of(new MergedFrame(0, 1), new MergedFrame(1, 1), new MergedFrame(2, 1),
                new MergedFrame(1, 1), new MergedFrame(0, 1), new MergedFrame(3, 1)), result.schedule());
    }

    @Test
    void boundariesSplitIntervals() {
        Result result = discrete(List.of(strip(2, 2), strip(2, 3)), 64);
        assertEquals(List.of(List.of(0, 0), List.of(1, 0), List.of(1, 1), List.of(0, 1)), indices(result));
        assertEquals(List.of(new MergedFrame(0, 2), new MergedFrame(1, 1), new MergedFrame(2, 1), new MergedFrame(3, 2),
                new MergedFrame(1, 2), new MergedFrame(0, 1), new MergedFrame(3, 1), new MergedFrame(2, 2)), result.schedule());
    }

    @Test
    void identicalTuplesShareAFrame() {
        List<Frame> bounce = List.of(new Frame(0, 2), new Frame(1, 3), new Frame(0, 4), new Frame(1, 1));
        Result result = discrete(List.of(bounce), 64);
        assertEquals(List.of(List.of(0), List.of(1)), indices(result));
        assertEquals(List.of(new MergedFrame(0, 2), new MergedFrame(1, 3), new MergedFrame(0, 4), new MergedFrame(1, 1)),
                result.schedule());
    }

    @Test
    void frameCapTruncates() {
        Result result = discrete(List.of(strip(97, 7), strip(101, 11), strip(103, 13)), 16);
        assertEquals(16, result.uniqueFrames().size());
        assertTrue(result.schedule().stream().allMatch(f -> f.uniqueFrameIndex() < 16 && f.durationTicks() > 0));
    }

    @Test
    void longPeriodWithFewTuplesIsBounded() {
        Result result = discrete(List.of(strip(2, 1), strip(2, 1000003), strip(2, 1000033)), 64);
        assertEquals(AnimationMerger.MAX_STEPS, result.schedule().size());
    }

    @Test
    void metadataListsSchedule() {
        Result result = discrete(List.of(strip(2, 5)), 64);
        List<Integer> seen = new ArrayList<>();
        AnimationMerger.toMetadata(result, 16, 16).forEachFrame((index, time) -> {
            seen.add(index);
            seen.add(time);
        });
        assertEquals(List.of(0, 5, 1, 5), seen);
    }

    @Test
    void interpolatedOverStaticStaysRuntime() {
        Result result = auto(1, smooth(strip(3, 4)), new Timeline(STILL, false));
        assertTrue(result.outputInterpolated());
        assertEquals(3, result.uniqueFrames().size());
        assertEquals(3, result.schedule().size());
    }

    @Test
    void alignedInterpolatedLayersStayRuntime() {
        Result result = auto(1, smooth(strip(2, 4)), smooth(strip(4, 4)));
        assertTrue(result.outputInterpolated());
        assertEquals(4, result.uniqueFrames().size());
        assertTrue(result.uniqueFrames().stream().flatMap(List::stream).allMatch(sample -> sample.weight255() == 0));
    }

    @Test
    void desyncedInterpolatedLayersAreBaked() {
        Result result = auto(1, smooth(strip(2, 4)), smooth(strip(2, 2)));
        assertFalse(result.outputInterpolated());
        assertEquals(8, result.schedule().size());
    }

    @Test
    void mixedLayersAreBaked() {
        Result result = auto(1, smooth(strip(2, 4)), new Timeline(strip(2, 2), false));
        assertFalse(result.outputInterpolated());
        assertEquals(8, result.uniqueFrames().size());
        assertEquals(List.of(0, 64, 128, 191, 0, 64, 128, 191),
                result.uniqueFrames().stream().map(tuple -> tuple.get(0).weight255()).toList());
        assertEquals(new LayerSample(1, 0, 191), result.uniqueFrames().get(7).get(0));
        assertEquals(LayerSample.of(1), result.uniqueFrames().get(7).get(1));
    }

    @Test
    void largerStepBakesFewerFrames() {
        Result result = auto(2, smooth(strip(2, 4)), new Timeline(strip(2, 2), false));
        assertEquals(4, result.uniqueFrames().size());
        assertTrue(result.schedule().stream().allMatch(frame -> frame.durationTicks() == 2));
    }

    @Test
    void lastBakedStepEndsWithThePeriod() {
        Result result = auto(3, smooth(strip(2, 4)), new Timeline(strip(2, 2), false));
        assertEquals(List.of(3, 3, 2), result.schedule().stream().map(MergedFrame::durationTicks).toList());
    }

    @Test
    void bakingRespectsBothCaps() {
        Timeline other = new Timeline(strip(2, 3), false);
        assertEquals(64, auto(1, smooth(strip(2, 1000)), other).uniqueFrames().size());
        Result steps = AnimationMerger.merge(List.of(smooth(strip(2, 1_000_000)), other),
                Integer.MAX_VALUE, InterpolationMode.AUTO, 1);
        assertEquals(AnimationMerger.MAX_STEPS, steps.schedule().size());
    }

    @Test
    void steppedKeepsScheduleWithoutInterpolation() {
        Result result = auto(1, smooth(strip(2, 4)));
        Result stepped = result.stepped();
        assertFalse(stepped.outputInterpolated());
        assertEquals(result.uniqueFrames(), stepped.uniqueFrames());
        assertEquals(result.schedule(), stepped.schedule());
        assertFalse(AnimationMerger.toMetadata(stepped, 16, 16).isInterpolatedFrames());
    }

    @Test
    void ignoreModeKeepsFramesDiscrete() {
        Result result = AnimationMerger.merge(List.of(smooth(strip(2, 4))), 64, InterpolationMode.IGNORE, 1);
        assertFalse(result.outputInterpolated());
        assertEquals(2, result.schedule().size());
    }
}
