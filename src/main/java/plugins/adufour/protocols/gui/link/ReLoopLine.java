package plugins.adufour.protocols.gui.link;

import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

import plugins.adufour.blocks.lang.Link;
import plugins.adufour.protocols.gui.block.BlockPanel;

public class ReLoopLine extends RoundedSquareLine {
    public ReLoopLine(BlockPanel srcPanel, BlockPanel dstPanel, Link<?> link) {
        super(srcPanel, dstPanel, link);
    }

    @Override
    public void updateShape(Path2D path, Ellipse2D closeButton) {
        path.moveTo(x1, y1);

        float xControl = Math.max(x1 + 10, x2 + 10);
        float yControl = 0.5f * (y1 + y2);

        path.curveTo(xControl, y1, xControl, y1, xControl, yControl);
        path.curveTo(xControl, y2, xControl, y2, x2, y2);

        Rectangle r = path.getBounds();
        closeButton.setFrame(r.x + r.width / 2f - 6, r.y + r.height / 2f - 6, 13, 13);
    }
}
