package com.mervyn.veneer.composite;

public record Transform(int rotate, boolean flipX, boolean flipY) {
    public static final Transform NONE = new Transform(0, false, false);

    // flips apply to the layer before the clockwise rotation, so sampling undoes the rotation first
    public int sourceX(int x, int y, int width, int height) {
        int unrotated = switch (rotate) {
            case 90 -> y;
            case 180 -> width - 1 - x;
            case 270 -> width - 1 - y;
            default -> x;
        };
        return flipX ? width - 1 - unrotated : unrotated;
    }

    public int sourceY(int x, int y, int width, int height) {
        int unrotated = switch (rotate) {
            case 90 -> height - 1 - x;
            case 180 -> height - 1 - y;
            case 270 -> x;
            default -> y;
        };
        return flipY ? height - 1 - unrotated : unrotated;
    }

    public boolean isIdentity() {
        return rotate == 0 && !flipX && !flipY;
    }

    void validate(int frameWidth, int frameHeight) {
        if ((rotate == 90 || rotate == 270) && frameWidth != frameHeight) {
            throw new IllegalArgumentException(
                    "Cannot rotate a " + frameWidth + "x" + frameHeight + " frame by " + rotate + ": it must be square");
        }
    }
}
