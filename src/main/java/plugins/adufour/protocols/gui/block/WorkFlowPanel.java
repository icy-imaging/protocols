package plugins.adufour.protocols.gui.block;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import icy.system.thread.ThreadUtil;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.Loop;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.blocks.util.VarVisibilityListener;
import plugins.adufour.protocols.gui.link.DragDropZone;
import plugins.adufour.protocols.gui.link.Line;
import plugins.adufour.protocols.gui.link.RoundedSquareLine;
import plugins.adufour.vars.lang.Var;

@SuppressWarnings("serial")
public class WorkFlowPanel extends BlockPanel implements VarVisibilityListener
{
    private final WorkFlow innerWorkFlow;
    
    protected final JScrollPane scrollPane;
    
    protected final WorkFlowContainer innerFlowPane;
    
    private final HashMap<Var<?>, Line> exposingLinks = new HashMap<Var<?>, Line>();
    
    private final MouseAdapter mouseAdapter;
    
    protected Box varBox;
    
    protected final JMenuItem menuRemoveEnclosure = new JMenuItem("Remove block but keep contents");
    
    public WorkFlowPanel(WorkFlowContainer wfPane, final BlockDescriptor blockDesc)
    {
        super(wfPane, blockDesc);
        
        this.innerWorkFlow = (WorkFlow) blockDesc.getBlock();
        
        mouseAdapter = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int xShift = innerFlowPane.getLocationOnScreen().x - getLocationOnScreen().x;
                int yShift = innerFlowPane.getLocationOnScreen().y - getLocationOnScreen().y;
                Point realPoint = new Point(e.getX() + xShift, e.getY() + yShift);
                
                for (Var<?> var : exposingLinks.keySet())
                    if (exposingLinks.get(var).isOverCloseButton(realPoint))
                    {
                        VarList vars = blockDesc.inputVars;
                        
                        if (!vars.contains(var)) vars = blockDesc.outputVars;
                        
                        vars.setVisible(var, !vars.isVisible(var));
                        
                        // prevent concurrent modification
                        break;
                    }
            }
            
            @Override
            public void mouseMoved(MouseEvent e)
            {
                int xShift = innerFlowPane.getLocationOnScreen().x - getLocationOnScreen().x;
                int yShift = innerFlowPane.getLocationOnScreen().y - getLocationOnScreen().y;
                Point realPoint = new Point(e.getX() + xShift, e.getY() + yShift);
                
                for (Line l : exposingLinks.values())
                    l.setCustomColor(l.contains(realPoint.x, realPoint.y));
            }
        };
        
        blockDesc.inputVars.addVisibilityListener(this);
        blockDesc.outputVars.addVisibilityListener(this);
        
        this.innerFlowPane = new WorkFlowContainer(innerWorkFlow, wfPane.isEditable());
        this.innerFlowPane.setParentPanel(this);
        
        this.innerFlowPane.addMouseListener(mouseAdapter);
        this.innerFlowPane.addMouseMotionListener(mouseAdapter);
        
        this.scrollPane = new JScrollPane(innerFlowPane);
        scrollPane.setOpaque(false);
        scrollPane.setPreferredSize(new Dimension());
        
        // XXX
        varBox = Box.createVerticalBox();
        varBox.setOpaque(false);
        varBox.setVisible(false);
        add(varBox, BorderLayout.SOUTH);
        
        // check whether the descriptor has a stored size
        Dimension size = innerWorkFlow.getBlockDescriptor().getDimension();
        
        if (size.width != 0)
        {
            // use it
            setPreferredSize(size);
        }
        else
        {
            // set a default (non-zero) size (because the "real" preferred size is currently 0)
            setPreferredSize(new Dimension(400, 400));
        }
        setResizeable(!blockDesc.isCollapsed());
        
        // add an extra option to close the work flow while preserving its contents
        menuRemoveEnclosure.addActionListener(this);
        menuRemoveEnclosure.setIcon(ICON_REMOVE);
        menuRemoveEnclosure.setToolTipText("<html><h4>Remove this block but keep its contents</h4></html>");
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == menuRemoveEnclosure)
        {
            innerFlowPane.selectAllBlocks();
            workFlowPane.getWorkFlow().removeBlock(blockDesc, true);
        }
        else super.actionPerformed(e);
    }
    
    @Override
    protected void drawMenu()
    {
        super.drawMenu();
        
        // add an extra option to close the work flow while preserving its contents
        super.menu.add(menuRemoveEnclosure);
    }
    
    @Override
    protected void drawContent()
    {
        int maxRow = drawExposedLinks(0);
        
        drawInnerWorkFlowContainer(maxRow + 1);
    }
    
    /**
     * Draws the drag'n'drop zones representing the exposed variables of the inner work flow
     * 
     * @param gridRow
     *            the row where to start drawing within the {@link GridBagLayout}
     * @return the last row used in the {@link GridBagLayout}
     */
    protected int drawExposedLinks(int row)
    {
        int rowIn = row;
        for (Var<?> input : innerWorkFlow.getBlockDescriptor().inputVars)
        {
            // don't show hidden variable
            if (!blockDesc.inputVars.isVisible(input)) continue;
            
            // don't show local loop variables
            if (blockDesc.isLoop() && ((Loop) blockDesc.getBlock()).isLoopVariable(input)) continue;
            
            if (!varDropZones.containsKey(input)) varDropZones.put(input, createVarDropZone(input, true, DragDropZone.LINK_RIGHT));
            
            GridBagConstraints gbc_dropZone = new GridBagConstraints();
            gbc_dropZone.anchor = GridBagConstraints.WEST;
            gbc_dropZone.fill = GridBagConstraints.NONE;
            gbc_dropZone.insets = new Insets(2, 0, 0, 0);
            gbc_dropZone.gridx = 0;
            gbc_dropZone.gridy = rowIn++;
            gbc_dropZone.weighty = 0;
            DragDropZone dropZone = varDropZones.get(input);
            jPanelContent.add(dropZone, gbc_dropZone);
        }
        
        int rowOut = row;
        for (Var<?> output : innerWorkFlow.getBlockDescriptor().outputVars)
        {
            // don't show hidden variable
            if (!blockDesc.outputVars.isVisible(output)) continue;
            
            // don't show local loop variables
            if (blockDesc.isLoop() && ((Loop) blockDesc.getBlock()).isLoopVariable(output)) continue;
            
            if (!varDragZones.containsKey(output)) varDragZones.put(output, createVarDragZone(output, true));
            
            GridBagConstraints gbc_dragZone = new GridBagConstraints();
            gbc_dragZone.anchor = GridBagConstraints.EAST;
            gbc_dragZone.fill = GridBagConstraints.NONE;
            gbc_dragZone.insets = new Insets(2, 0, 0, 0);
            gbc_dragZone.gridx = 0;
            gbc_dragZone.gridy = rowOut++;
            DragDropZone dragZone = varDragZones.get(output);
            jPanelContent.add(dragZone, gbc_dragZone);
        }
        
        return Math.max(rowIn, rowOut);
    }
    
    /**
     * Draws the container representing the inner work flow
     * 
     * @param row
     *            the row where the container should be added within the {@link GridBagLayout}
     */
    protected void drawInnerWorkFlowContainer(int row)
    {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 15, 5, 13);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = row;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        jPanelContent.add(scrollPane, gbc);
    }
    
    @Override
    void dispose()
    {
        blockDesc.inputVars.addVisibilityListener(this);
        blockDesc.outputVars.addVisibilityListener(this);
        
        menuRemoveEnclosure.removeActionListener(this);
        
        innerFlowPane.removeMouseListener(mouseAdapter);
        innerFlowPane.removeMouseMotionListener(mouseAdapter);
        
        innerFlowPane.dispose();
        
        super.dispose();
    }
    
    @Override
    public void paintChildren(Graphics g)
    {
        Graphics gg = g.create();        
        
        super.paintChildren(g);
        
        Rectangle clip = scrollPane.getBounds();
        gg.clipRect(clip.x, clip.y, clip.width, clip.height);
        if (!blockDesc.isCollapsed()) for (Line line : exposingLinks.values())
        {
            line.update();
            line.paint((Graphics2D) gg);
        }
        gg.dispose();
        
    }
    
    @Override
    public void visibilityChanged(final Var<?> variable, boolean isVisible)
    {
        // Refresh first to ensure all existing blocks and links are visible
        ThreadUtil.invokeNow(new Runnable()
        {
            @Override
            public void run()
            {
                refreshNow();
            }
        });
        
        if (isVisible)
        {
            if (innerWorkFlow.getInputOwner(variable) != null)
            {
                // source is an input variable
                
                final BlockDescriptor innerBlock = innerWorkFlow.getInputOwner(variable);
                
                ThreadUtil.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        final BlockPanel ownerPanel = innerFlowPane.getBlockPanel(innerBlock);
                        
                        exposingLinks.put(variable, new RoundedSquareLine(WorkFlowPanel.this, ownerPanel, variable)
                        {
                            @Override
                            protected DragDropZone getP1Zone()
                            {
                                return varDropZones.get(variable);
                            }
                            
                            @Override
                            protected DragDropZone getP2Zone()
                            {
                                return ownerPanel.varDropZones.get(variable);
                            }
                            
                            @Override
                            public void updateP1()
                            {
                                if (blockDesc.isCollapsed()) return;
                                
                                DragDropZone dz = varDropZones.get(variable);
                                int y = dz.getLocationOnScreen().y;
                                int offsetY = getLocationOnScreen().y;
                                
                                x1 = dz.getWidth();
                                y1 = y - offsetY + dz.getHeight() / 2;
                            }
                            
                            @Override
                            public void updateP2()
                            {
                                if (ownerPanel.blockDesc.isCollapsed())
                                {
                                    Point loc = ownerPanel.getLocationOnScreen();
                                    x2 = loc.x - getLocationOnScreen().x + SHADOW_SIZE;
                                    y2 = loc.y - getLocationOnScreen().y + ownerPanel.getHeight() / 2;
                                }
                                else
                                {
                                    DragDropZone dz = ownerPanel.varDropZones.get(variable);
                                    Point loc = dz.getLocationOnScreen();
                                    x2 = loc.x - getLocationOnScreen().x;
                                    y2 = loc.y - getLocationOnScreen().y + dz.getHeight() / 2;
                                }
                            }
                        });
                        
                        refresh();
                    }
                }, true);
                
            }
            else
            {
                // source is an output variable
                
                final BlockDescriptor innerBlock = innerWorkFlow.getOutputOwner(variable);
                
                ThreadUtil.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        
                        final BlockPanel ownerPanel = innerFlowPane.getBlockPanel(innerBlock);
                        
                        exposingLinks.put(variable, new RoundedSquareLine(ownerPanel, WorkFlowPanel.this, variable)
                        {
                            @Override
                            protected DragDropZone getP1Zone()
                            {
                                return ownerPanel.varDragZones.get(variable);
                            }
                            
                            @Override
                            protected DragDropZone getP2Zone()
                            {
                                return varDragZones.get(variable);
                            }
                            
                            @Override
                            public void updateP1()
                            {
                                if (ownerPanel.blockDesc.isCollapsed())
                                {
                                    Point loc = ownerPanel.getLocationOnScreen();
                                    x1 = loc.x - getLocationOnScreen().x + ownerPanel.getWidth() - SHADOW_SIZE;
                                    y1 = loc.y - getLocationOnScreen().y + ownerPanel.getHeight() / 2;
                                }
                                else
                                {
                                    DragDropZone dz = ownerPanel.varDragZones.get(variable);
                                    Point loc = dz.getLocationOnScreen();
                                    x1 = loc.x - getLocationOnScreen().x + dz.getWidth();
                                    y1 = loc.y - getLocationOnScreen().y + dz.getHeight() / 2;
                                }
                            }
                            
                            @Override
                            public void updateP2()
                            {
                                if (blockDesc.isCollapsed()) return;
                                
                                DragDropZone dz = varDragZones.get(variable);
                                int y = dz.getLocationOnScreen().y;
                                int innerY = getLocationOnScreen().y;
                                
                                x2 = dz.getLocation().x;
                                y2 = y - innerY + dz.getHeight() / 2;
                            }
                        });
                    }
                }, true);
            }
        }
        else if (exposingLinks.containsKey(variable))
        {
            variable.setReference(null);
            
            exposingLinks.remove(variable).dispose();
            
            BlockDescriptor block = innerWorkFlow.getInputOwner(variable);
            
            WorkFlow parentWorkFlow = blockDesc.getContainer();
            
            if (block != null)
            {
                // source is an input variable
                varDropZones.remove(variable);
                blockDesc.getContainer().getBlockDescriptor().removeInput(variable);
                
                // remove links pointed to the exposed variable (if any)
                if (parentWorkFlow.isLinked(variable)) parentWorkFlow.removeLink(variable);
            }
            else
            {
                block = innerWorkFlow.getOutputOwner(variable);
                
                if (block != null)
                {
                    varDragZones.remove(variable);
                    blockDesc.getContainer().getBlockDescriptor().removeOutput(variable);
                    
                    // remove links pointed to the exposed variable (if any)
                    ArrayList<Link<?>> linksToDelete = new ArrayList<Link<?>>();
                    for (Link<?> link : parentWorkFlow.getLinksIterator())
                        if (link.srcVar == variable) linksToDelete.add(link);
                        
                    for (Link<?> link : linksToDelete)
                        parentWorkFlow.removeLink(link.dstVar);
                }
            }
        }
        
        repaint();
        
        // Repaint the top-level container
        getParent().repaint();
    }
    
    public void drawPanel()
    {
        super.drawPanel();
        varBox.setVisible(blockDesc.isCollapsed());
        if (blockDesc.isCollapsed())
        {
            GridBagConstraints gbc_dropZone = new GridBagConstraints();
            gbc_dropZone.anchor = GridBagConstraints.WEST;
            gbc_dropZone.fill = GridBagConstraints.NONE;
            gbc_dropZone.insets = new Insets(2, 0, 0, 0);
            gbc_dropZone.gridx = 0;
            
            GridBagConstraints gbc_varName = new GridBagConstraints();
            gbc_varName.anchor = GridBagConstraints.CENTER;
            gbc_varName.fill = GridBagConstraints.HORIZONTAL;
            gbc_varName.insets = new Insets(2, 5, 0, 0);
            gbc_varName.gridx = 1;
            gbc_varName.weightx = 1;
            
            GridBagConstraints gbc_dragZone = new GridBagConstraints();
            gbc_dragZone.anchor = GridBagConstraints.EAST;
            gbc_dragZone.fill = GridBagConstraints.NONE;
            gbc_dragZone.insets = new Insets(2, 5, 0, 0);
            gbc_dragZone.gridx = 2;
            
            LinkedHashMap<BlockDescriptor, ArrayList<Var<?>>> inputs = new LinkedHashMap<BlockDescriptor, ArrayList<Var<?>>>();
            LinkedHashMap<BlockDescriptor, ArrayList<Var<?>>> outputs = new LinkedHashMap<BlockDescriptor, ArrayList<Var<?>>>();
            
            JPanel blockP;
            Box blockBox;
            JPanel varPanel;
            
            varBox.removeAll();
            
            ArrayList<Var<?>> in;
            ArrayList<Var<?>> out;
            
            int nbExposingBlocks = 0;
            
            for (BlockDescriptor bd : innerWorkFlow)
            {
                in = new ArrayList<Var<?>>();
                out = new ArrayList<Var<?>>();
                
                for (Var<?> v : bd.inputVars)
                    if (exposingLinks.keySet().contains(v))
                    {
                        in.add(v);
                        if (!inputs.containsKey(bd))
                        {
                            inputs.put(bd, in);
                            outputs.put(bd, out);
                            nbExposingBlocks++;
                        }
                    }
                for (Var<?> v : bd.outputVars)
                    if (exposingLinks.keySet().contains(v))
                    {
                        out.add(v);
                        if (!inputs.containsKey(bd))
                        {
                            inputs.put(bd, in);
                            outputs.put(bd, out);
                            nbExposingBlocks++;
                        }
                    }
            }
            
            varBox.setPreferredSize(new Dimension(0, exposingLinks.size() * 20 + nbExposingBlocks * 25));
            
            for (BlockDescriptor bd : inputs.keySet())
            {
                varBox.add(new JSeparator(JSeparator.HORIZONTAL));
                blockP = new JPanel(new BorderLayout());
                blockP.setOpaque(false);
                
                JLabel blockName = new JLabel("\t" + bd.getDefinedName(), JLabel.CENTER);
                blockName.setFont(blockName.getFont().deriveFont(Font.BOLD + Font.ITALIC, 12));
                blockP.add(blockName, BorderLayout.NORTH);
                
                blockBox = Box.createVerticalBox();
                blockBox.setOpaque(false);
                blockP.add(blockBox, BorderLayout.CENTER);
                
                for (Var<?> v : inputs.get(bd))
                {
                    varPanel = new JPanel(new GridBagLayout());
                    varPanel.setOpaque(false);
                    
                    varPanel.add(varDropZones.get(v), gbc_dropZone);
                    varPanel.add(new JLabel("\t" + v.getName()), gbc_varName);
                    
                    blockBox.add(varPanel);
                }
                
                for (Var<?> v : outputs.get(bd))
                {
                    varPanel = new JPanel(new GridBagLayout());
                    varPanel.setOpaque(false);
                    
                    varPanel.add(new JLabel("\t\t\t"), gbc_dropZone);
                    varPanel.add(new JLabel("\t" + v.getName()), gbc_varName);
                    varPanel.add(varDragZones.get(v), gbc_dragZone);
                    
                    blockBox.add(varPanel);
                }
                
                varBox.add(blockP);
            }
            setSize(getPreferredSize());
        }
    }
    
    public Dimension getPreferredSize()
    {
        Dimension d = super.getPreferredSize();
        
        if (!blockDesc.isCollapsed()) return d;
        
        return new Dimension(200, d.height + varBox.getPreferredSize().height);
    }
}
