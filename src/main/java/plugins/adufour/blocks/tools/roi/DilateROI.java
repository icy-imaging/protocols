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

public class DilateROI extends MorphROI
{
    public DilateROI()
    {
        roiOUT = new VarROIArray("Dilated ROI");
    }

    @Override
    public void run()
    {
        try
        {
            switch (unit.getValue())
            {
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
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Perform a morphological dilation on the specified set of ROI by the given amount in each
     * dimension
     * 
     * @param inputRoi
     *        The ROI to dilate
     * @param xRadius
     *        the radius (in pixels) along X
     * @param yRadius
     *        the radius (in pixels) along X
     * @param zRadius
     *        the radius (in pixels) along Z (not used if <code>roi</code> is 2D)
     * @return a new set of dilated ROI of type "area"
     * @throws InterruptedException
     */
    public static ROI[] dilateROI(ROI[] inputRoi, int xRadius, int yRadius, int zRadius) throws InterruptedException
    {
        ArrayList<ROI> out = new ArrayList<ROI>(inputRoi.length);

        for (ROI roi : inputRoi)
        {
            if (Thread.currentThread().isInterrupted())
                break;

            ROI dilated = dilateROI(roi, xRadius, yRadius, zRadius);
            if (dilated != null)
                out.add(dilated);
        }

        return out.toArray(new ROI[out.size()]);
    }

    /**
     * Perform a morphological dilation on the specified set of ROI by the given percentage in each
     * dimension
     * 
     * @param inputRoi
     *        the ROI to dilate
     * @param xPct
     *        the percentage (from 0 to 100) to dilate along X
     * @param yPct
     *        the percentage (from 0 to 100) to dilate along Y
     * @param zPct
     *        the percentage (from 0 to 100) to dilate along Z (not used in 2D)
     * @return a new set of dilated ROI of type "area"
     * @throws InterruptedException
     */
    public static ROI[] dilateROIByPercentage(ROI[] inputRoi, int xPct, int yPct, int zPct) throws InterruptedException
    {
        ArrayList<ROI> out = new ArrayList<ROI>(inputRoi.length);

        for (ROI roi : inputRoi)
        {
            if (Thread.currentThread().isInterrupted())
                break;

            ROI dilated = dilateROIByPercentage(roi, xPct, yPct, zPct);
            if (dilated != null)
                out.add(dilated);
        }

        return out.toArray(new ROI[out.size()]);
    }

    /**
     * Perform a morphological dilation on the specified ROI by the given percentage in each
     * dimension
     * 
     * @author Joel Rogers, Alexandre Dufour
     * @param roi
     *        the ROI to dilate
     * @param xPct
     *        the percentage (from 0 to 100) to dilate along X
     * @param yPct
     *        the percentage (from 0 to 100) to dilate along Y
     * @param zPct
     *        the percentage (from 0 to 100) to dilate along Z (not used in 2D)
     * @return a new, dilated ROI of type "area"
     * @throws InterruptedException
     */
    public static ROI dilateROIByPercentage(ROI roi, int xPct, int yPct, int zPct) throws InterruptedException
    {
        int xRadius = percentageToRadiusX(roi, xPct);
        int yRadius = percentageToRadiusY(roi, yPct);
        int zRadius = percentageToRadiusZ(roi, zPct);

        return dilateROI(roi, xRadius, yRadius, zRadius);
    }

    /**
     * Perform a morphological dilation on the specified ROI by the given radius in each dimension
     * 
     * @param roi
     *        the ROI to dilate
     * @param xRadius
     *        the radius in pixels along X
     * @param yRadius
     *        the radius in pixels along X
     * @param zRadius
     *        the radius in pixels along Z (not used if <code>roi</code> is 2D)
     * @return a new, dilated ROI of type "area"
     * @throws InterruptedException
     */
    public static ROI dilateROI(ROI roi, int xRadius, int yRadius, int zRadius) throws InterruptedException
    {
        int rx = xRadius, rrx = rx * rx;
        int ry = yRadius, rry = ry * ry;
        int rz = zRadius, rrz = rz * rz;

        if (roi instanceof ROI2D)
        {
            BooleanMask2D m2 = ((ROI2D) roi).getBooleanMask(true);
            ROI2DArea r2 = new ROI2DArea(m2);
            r2.setC(((ROI2D) roi).getC());
            r2.setZ(((ROI2D) roi).getZ());
            r2.setT(((ROI2D) roi).getT());
            r2.setName(roi.getName() + " dilated[" + xRadius + "," + yRadius + "]");

            r2.beginUpdate();

            for (Point p : m2.getContourPoints())
            {
                // Brute force
                for (int y = -ry; y <= ry; y++)
                    for (int x = -rx; x <= rx; x++)
                    {
                        double xr2 = rrx == 0 ? 0 : x * x / rrx;
                        double yr2 = rry == 0 ? 0 : y * y / rry;

                        if (xr2 + yr2 <= 1.0)
                        {
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
        else if (roi instanceof ROI3D)
        {
            ROI3D roi3D = (ROI3D) roi;

            BooleanMask3D m3 = roi3D.getBooleanMask(true);

            ROI3DArea r3 = new ROI3DArea(m3);
            r3.setC(((ROI3D) roi).getC());
            r3.setT(((ROI3D) roi).getT());
            r3.setName(roi.getName() + " dilated[" + xRadius + "," + yRadius + "," + zRadius + "]");

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

                            if (xr2 + yr2 <= 1.0)
                            {
                                if (!m3.contains(p.x + x, p.y + y, p.z + z))
                                    r3.addPoint(p.x + x, p.y + y, p.z + z);
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

                                if (xr2 + yr2 + zr2 <= 1.0)
                                {
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

        System.out.println("[Dilate ROI] Warning: unsupported ROI: " + roi.getName());
        return null;
    }
}
