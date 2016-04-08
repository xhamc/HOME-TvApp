// Copyright (C) 2013 Sony Mobile Communications AB.
// All rights, including trade secret rights, reserved.

package com.sony.sel.util;

import android.graphics.Point;
import android.graphics.Rect;

/**
 * Class for scaling utilities that help with geometry transformations, scaling etc.
 */
public class ScalingHelper {

    /**
     * Utility for scaling an integer by a float value.
     * <p/>
     * Prevents a lot of explicit casts in the caller's source code
     *
     * @param value   Value to scale.
     * @param scaling Scaling factor (multiplied by value).
     * @return The scaled value as an int.
     */
    static public int scale(int value, float scaling) {
        return (int) ((float) value * scaling);
    }

    /**
     * Utility for scaling a Rect by arbitrary H and V scaling values.
     * <p/>
     * Used to normalize cropping rects.
     *
     * @param r        Rect to scale.
     * @param hScaling Horizontal scaling factor.
     * @param vScaling Vertical scaling factor.
     * @return A new Rect that represents the scaled value.
     */
    static public Rect scale(final Rect r, float hScaling, float vScaling) {
        return new Rect(scale(r.left, hScaling), scale(r.top, vScaling), scale(r.right, hScaling),
                scale(r.bottom, vScaling));
    }

    /**
     * Utility for scaling a Point by arbitrary H and V scaling values.
     * <p/>
     * Used to normalize Points used for width/height.
     *
     * @param p        Point to scale.
     * @param hScaling Horizontal scaling factor.
     * @param vScaling Vertical scaling factor.
     * @return A new Point that represents the scaled value.
     */
    static public Point scale(final Point p, float hScaling, float vScaling) {
        return new Point(scale(p.x, hScaling), scale(p.y, vScaling));
    }

    /**
     * Return a float scaling factor created by dividing the numerator by the denominator.
     *
     * @param numerator   Integer numerator for scaling factor.
     * @param denominator Integer denominator for scaling factor.
     * @return The scaling factor as a float.
     */
    static public float getScale(int numerator, int denominator) {
        return (float) numerator / (float) denominator;
    }

    /**
     * Return a width & height for the source that will fit within
     * the destination width & height maintaining the same aspect ratio.
     *
     * @param srcWidthHeight  Source size (x = width, y = height)
     * @param destWidthHeight Destination size (x = width, y = height)
     * @return Size that the source should be scaled to.
     */
    static public Point fit(final Point srcWidthHeight, final Point destWidthHeight) {
        float scaling =
                Math.min(getScale(destWidthHeight.x, srcWidthHeight.x),
                        getScale(destWidthHeight.y, srcWidthHeight.y));
        return scale(srcWidthHeight, scaling, scaling);
    }

    /**
     * Return a width & height for the source that will "center-crop" within
     * the destination width & height maintaining the same aspect ratio.
     *
     * @param srcWidthHeight  Source size (x = width, y = height)
     * @param destWidthHeight Destination size (x = width, y = height)
     * @return Size that the source should be scaled to.
     */
    static public Point centerCrop(final Point srcWidthHeight, final Point destWidthHeight) {
        float scalingX = getScale(destWidthHeight.x, srcWidthHeight.x);
        float scalingY = getScale(destWidthHeight.y, srcWidthHeight.y);
        return scale(srcWidthHeight, scalingX, scalingY);

    }

}
