package plugins.adufour.protocols.gui.block;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import plugins.adufour.blocks.lang.Batch;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.Loop;
import plugins.adufour.protocols.gui.link.DragDropZone;
import plugins.adufour.protocols.gui.link.DragZone;
import plugins.adufour.vars.lang.Var;

@SuppressWarnings("serial")
public class LoopPanel extends WorkFlowPanel
{
    private final Loop        loop;
    
    private final JPanel      innerLoopPanel;
    
    /**
     * Option used in loops
     */
    protected final JCheckBox menuStopOnFirstError = new JCheckBox("Stop on first error");
    
    public LoopPanel(WorkFlowContainer wfPane, BlockDescriptor blockInfo)
    {
        super(wfPane, blockInfo);
        this.loop = (Loop) blockDesc.getBlock();
        
        // create a special panel to receive the loop variables
        innerLoopPanel = new JPanel(new GridBagLayout())
        {
            @Override
            public void paintChildren(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g;
                
                Point2D p1 = gradient.getPoint1();
                p1.setLocation(p1.getX() + 1, p1.getY() - 30);
                g2.setPaint(new GradientPaint(p1, gradient.getColor1(), gradient.getPoint2(), gradient.getColor2()));
                g2.fillRect(5, 0, getWidth() - 11, getHeight());
                g2.setPaint(null);
                g2.setColor(Color.gray);
                g2.drawLine(15, getHeight() - 1, getWidth() - 6, getHeight() - 1);
                g2.drawLine(getWidth() - 6, 0, getWidth() - 6, getHeight());
                
                super.paintChildren(g2);
            }
        };
        
        innerLoopPanel.setOpaque(false);
        innerLoopPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 2));
        
        menuStopOnFirstError.addActionListener(this);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == menuStopOnFirstError)
        {
            loop.stopOnFirstError.setValue(menuStopOnFirstError.isSelected());
        }
        else super.actionPerformed(e);
    }
    
    @Override
    protected void drawMenu()
    {
        super.drawMenu();
        
        // add an extra option to close the work flow while preserving its contents
        super.menu.add(menuStopOnFirstError);
    }
    
    @Override
    protected void drawContent()
    {
        innerLoopPanel.removeAll();
        
        int nextRow = 0;
        
        // add batch elements (if any) to the loop panel
        if (blockDesc.getBlock() instanceof Batch) nextRow = drawBatchVariables(nextRow);
        
        // add other loop variables
        for (Var<?> loopVariable : loop.getLoopVariables())
        {
            // don't re-draw batch elements again...
            if (blockDesc.getBlock() instanceof Batch)
            {
                Batch batch = (Batch) blockDesc.getBlock();
                if (loopVariable == batch.getBatchSource() || loopVariable == batch.getBatchElement()) continue;
            }
            
            nextRow = drawLoopVariable(nextRow, loopVariable);
        }
        
        // add the loop panel to the main container
        GridBagConstraints gbc_loop = new GridBagConstraints();
        gbc_loop.anchor = GridBagConstraints.NORTHWEST;
        jPanelContent.add(innerLoopPanel, gbc_loop);
        
        nextRow = drawExposedLinks(nextRow);
        
        drawInnerWorkFlowContainer(nextRow + 1);
    }
    
    private int drawBatchVariables(int row)
    {
        Batch batch = (Batch) blockDesc.getBlock();
        
        // Batch variables (source and element) are drawn in a peculiar way:
        // [source drop zone] [source label] [source editor] [element drag zone]
        
        // create drop zone and editor for the batch source
        Var<?> batchSource = batch.getBatchSource();
        if (!varDropZones.containsKey(batchSource)) varDropZones.put(batchSource, createVarDropZone(batchSource, true, DragDropZone.LINK_RIGHT));
        if (!varEditors.containsKey(batchSource)) varEditors.put(batchSource, createVarEditor(batchSource));
        
        // create drag zone for the batch element
        Var<?> batchElement = batch.getBatchElement();
        if (!varDragZones.containsKey(batchElement)) varDragZones.put(batchElement, createVarDragZone(batchElement, false));
        
        // Draw the first row in a particular way (source => elements)
        
        GridBagConstraints gbc_dropZone = new GridBagConstraints();
        gbc_dropZone.anchor = GridBagConstraints.WEST;
        gbc_dropZone.fill = GridBagConstraints.NONE;
        gbc_dropZone.insets = new Insets(2, 0, 0, 0);
        gbc_dropZone.gridx = 0;
        gbc_dropZone.gridy = row;
        innerLoopPanel.add(varDropZones.get(batchSource), gbc_dropZone);
        
        GridBagConstraints gbc_inputName = new GridBagConstraints();
        gbc_inputName.anchor = GridBagConstraints.CENTER;
        gbc_inputName.fill = GridBagConstraints.HORIZONTAL;
        gbc_inputName.insets = new Insets(2, 5, 0, 0);
        gbc_inputName.gridx = 1;
        gbc_inputName.gridy = row;
        gbc_inputName.weightx = 1;
        JLabel inputName = new JLabel(batchSource.getName());
        Dimension inputNameDim = inputName.getPreferredSize();
        inputNameDim.height = 20;
        inputName.setPreferredSize(inputNameDim);
        innerLoopPanel.add(inputName, gbc_inputName);
        
        GridBagConstraints gbc_inputComponent = new GridBagConstraints();
        gbc_inputComponent.anchor = GridBagConstraints.CENTER;
        gbc_inputComponent.fill = GridBagConstraints.HORIZONTAL;
        gbc_inputComponent.insets = new Insets(2, 5, 0, 0);
        gbc_inputComponent.gridx = 2;
        gbc_inputComponent.gridy = row;
        gbc_inputComponent.weightx = 1;
        JComponent inputEditor = (JComponent) varEditors.get(batchSource).getEditorComponent();
        inputEditor.setOpaque(false);
        inputEditor.setFocusable(false);
        inputEditor.setPreferredSize(varEditors.get(batchSource).getPreferredSize());
        innerLoopPanel.add(inputEditor, gbc_inputComponent);
        
        GridBagConstraints gbc_dragZone = new GridBagConstraints();
        gbc_dragZone.anchor = GridBagConstraints.EAST;
        gbc_dragZone.fill = GridBagConstraints.NONE;
        gbc_dragZone.insets = new Insets(2, 5, 0, 0);
        gbc_dragZone.gridx = 3;
        gbc_dragZone.gridy = row;
        innerLoopPanel.add(varDragZones.get(batchElement), gbc_dragZone);
        
        return row + 1;
    }
    
    private int drawLoopVariable(int startRow, Var<?> loopVar)
    {
        boolean isLoopInput = loop.getBlockDescriptor().inputVars.contains(loopVar);
        
        if (!varDropZones.containsKey(loopVar) && isLoopInput) varDropZones.put(loopVar, createVarDropZone(loopVar, true, DragDropZone.LINK_RIGHT));
        if (!varEditors.containsKey(loopVar)) varEditors.put(loopVar, createVarEditor(loopVar));
        if (!varDragZones.containsKey(loopVar)) varDragZones.put(loopVar, createVarDragZone(loopVar, true));
        
        if (isLoopInput)
        {
            GridBagConstraints gbc_dropZone = new GridBagConstraints();
            gbc_dropZone.anchor = GridBagConstraints.WEST;
            gbc_dropZone.fill = GridBagConstraints.NONE;
            gbc_dropZone.insets = new Insets(2, 0, 0, 0);
            gbc_dropZone.gridx = 0;
            gbc_dropZone.gridy = startRow;
            innerLoopPanel.add(loopVar.isEnabled() ? varDropZones.get(loopVar) : new JLabel("   "), gbc_dropZone);
        }
        
        GridBagConstraints gbc_inputName = new GridBagConstraints();
        gbc_inputName.anchor = GridBagConstraints.CENTER;
        gbc_inputName.fill = GridBagConstraints.HORIZONTAL;
        gbc_inputName.insets = new Insets(2, 5, 0, 0);
        gbc_inputName.gridx = 1;
        gbc_inputName.gridy = startRow;
        gbc_inputName.weightx = 1;
        JLabel inputName = new JLabel(loopVar.getName());
        Dimension inputNameDim = inputName.getPreferredSize();
        inputNameDim.height = 20;
        inputName.setPreferredSize(inputNameDim);
        innerLoopPanel.add(inputName, gbc_inputName);
        
        GridBagConstraints gbc_inputComponent = new GridBagConstraints();
        gbc_inputComponent.anchor = GridBagConstraints.CENTER;
        gbc_inputComponent.fill = GridBagConstraints.HORIZONTAL;
        gbc_inputComponent.insets = new Insets(2, 5, 0, 0);
        gbc_inputComponent.gridx = 2;
        gbc_inputComponent.gridy = startRow;
        gbc_inputComponent.weightx = 1;
        JComponent inputEditor = (JComponent) varEditors.get(loopVar).getEditorComponent();
        inputEditor.setOpaque(inputEditor instanceof JTextComponent);
        inputEditor.setFocusable(true);
        inputEditor.setEnabled(inputEditor instanceof JLabel || loopVar.getReference() == null);
        inputEditor.setPreferredSize(varEditors.get(loopVar).getPreferredSize());
        innerLoopPanel.add(inputEditor, gbc_inputComponent);
        
        GridBagConstraints gbc_dragZone = new GridBagConstraints();
        gbc_dragZone.anchor = GridBagConstraints.EAST;
        gbc_dragZone.fill = GridBagConstraints.NONE;
        gbc_dragZone.insets = new Insets(2, 5, 0, 0);
        gbc_dragZone.gridx = 3;
        gbc_dragZone.gridy = startRow;
        innerLoopPanel.add(varDragZones.get(loopVar), gbc_dragZone);
        
        return startRow + 1;
    }
    
    @Override
    public Point getDragZoneLocation(Var<?> output)
    {
        if (blockDesc.isCollapsed()) return super.getDragZoneLocation(output);
        
        if (loop.isLoopVariable(output))
        {
            DragDropZone dz = varDragZones.get(output);
            int x = dz.getLocationOnScreen().x - innerFlowPane.getLocationOnScreen().x;
            int y = dz.getLocationOnScreen().y - innerFlowPane.getLocationOnScreen().y;
            return new Point(x + dz.getWidth(), y + dz.getHeight() / 2);
        }
        
        return super.getDragZoneLocation(output);
    }
    
    @Override
    public int getVarPanelWidth()
    {
        return innerLoopPanel.getWidth();
    }
    
    @Override
    public int getVarPanelHeight()
    {
        // return innerLoopPanel.getHeight() + 2;
        return getHeight();
    }
    
    public void drawPanel()
    {
        super.drawPanel();
        if (blockDesc.isCollapsed())
        {
            for (Component c : innerLoopPanel.getComponents())
                if (c instanceof DragZone<?>) c.setPreferredSize(new Dimension());
            
            innerLoopPanel.setSize(innerLoopPanel.getPreferredSize());
            varBox.add(innerLoopPanel);
            Dimension di = varBox.getPreferredSize();
            varBox.setPreferredSize(new Dimension(di.width, di.height + innerLoopPanel.getHeight()));
            
            setSize(getPreferredSize());
        }
        else for (Component c : innerLoopPanel.getComponents())
            if (c instanceof DragZone<?>) c.setPreferredSize(new Dimension(DragDropZone.DEFAULT_ICON_SIZE, DragDropZone.DEFAULT_ICON_SIZE));
    }
    
    @Override
    void dispose()
    {
        menuStopOnFirstError.removeActionListener(this);
        
        super.dispose();
    }
}
