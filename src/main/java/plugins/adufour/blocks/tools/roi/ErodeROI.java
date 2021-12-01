package plugins.adufour.blocks.tools.roi;

import java.awt.Point;
import java.util.ArrayList;

import icy.roi.BooleanMask2D;
import icy.roi.BooleanMask3D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROI3D;
import icy.type.point.Point3D;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.util.VarException;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi3d.ROI3DArea;

public class ErodeROI extends MorphROI
{
    public ErodeROI()
    {
        roiOUT = new VarROIArray("Eroded ROI");
    }

    @Override
    public void run()
    {
        try
        {
            switch (unit.getValue())
            {
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
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Perform a morphological erosion on the specified set of ROI by the given radius in each
     * dimension
     * 
     * @param inputRoi
     *        the ROI to erode
     * @param xRadius
     *        the radius (in pixels) along X
     * @param yRadius
     *        the radius (in pixels) along Y
     * @param zRadius
     *        the radius (in pixels) along Z (not used if <code>roi</code> is 2D)
     * @return a new set of eroded ROI of type "area"
     * @throws InterruptedException
     */
    public static ROI[] erodeROI(ROI[] inputRoi, int xRadius, int yRadius, int zRadius) throws InterruptedException
    {
        ArrayList<ROI> out = new ArrayList<ROI>(inputRoi.length);

        for (ROI roi : inputRoi)
        {
            if (Thread.currentThread().isInterrupted())
                break;

            ROI eroded = erodeROI(roi, xRadius, yRadius, zRadius);
            if (eroded != null)
                out.add(eroded);
        }

        return out.toArray(new ROI[out.size()]);
    }

    /**
     * Perform a morphological erosion on the specified set of ROI by the given scale factor in each
     * dimension
     * 
     * @param inputRoi
     *        the ROI to erode
     * @param xPct
     *        the percentage (from 0 to 100) to erode along X
     * @param yPct
     *        the percentage (from 0 to 100) to erode along Y
     * @param zPct
     *        the percentage (from 0 to 100) to erode along Z (not used in 2D)
     * @return a new set of eroded ROI of type "area"
     * @throws InterruptedException
     */
    public static ROI[] erodeROIByPercentage(ROI[] inputRoi, int xPct, int yPct, int zPct) throws InterruptedException
    {
        ArrayList<ROI> out = new ArrayList<ROI>(inputRoi.length);

        for (ROI roi : inputRoi)
        {
            if (Thread.currentThread().isInterrupted())
                break;

            ROI eroded = erodeROIByPercentage(roi, xPct, yPct, zPct);
            if (eroded != null)
                out.add(eroded);
        }

        return out.toArray(new ROI[out.size()]);
    }

    /**
     * Perform a morphological erosion on the specified ROI by the given scale factor in each
     * dimension
     * 
     * @author Joel Rogers, Alexandre Dufour
     * @param roi
     *        the ROI to erode
     * @param xPct
     *        the percentage (from 0 to 100) to dilate along X
     * @param yPct
     *        the percentage (from 0 to 100) to dilate along Y
     * @param zPct
     *        the percentage (from 0 to 100) to dilate along Z (not used in 2D)
     * @return a new, dilated ROI of type "area"
     * @throws InterruptedException
     */
    public static ROI erodeROIByPercentage(ROI roi, int xPct, int yPct, int zPct) throws InterruptedException
    {
        int xRadius = percentageToRadiusX(roi, xPct);
        int yRadius = percentageToRadiusY(roi, yPct);
        int zRadius = percentageToRadiusZ(roi, zPct);

        return erodeROI(roi, xRadius, yRadius, zRadius);
    }

    /**
     * Perform a morphological erosion on the specified ROI by the given radius in each dimension
     * 
     * @param roi
     *        the ROI to erode
     * @param xRadius
     *        the radius in pixels along X
     * @param yRadius
     *        the radius in pixels along X
     * @param zRadius
     *        the radius in pixels along Z (not used if <code>roi</code> is 2D)
     * @return a new, eroded ROI of type "area"
     * @throws InterruptedException
     */
    public static ROI erodeROI(ROI roi, int xRadius, int yRadius, int zRadius) throws InterruptedException
    {
        // The basis of this erosion operator is to remove all pixels within a distance of "radius"
        // from the border. Since we have easy access to the contour points of the ROI, we will
        // start from there and instead use a radius of "radius - 1" when searching for pixels to
        // erase, so as to be consistent with the dual dilation operator, such that openings
        // (erosion + dilation) and closings (dilation + erosion) preserve the global ROI size

        int rx = Math.max(0, xRadius - 1), rrx = rx * rx;
        int ry = Math.max(0, yRadius - 1), rry = ry * ry;
        int rz = Math.max(0, zRadius - 1), rrz = rz * rz;

        if (roi instanceof ROI2D)
        {
            BooleanMask2D m2 = ((ROI2D) roi).getBooleanMask(true);
            ROI2DArea r2 = new ROI2DArea(m2);
            r2.setC(((ROI2D) roi).getC());
            r2.setZ(((ROI2D) roi).getZ());
            r2.setT(((ROI2D) roi).getT());
            r2.setName(roi.getName() + " eroded[" + xRadius + "," + yRadius + "]");

            r2.beginUpdate();

            for (Point p : m2.getContourPoints())
            {
                // Brute force
                for (int y = -ry; y <= ry; y++)
                    for (int x = -rx; x <= rx; x++)
                    {
                        double xr2 = rrx == 0 ? 0 : x * x / rrx;
                        double yr2 = rry == 0 ? 0 : y * y / rry;

                        // correct the sphere equation to include the outer rim for each pixel
                        if (xr2 + yr2 <= 2.0)
                        {
                            if (m2.contains(p.x + x, p.y + y))
                                r2.removePoint(p.x + x, p.y + y);
                        }
                    }
            }
            r2.endUpdate();

            return r2.getNumberOfPoints() > 0 ? r2 : null;
        }
        else if (roi instanceof ROI3D)
        {
            ROI3D roi3D = (ROI3D) roi;

            BooleanMask3D m3 = roi3D.getBooleanMask(true);

            ROI3DArea r3 = new ROI3DArea(m3);
            r3.setC(((ROI3D) roi).getC());
            r3.setT(((ROI3D) roi).getT());
            r3.setName(roi.getName() + " eroded[" + xRadius + "," + yRadius + "," + zRadius + "]");

            r3.beginUpdate();

            for (Point3D.Integer p : m3.getContourPoints())
            { // Brute force

                if (rrz == 0)
                {
                    int z = 0;
                    for (int y = -ry; y <= ry; y++)
                        for (int x = -rx; x <= rx; x++)
                        {
                            double xr2 = rrx == 0 ? 0 : x * x / rrx;
                            double yr2 = rry == 0 ? 0 : y * y / rry;

                            // correct the sphere equation to include the outer rim for each pixel
                            if (xr2 + yr2 <= 2.0)
                            {
                                if (m3.contains(p.x + x, p.y + y, p.z + z))
                                    try
                                    {
                                        r3.removePoint(p.x + x, p.y + y, p.z + z);
                                    }
                                    catch (ArrayIndexOutOfBoundsException aioobe)
                                    {
                                        // FIXME @Stephane, what's wrong here?!
                                    }
                            }
                        }
                }
                else
                {
                    for (int z = -rz; z <= rz; z++)
                        for (int y = -ry; y <= ry; y++)
                            for (int x = -rx; x <= rx; x++)
                            {
                                double xr2 = rrx == 0 ? 0 : x * x / rrx;
                                double yr2 = rry == 0 ? 0 : y * y / rry;
                                double zr2 = rrz == 0 ? 0 : z * z / rrz;

                                // correct the sphere equation to include the outer rim for each
                                // pixel
                                if (xr2 + yr2 + zr2 <= 2.0)
                                {
                                    if (m3.contains(p.x + x, p.y + y, p.z + z))
                                        try
                                        {
                                            r3.removePoint(p.x + x, p.y + y, p.z + z);
                                        }
                                        catch (ArrayIndexOutOfBoundsException aioobe)
                                        {
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

        System.out.println("[Erode ROI] Warning: unsupported ROI: " + roi.getName());
        return null;
    }
}
