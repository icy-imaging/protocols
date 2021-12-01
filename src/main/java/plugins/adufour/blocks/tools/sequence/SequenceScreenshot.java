package plugins.adufour.blocks.tools.sequence;

import icy.canvas.Canvas2D;
import icy.gui.viewer.Viewer;
import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarSequence;

public class SequenceScreenshot extends Plugin implements SequenceBlock
{
    private VarSequence seq = new VarSequence("Sequence", null);

    private VarSequence seqOut = new VarSequence("Screenshot", null);

    @Override
    public void run()
    {
        final Sequence in = seq.getValue();
        final int time = in.getSizeT();
        final int depth = in.getSizeZ();
        final Sequence out = new Sequence("Screenshot of " + in.getName());

        final Viewer viewer = new Viewer(in, false);
        final Canvas2D[] canvas2DP = new Canvas2D[1];

        // init
        canvas2DP[0] = null;
        // force completion of SWING EDT tasks
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                canvas2DP[0] = (Canvas2D) viewer.getCanvas();
            }
        });

        final long startTime = System.currentTimeMillis();
        // wait 2 seconds max for initialization
        while ((canvas2DP[0] == null) && ((System.currentTimeMillis() - startTime) < 2000L))
            ThreadUtil.sleep(10);

        final Canvas2D canvas2D = canvas2DP[0];

        try
        {
            for (int t = 0; t < time; t++)
                for (int z = 0; z < depth; z++)
                    out.setImage(t, z, canvas2D.getRenderedImage(t, z, -1, false));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return;
        }
        finally
        {
            viewer.close();
        }

        seqOut.setValue(out);
    }

    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("input sequence", seq);
    }

    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("screenshot", seqOut);
    }
}
