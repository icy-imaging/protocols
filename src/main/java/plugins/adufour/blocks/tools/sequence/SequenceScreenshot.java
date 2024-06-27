/*
 * Copyright (c) 2010-2024. Institut Pasteur.
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

package plugins.adufour.blocks.tools.sequence;

import org.bioimageanalysis.icy.extension.plugin.abstract_.Plugin;
import org.bioimageanalysis.icy.gui.canvas.Canvas2D;
import org.bioimageanalysis.icy.gui.viewer.Viewer;
import org.bioimageanalysis.icy.model.sequence.Sequence;
import org.bioimageanalysis.icy.system.thread.ThreadUtil;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarSequence;

public class SequenceScreenshot extends Plugin implements SequenceBlock {
    private final VarSequence seq = new VarSequence("Sequence", null);

    private final VarSequence seqOut = new VarSequence("Screenshot", null);

    @Override
    public void run() {
        final Sequence in = seq.getValue();
        final int time = in.getSizeT();
        final int depth = in.getSizeZ();
        final Sequence out = new Sequence("Screenshot of " + in.getName());

        final Viewer viewer = new Viewer(in, false);
        final Canvas2D[] canvas2DP = new Canvas2D[1];

        // init
        canvas2DP[0] = null;
        // force completion of SWING EDT tasks
        ThreadUtil.invokeLater(() -> canvas2DP[0] = (Canvas2D) viewer.getCanvas());

        final long startTime = System.currentTimeMillis();
        // wait 2 seconds max for initialization
        while ((canvas2DP[0] == null) && ((System.currentTimeMillis() - startTime) < 2000L))
            ThreadUtil.sleep(10);

        final Canvas2D canvas2D = canvas2DP[0];

        try {
            for (int t = 0; t < time; t++)
                for (int z = 0; z < depth; z++)
                    out.setImage(t, z, canvas2D.getRenderedImage(t, z, -1, false));
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        finally {
            viewer.close();
        }

        seqOut.setValue(out);
    }

    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("input sequence", seq);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
        outputMap.add("screenshot", seqOut);
    }
}
