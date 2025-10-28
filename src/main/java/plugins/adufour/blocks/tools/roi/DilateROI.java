/*
 * Copyright (c) 2010-2025. Institut Pasteur.
 *
 * This file is part of Icy.
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <https://www.gnu.org/licenses/>.
 */

package plugins.adufour.blocks.tools.roi;

import org.bioimageanalysis.extension.kernel.roi.roi2d.ROI2DArea;
import org.bioimageanalysis.extension.kernel.roi.roi3d.ROI3DArea;
import org.bioimageanalysis.icy.common.geom.point.Point3D;
import org.bioimageanalysis.icy.model.roi.ROI;
import org.bioimageanalysis.icy.model.roi.ROI2D;
import org.bioimageanalysis.icy.model.roi.ROI3D;
import org.bioimageanalysis.icy.model.roi.mask.BooleanMask2D;
import org.bioimageanalysis.icy.model.roi.mask.BooleanMask3D;
import org.bioimageanalysis.icy.system.logging.IcyLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.util.VarException;

import java.awt.*;
import java.util.ArrayList;

/**
 * @author Joel Rogers
 * @author Alexandre Dufour
 */
public class DilateROI extends MorphROI {
    public DilateROI() {
        roiOUT = new VarROIArray("Dilated ROI");
    }

    @Override
    public void run() {
        try {
            switch (unit.getValue()) {
                case PIXELS:
                    roiOUT.setValue(dilateROI(roiIN.getValue(), x.getValue(), y.getValue(), z.getValue()));
                    break;
                case PERCENTAGE:
                    roiOUT.setValue(dilateROIByPercentage(roiIN.getValue(), x.getValue(), y.getValue(), z.getValue()));
                    break;
                default:
                    throw new VarException(unit, "Unsupported unit");
            }
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Perform a morphological dilation on the specified set of ROI by the given amount in each
     * dimension
     *
     * @param inputRoi The ROI to dilate
     * @param xRadius  the radius (in pixels) along X
     * @param yRadius  the radius (in pixels) along X
     * @param zRadius  the radius (in pixels) along Z (not used if <code>roi</code> is 2D)
     * @return a new set of dilated ROI with type "area"
     * @throws InterruptedException if the user interrupts the process
     */
    public static ROI @NotNull [] dilateROI(final ROI @NotNull [] inputRoi, final int xRadius, final int yRadius, final int zRadius) throws InterruptedException {
        final ArrayList<ROI> out = new ArrayList<>(inputRoi.length);

        for (final ROI roi : inputRoi) {
            if (Thread.currentThread().isInterrupted())
                break;

            final ROI dilated = dilateROI(roi, xRadius, yRadius, zRadius);
            if (dilated != null)
                out.add(dilated);
        }

        return out.toArray(new ROI[0]);
    }

    /**
     * Perform a morphological dilation on the specified set of ROI by the given percentage in each
     * dimension
     *
     * @param inputRoi the ROI to dilate
     * @param xPct     the percentage (from 0 to 100) to dilate along X
     * @param yPct     the percentage (from 0 to 100) to dilate along Y
     * @param zPct     the percentage (from 0 to 100) to dilate along Z (not used in 2D)
     * @return a new set of dilated ROI with type "area"
     * @throws InterruptedException if the user interrupts the process
     */
    public static ROI @NotNull [] dilateROIByPercentage(final ROI @NotNull [] inputRoi, final int xPct, final int yPct, final int zPct) throws InterruptedException {
        final ArrayList<ROI> out = new ArrayList<>(inputRoi.length);

        for (final ROI roi : inputRoi) {
            if (Thread.currentThread().isInterrupted())
                break;

            final ROI dilated = dilateROIByPercentage(roi, xPct, yPct, zPct);
            if (dilated != null)
                out.add(dilated);
        }

        return out.toArray(new ROI[0]);
    }

    /**
     * Perform a morphological dilation on the specified ROI by the given percentage in each
     * dimension
     *
     * @param roi  the ROI to dilate
     * @param xPct the percentage (from 0 to 100) to dilate along X
     * @param yPct the percentage (from 0 to 100) to dilate along Y
     * @param zPct the percentage (from 0 to 100) to dilate along Z (not used in 2D)
     * @return a new, dilated ROI of type "area"
     * @throws InterruptedException if the user interrupts the process
     */
    public static ROI dilateROIByPercentage(final ROI roi, final int xPct, final int yPct, final int zPct) throws InterruptedException {
        final int xRadius = percentageToRadiusX(roi, xPct);
        final int yRadius = percentageToRadiusY(roi, yPct);
        final int zRadius = percentageToRadiusZ(roi, zPct);

        return dilateROI(roi, xRadius, yRadius, zRadius);
    }

    /**
     * Perform a morphological dilation on the specified ROI by the given radius in each dimension
     *
     * @param roi     the ROI to dilate
     * @param xRadius the radius in pixels along X
     * @param yRadius the radius in pixels along X
     * @param zRadius the radius in pixels along Z (not used if <code>roi</code> is 2D)
     * @return a new, dilated ROI of type "area"
     * @throws InterruptedException if the user interrupts the process
     */
    public static @Nullable ROI dilateROI(final ROI roi, final int xRadius, final int yRadius, final int zRadius) throws InterruptedException {
        final int rx = xRadius, rrx = rx * rx;
        final int ry = yRadius, rry = ry * ry;
        final int rz = zRadius, rrz = rz * rz;

        if (roi instanceof ROI2D) {
            final BooleanMask2D m2 = ((ROI2D) roi).getBooleanMask(true);
            final ROI2DArea r2 = new ROI2DArea(m2);
            r2.setC(((ROI2D) roi).getC());
            r2.setZ(((ROI2D) roi).getZ());
            r2.setT(((ROI2D) roi).getT());
            r2.setName(roi.getName() + " dilated[" + xRadius + "," + yRadius + "]");

            r2.beginUpdate();

            for (final Point p : m2.getContourPoints()) {
                // Brute force
                for (int y = -ry; y <= ry; y++)
                    for (int x = -rx; x <= rx; x++) {
                        final double xr2 = rrx == 0 ? 0 : (double) (x * x) / rrx;
                        final double yr2 = rry == 0 ? 0 : (double) (y * y) / rry;

                        if (xr2 + yr2 <= 1.0) {
                            if (!m2.contains(p.x + x, p.y + y))
                                r2.addPoint(p.x + x, p.y + y);
                        }
                    }

                // Bresenham style
                // int x = r;
                // int y = 0;
                // int xChange = 1 - (r << 1);
                // int yChange = 0;
                // int radiusError = 0;
                //
                // while (x >= y)
                // {
                // for (int i = p.x - x; i <= p.x + x; i++)
                // {
                // if (!m2.contains(i, p.y + y)) r2.addPoint(i, p.y + y);
                // if (!m2.contains(i, p.y - y)) r2.addPoint(i, p.y - y);
                // }
                // for (int i = p.x - y; i <= p.x + y; i++)
                // {
                // if (!m2.contains(i, p.y + x)) r2.addPoint(i, p.y + x);
                // if (!m2.contains(i, p.y - x)) r2.addPoint(i, p.y - x);
                // }
                //
                // y++;
                // radiusError += yChange;
                // yChange += 2;
                // if (((radiusError << 1) + xChange) > 0)
                // {
                // x--;
                // radiusError += xChange;
                // xChange += 2;
                // }
                // }
            }
            r2.endUpdate();

            return r2;
        }
        else if (roi instanceof final ROI3D roi3D) {
            final BooleanMask3D m3 = roi3D.getBooleanMask(true);

            final ROI3DArea r3 = new ROI3DArea(m3);
            r3.setC(((ROI3D) roi).getC());
            r3.setT(((ROI3D) roi).getT());
            r3.setName(roi.getName() + " dilated[" + xRadius + "," + yRadius + "," + zRadius + "]");

            r3.beginUpdate();

            for (final Point3D.Integer p : m3.getContourPoints()) { // Brute force

                if (rrz == 0) {
                    final int z = 0;
                    for (int y = -ry; y <= ry; y++)
                        for (int x = -rx; x <= rx; x++) {
                            final double xr2 = rrx == 0 ? 0 : (double) (x * x) / rrx;
                            final double yr2 = rry == 0 ? 0 : (double) (y * y) / rry;

                            if (xr2 + yr2 <= 1.0) {
                                if (!m3.contains(p.x + x, p.y + y, p.z + z))
                                    r3.addPoint(p.x + x, p.y + y, p.z + z);
                            }
                        }
                }
                else {
                    for (int z = -rz; z <= rz; z++)
                        for (int y = -ry; y <= ry; y++)
                            for (int x = -rx; x <= rx; x++) {
                                final double xr2 = rrx == 0 ? 0 : (double) (x * x) / rrx;
                                final double yr2 = rry == 0 ? 0 : (double) (y * y) / rry;
                                final double zr2 = rrz == 0 ? 0 : (double) (z * z) / rrz;

                                if (xr2 + yr2 + zr2 <= 1.0) {
                                    if (!m3.contains(p.x + x, p.y + y, p.z + z))
                                        r3.addPoint(p.x + x, p.y + y, p.z + z);
                                }
                            }
                }
            }
            r3.endUpdate();
            r3.optimizeBounds();
            return r3;
        }

        IcyLogger.warn(DilateROI.class, "Unsupported ROI: " + roi.getName());
        return null;
    }
}
