package plugins.adufour.protocols.gui.link;

import java.awt.Rectangle;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

import plugins.adufour.blocks.lang.Link;
import plugins.adufour.protocols.gui.block.BlockPanel;
import plugins.adufour.vars.lang.Var;

public class CurvedLine extends Line {
    /**
     * Creates a line representing a link between two variables
     *
     * @param srcPanel the source panel of the link
     * @param dstPanel the destination panel of the link
     * @param link     the link between the two variables
     * @throws IllegalArgumentException if the <code>link</code> argument is <code>null</code>
     */
    public CurvedLine(BlockPanel srcPanel, BlockPanel dstPanel, Link<?> link) throws IllegalArgumentException {
        super(srcPanel, dstPanel, link);
    }

    /**
     * Creates a line exposing a variable outside a work flow.
     *
     * @param srcPanel        the source panel of the link
     * @param dstPanel        the destination panel of the link
     * @param exposedVariable the exposed variable
     */
    public CurvedLine(BlockPanel srcPanel, BlockPanel dstPanel, Var<?> exposedVariable) {
        super(srcPanel, dstPanel, exposedVariable);
    }

    @Override
    public void updateShape(Path2D path, Ellipse2D closeButton) {
        float border = 30;
        path.append(new CubicCurve2D.Float(x1, y1, x1 + border, y1, x2 - border, y2, x2, y2), false);
        Rectangle r = path.getBounds();
        closeButton.setFrame(r.x + r.width / 2f - 6, r.y + r.height / 2f - 6, 13, 13);
    }
}
