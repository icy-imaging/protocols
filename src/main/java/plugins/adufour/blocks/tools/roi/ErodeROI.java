/*
 * Copyright (c) 2010-2026. Institut Pasteur.
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

import fr.icy.extension.kernel.roi.roi2d.ROI2DArea;
import fr.icy.extension.kernel.roi.roi3d.ROI3DArea;
import fr.icy.common.geom.point.Point3D;
import fr.icy.model.roi.ROI;
import fr.icy.model.roi.ROI2D;
import fr.icy.model.roi.ROI3D;
import fr.icy.model.roi.mask.BooleanMask2D;
import fr.icy.model.roi.mask.BooleanMask3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.util.VarException;

import java.awt.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Joel Rogers
 * @author Alexandre Dufour
 */
public class ErodeROI extends MorphROI {
    private static final Logger LOGGER = Logger.getLogger(ErodeROI.class.getName());

    public ErodeROI() {
        roiOUT = new VarROIArray("Eroded ROI");
    }

    @Override
    public void run() {
        try {
            switch (unit.getValue()) {
                case PIXELS:
                    roiOUT.setValue(erodeROI(roiIN.getValue(), x.getValue(), y.getValue(), z.getValue()));
                    break;
                case PERCENTAGE:
                    roiOUT.setValue(erodeROIByPercentage(roiIN.getValue(), x.getValue(), y.getValue(), z.getValue()));
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
     * Perform a morphological erosion on the specified set of ROI by the given radius in each
     * dimension
     *
     * @param inputRoi the ROI to erode
     * @param xRadius  the radius (in pixels) along X
     * @param yRadius  the radius (in pixels) along Y
     * @param zRadius  the radius (in pixels) along Z (not used if <code>roi</code> is 2D)
     * @return a new set of eroded ROI with type "area"
     * @throws InterruptedException if the user interrupts the process
     */
    public static ROI @NotNull [] erodeROI(final ROI @NotNull [] inputRoi, final int xRadius, final int yRadius, final int zRadius) throws InterruptedException {
        final ArrayList<ROI> out = new ArrayList<>(inputRoi.length);

        for (final ROI roi : inputRoi) {
            if (Thread.currentThread().isInterrupted())
                break;

            final ROI eroded = erodeROI(roi, xRadius, yRadius, zRadius);
            if (eroded != null)
                out.add(eroded);
        }

        return out.toArray(new ROI[0]);
    }

    /**
     * Perform a morphological erosion on the specified set of ROI by the given scale factor in each
     * dimension
     *
     * @param inputRoi the ROI to erode
     * @param xPct     the percentage (from 0 to 100) to erode along X
     * @param yPct     the percentage (from 0 to 100) to erode along Y
     * @param zPct     the percentage (from 0 to 100) to erode along Z (not used in 2D)
     * @return a new set of eroded ROI with type "area"
     * @throws InterruptedException if the user interrupts the process
     */
    public static ROI @NotNull [] erodeROIByPercentage(final ROI @NotNull [] inputRoi, final int xPct, final int yPct, final int zPct) throws InterruptedException {
        final ArrayList<ROI> out = new ArrayList<>(inputRoi.length);

        for (final ROI roi : inputRoi) {
            if (Thread.currentThread().isInterrupted())
                break;

            final ROI eroded = erodeROIByPercentage(roi, xPct, yPct, zPct);
            if (eroded != null)
                out.add(eroded);
        }

        return out.toArray(new ROI[0]);
    }

    /**
     * Perform a morphological erosion on the specified ROI by the given scale factor in each
     * dimension
     *
     * @param roi  the ROI to erode
     * @param xPct the percentage (from 0 to 100) to dilate along X
     * @param yPct the percentage (from 0 to 100) to dilate along Y
     * @param zPct the percentage (from 0 to 100) to dilate along Z (not used in 2D)
     * @return a new, dilated ROI of type "area"
     * @throws InterruptedException if the user interrupts the process
     */
    public static ROI erodeROIByPercentage(final ROI roi, final int xPct, final int yPct, final int zPct) throws InterruptedException {
        final int xRadius = percentageToRadiusX(roi, xPct);
        final int yRadius = percentageToRadiusY(roi, yPct);
        final int zRadius = percentageToRadiusZ(roi, zPct);

        return erodeROI(roi, xRadius, yRadius, zRadius);
    }

    /**
     * Perform a morphological erosion on the specified ROI by the given radius in each dimension
     *
     * @param roi     the ROI to erode
     * @param xRadius the radius in pixels along X
     * @param yRadius the radius in pixels along X
     * @param zRadius the radius in pixels along Z (not used if <code>roi</code> is 2D)
     * @return a new, eroded ROI of type "area"
     * @throws InterruptedException if the user interrupts the process
     */
    public static @Nullable ROI erodeROI(final ROI roi, final int xRadius, final int yRadius, final int zRadius) throws InterruptedException {
        // The basis of this erosion operator is to remove all pixels within a distance of "radius"
        // from the border. Since we have easy access to the contour points of the ROI, we will
        // start from there and instead use a radius of "radius - 1" when searching for pixels to
        // erase, to be consistent with the dual dilation operator, such that openings
        // (erosion + dilation) and closings (dilation + erosion) preserve the global ROI size

        final int rx = Math.max(0, xRadius - 1), rrx = rx * rx;
        final int ry = Math.max(0, yRadius - 1), rry = ry * ry;
        final int rz = Math.max(0, zRadius - 1), rrz = rz * rz;

        if (roi instanceof ROI2D) {
            final BooleanMask2D m2 = ((ROI2D) roi).getBooleanMask(true);
            final ROI2DArea r2 = new ROI2DArea(m2);
            r2.setC(((ROI2D) roi).getC());
            r2.setZ(((ROI2D) roi).getZ());
            r2.setT(((ROI2D) roi).getT());
            r2.setName(roi.getName() + " eroded[" + xRadius + "," + yRadius + "]");

            r2.beginUpdate();

            for (final Point p : m2.getContourPoints()) {
                // Brute force
                for (int y = -ry; y <= ry; y++)
                    for (int x = -rx; x <= rx; x++) {
                        final double xr2 = rrx == 0 ? 0 : (double) (x * x) / rrx;
                        final double yr2 = rry == 0 ? 0 : (double) (y * y) / rry;

                        // correct the sphere equation to include the outer rim for each pixel
                        if (xr2 + yr2 <= 2.0) {
                            if (m2.contains(p.x + x, p.y + y))
                                r2.removePoint(p.x + x, p.y + y);
                        }
                    }
            }
            r2.endUpdate();

            return r2.getNumberOfPoints() > 0 ? r2 : null;
        }
        else if (roi instanceof final ROI3D roi3D) {
            final BooleanMask3D m3 = roi3D.getBooleanMask(true);

            final ROI3DArea r3 = new ROI3DArea(m3);
            r3.setC(((ROI3D) roi).getC());
            r3.setT(((ROI3D) roi).getT());
            r3.setName(roi.getName() + " eroded[" + xRadius + "," + yRadius + "," + zRadius + "]");

            r3.beginUpdate();

            for (final Point3D.Integer p : m3.getContourPoints()) { // Brute force

                if (rrz == 0) {
                    final int z = 0;
                    for (int y = -ry; y <= ry; y++)
                        for (int x = -rx; x <= rx; x++) {
                            final double xr2 = rrx == 0 ? 0 : (double) (x * x) / rrx;
                            final double yr2 = rry == 0 ? 0 : (double) (y * y) / rry;

                            // correct the sphere equation to include the outer rim for each pixel
                            if (xr2 + yr2 <= 2.0) {
                                if (m3.contains(p.x + x, p.y + y, p.z + z))
                                    try {
                                        r3.removePoint(p.x + x, p.y + y, p.z + z);
                                    }
                                    catch (final ArrayIndexOutOfBoundsException aioobe) {
                                        // FIXME @Stephane, what's wrong here?!
                                    }
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

                                // correct the sphere equation to include the outer rim for each
                                // pixel
                                if (xr2 + yr2 + zr2 <= 2.0) {
                                    if (m3.contains(p.x + x, p.y + y, p.z + z))
                                        try {
                                            r3.removePoint(p.x + x, p.y + y, p.z + z);
                                        }
                                        catch (final ArrayIndexOutOfBoundsException aioobe) {
                                            // FIXME @Stephane, what's wrong here?!
                                        }
                                }
                            }
                }
            }
            r3.endUpdate();
            r3.optimizeBounds();

            return r3.getNumberOfPoints() > 0 ? r3 : null;
        }

        if (LOGGER.isLoggable(Level.WARNING))
            LOGGER.log(Level.WARNING, "Unsupported ROI: " + roi.getName());
        return null;
    }
}
