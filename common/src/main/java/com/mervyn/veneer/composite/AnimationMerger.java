package com.mervyn.veneer.composite;

import com.mervyn.veneer.VeneerConstants;
import com.mervyn.veneer.atlas.InterpolationMode;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AnimationMerger {
    static final int MAX_STEPS = 65536;

    private AnimationMerger() {
    }

    public static Result merge(List<Timeline> timelines, int maxFrames, InterpolationMode mode, int step) {
        List<List<Frame>> layers = timelines.stream().map(Timeline::frames).toList();
        if (layers.stream().allMatch(layer -> layer.size() == 1)) {
            List<LayerSample> still = layers.stream().map(layer -> LayerSample.of(layer.get(0).index())).toList();
            return new Result(List.of(still), List.of(), false);
        }

        long period = period(layers);
        List<Timeline> animated = timelines.stream().filter(timeline -> timeline.frames().size() > 1).toList();
        boolean blend = mode == InterpolationMode.AUTO && animated.stream().anyMatch(Timeline::interpolated);
        boolean runtime = blend && animated.stream().allMatch(Timeline::interpolated)
                && animated.stream().map(timeline -> cycle(timeline.frames())).distinct().count() == 1;
        if (blend && !runtime) {
            return bake(timelines, period, maxFrames, step);
        }
        return discrete(layers, period, maxFrames, runtime);
    }

    private static Result discrete(List<List<Frame>> layers, long period, int maxFrames, boolean outputInterpolated) {
        int[] cursors = new int[layers.size()];
        long[] ends = new long[layers.size()];
        for (int i = 0; i < ends.length; i++) {
            ends[i] = layers.get(i).size() > 1 ? layers.get(i).get(0).duration() : Long.MAX_VALUE;
        }

        Schedule schedule = new Schedule(maxFrames);
        long tick = 0;
        while (tick < period) {
            List<LayerSample> tuple = new ArrayList<>(layers.size());
            for (int i = 0; i < cursors.length; i++) {
                tuple.add(LayerSample.of(layers.get(i).get(cursors[i]).index()));
            }
            long next = Math.min(period, Arrays.stream(ends).min().getAsLong());
            if (!schedule.add(tuple, tick, (int) (next - tick))) {
                break;
            }
            for (int i = 0; i < ends.length; i++) {
                if (ends[i] == next) {
                    cursors[i] = (cursors[i] + 1) % layers.get(i).size();
                    ends[i] += layers.get(i).get(cursors[i]).duration();
                }
            }
            tick = next;
        }
        return schedule.result(outputInterpolated);
    }

    private static Result bake(List<Timeline> timelines, long period, int maxFrames, int step) {
        List<long[]> starts = timelines.stream().map(timeline -> starts(timeline.frames())).toList();
        Schedule schedule = new Schedule(maxFrames);
        for (long tick = 0; tick < period; tick += step) {
            List<LayerSample> tuple = new ArrayList<>(timelines.size());
            for (int i = 0; i < timelines.size(); i++) {
                tuple.add(sample(timelines.get(i), starts.get(i), tick));
            }
            if (!schedule.add(tuple, tick, (int) Math.min(step, period - tick))) {
                break;
            }
        }
        return schedule.result(false);
    }

    private static long[] starts(List<Frame> frames) {
        long[] starts = new long[frames.size() + 1];
        for (int i = 0; i < frames.size(); i++) {
            starts[i + 1] = starts[i] + frames.get(i).duration();
        }
        return starts;
    }

    private static LayerSample sample(Timeline timeline, long[] starts, long tick) {
        List<Frame> frames = timeline.frames();
        if (frames.size() == 1) {
            return LayerSample.of(frames.get(0).index());
        }
        long local = tick % starts[frames.size()];
        int found = Arrays.binarySearch(starts, local);
        int current = found >= 0 ? found : -found - 2;
        Frame frame = frames.get(current);
        if (!timeline.interpolated()) {
            return LayerSample.of(frame.index());
        }
        int weight = Math.round((float) (local - starts[current]) / frame.duration() * 255.0F);
        return new LayerSample(frame.index(), frames.get((current + 1) % frames.size()).index(), weight);
    }

    // shortest repeating run of durations: layers share transition ticks exactly when these match
    private static List<Integer> cycle(List<Frame> frames) {
        List<Integer> durations = frames.stream().map(Frame::duration).toList();
        int size = durations.size();
        for (int length = 1; length < size; length++) {
            if (size % length == 0 && durations.subList(length, size).equals(durations.subList(0, size - length))) {
                return durations.subList(0, length);
            }
        }
        return durations;
    }

    public static AnimationMetadataSection toMetadata(Result result, int frameWidth, int frameHeight) {
        List<AnimationFrame> frames = result.schedule().stream()
                .map(frame -> new AnimationFrame(frame.uniqueFrameIndex(), frame.durationTicks()))
                .toList();
        return new AnimationMetadataSection(frames, frameWidth, frameHeight, 1, result.outputInterpolated());
    }

    static long period(List<List<Frame>> layers) {
        long period = 1;
        for (List<Frame> layer : layers) {
            if (layer.size() > 1) {
                long length = layer.stream().mapToLong(Frame::duration).sum();
                long factor = period / gcd(period, length);
                if (factor > Long.MAX_VALUE / length) {
                    VeneerConstants.LOGGER.warn("Merged animation period overflows, it will be truncated");
                    return Long.MAX_VALUE;
                }
                period = factor * length;
            }
        }
        return period;
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    public record Frame(int index, int duration) {
    }

    public record Timeline(List<Frame> frames, boolean interpolated) {
    }

    public record LayerSample(int frameIndex, int nextFrameIndex, int weight255) {
        public static LayerSample of(int frameIndex) {
            return new LayerSample(frameIndex, frameIndex, 0);
        }
    }

    public record MergedFrame(int uniqueFrameIndex, int durationTicks) {
    }

    public record Result(List<List<LayerSample>> uniqueFrames, List<MergedFrame> schedule, boolean outputInterpolated) {
        public boolean isAnimated() {
            return !schedule.isEmpty();
        }

        public Result stepped() {
            return new Result(uniqueFrames, schedule, false);
        }
    }

    private static final class Schedule {
        private final Map<List<LayerSample>, Integer> unique = new LinkedHashMap<>();
        private final List<MergedFrame> steps = new ArrayList<>();
        private final int maxFrames;

        Schedule(int maxFrames) {
            this.maxFrames = maxFrames;
        }

        boolean add(List<LayerSample> tuple, long tick, int duration) {
            if (steps.size() >= MAX_STEPS) {
                VeneerConstants.LOGGER.warn("Merged animation needs more than {} steps, truncating at tick {}", MAX_STEPS, tick);
                return false;
            }
            Integer id = unique.get(tuple);
            if (id == null) {
                if (unique.size() >= maxFrames) {
                    VeneerConstants.LOGGER.warn("Merged animation needs more than {} frames, truncating at tick {}", maxFrames, tick);
                    return false;
                }
                id = unique.size();
                unique.put(tuple, id);
            }
            steps.add(new MergedFrame(id, duration));
            return true;
        }

        Result result(boolean outputInterpolated) {
            return new Result(List.copyOf(unique.keySet()), List.copyOf(steps), outputInterpolated);
        }
    }
}
