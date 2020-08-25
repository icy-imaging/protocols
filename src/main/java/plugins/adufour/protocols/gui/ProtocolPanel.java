package plugins.adufour.protocols.gui;

import icy.file.FileUtil;
import icy.file.Saver;
import icy.gui.component.button.IcyButton;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.system.IcyHandledException;
import icy.system.thread.ThreadUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import org.w3c.dom.Document;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.util.BlockListener;
import plugins.adufour.blocks.util.BlocksException;
import plugins.adufour.blocks.util.BlocksFinder;
import plugins.adufour.blocks.util.BlocksML;
import plugins.adufour.blocks.util.BlocksReloadedException;
import plugins.adufour.blocks.util.WorkFlowListener;
import plugins.adufour.protocols.Protocols;
import plugins.adufour.protocols.gui.block.WorkFlowContainer;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarListener;

import com.ochafik.io.JTextAreaOutputStream;

@SuppressWarnings("serial")
public class ProtocolPanel extends JPanel implements WorkFlowListener, PropertyChangeListener
{
    private final WorkFlowContainer     workFlowContainer;
    
    private final JScrollPane           mainScrollPane;
    
    private final JSplitPane            splitPane;
    
    private final JPanel                logPanel;
    
    private final JTextAreaOutputStream logStream;
    
    private final VarString             statusMessage = new VarString("status", "status");
    
    private final VarListener<String>   statusListener;
    
    private WorkFlow                    workFlow;
    
    private boolean                     xmlDirty      = false;
    
    private File                        xmlFile;
    
    public ProtocolPanel(MainFrame frame)
    {
        this(null, new WorkFlow(true), frame);
    }
    
    private ProtocolPanel(File file, final WorkFlow workFlow, MainFrame frame)
    {
        // Prepare the log panel
        JTextArea logArea = new JTextArea();
        logArea.setOpaque(false);
        logStream = new JTextAreaOutputStream(logArea);
        
        logPanel = new JPanel(new BorderLayout());
        logPanel.setOpaque(false);
        logPanel.add(new JLabel("Execution log", JLabel.CENTER), BorderLayout.NORTH);
        JScrollPane logScrollPane = new JScrollPane(logArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        this.xmlFile = file;
        
        setWorkFlow(workFlow);
        
        this.workFlowContainer = new WorkFlowContainer(workFlow, true);
        this.workFlowContainer.addPropertyChangeListener(WORKFLOW_REPLACED, this);
        
        this.mainScrollPane = new JScrollPane(this.workFlowContainer);
        setLayout(new BorderLayout());
        
//        add(mainScrollPane, BorderLayout.CENTER);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, mainScrollPane, logPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(400);
        splitPane.getRightComponent().setMinimumSize(new Dimension());
        splitPane.setResizeWeight(1.0);
        add(splitPane, BorderLayout.CENTER);
        
        final JLabel workFlowStatusLabel = new JLabel(" ", JLabel.RIGHT);
        
        add(workFlowStatusLabel, BorderLayout.SOUTH);
        
        statusListener = new VarListener<String>()
        {
            @Override
            public void valueChanged(Var<String> source, String oldValue, final String newValue)
            {
                ThreadUtil.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        workFlowStatusLabel.setText(newValue);
                    }
                });
            }
            
            @Override
            public void referenceChanged(Var<String> source, Var<? extends String> oldReference, Var<? extends String> newReference)
            {
                
            }
        };
        
        BlocksML.getInstance().addStatusListener(statusListener);
        statusMessage.addListener(statusListener);
        
        ThreadUtil.invokeLater(new Runnable()
        {
            public void run()
            {
                splitPane.setDividerLocation(1.0);
            }
        }, true);
    }
    
    @Override
    public void blockAdded(WorkFlow source, BlockDescriptor addedBlock)
    {
        setDirty(true);
    }
    
    @Override
    public void blockCollapsed(WorkFlow source, BlockDescriptor block, boolean collapsed)
    {
        setDirty(true);
    }
    
    @Override
    public void blockDimensionChanged(WorkFlow source, BlockDescriptor block, int newWidth, int newHeight)
    {
        setDirty(true);
    }
    
    @Override
    public void blockLocationChanged(WorkFlow source, BlockDescriptor block, int newX, int newY)
    {
        setDirty(true);
    }
    
    @Override
    public void blockStatusChanged(WorkFlow source, BlockDescriptor block, BlockStatus status)
    {
        
    }
    
    @Override
    public void blockVariableAdded(WorkFlow source, BlockDescriptor block, Var<?> variable)
    {
        setDirty(true);
    }
    
    @Override
    public <T> void blockVariableChanged(WorkFlow source, BlockDescriptor block, Var<T> variable, T newValue)
    {
        // Mark the protocol as changed if a user input has changed
        if (block.inputVars.contains(variable) && variable.getReference() == null)
        {
            setDirty(true);
        }
    }
    
    @Override
    public void blockRemoved(WorkFlow source, BlockDescriptor removedBlock)
    {
        setDirty(true);
    }
    
    @Override
    public void linkAdded(WorkFlow source, Link<?> addedLink)
    {
        setDirty(true);
    }
    
    @Override
    public void linkRemoved(WorkFlow source, Link<?> removedLink)
    {
        setDirty(true);
    }
    
    public File getFile()
    {
        return xmlFile;
    }
    
    public WorkFlow getWorkFlow()
    {
        return workFlow;
    }
    
    public boolean isDirty()
    {
        return xmlDirty;
    }
    
    public boolean isEmpty()
    {
        return workFlow.size() == 0;
    }
    
    public void setFile(File file)
    {
        this.xmlFile = file;
    }
    
    void setDirty(boolean dirty)
    {
        firePropertyChange(WORKFLOW_MODIFIED, this.xmlDirty, dirty);
        this.xmlDirty = dirty;
    }
    
    /**
     * Saves the protocol to a XML file on disk. If no file was currently known for this protocol, a
     * dialog appears to ask for one
     * 
     * @param askForNewFile
     *            set to true if the protocol should be saved to a new file (i.e. "save as")
     * @return true if the saving operation was successful, false if canceled
     * @throws BlocksException
     *             if an error occurred while saving to disk
     */
    public boolean saveToDisk() throws BlocksException
    {
        if (xmlFile == null)
        {
            JFileChooser jfc = new JFileChooser(Protocols.getDefaultProtocolFolder());
            
            jfc.setFileFilter(BlocksML.XML_FILE_FILTER);
            jfc.setDialogTitle("Save an Icy protocol...");
            
            if (jfc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
            
            File file = jfc.getSelectedFile();
            
            if (file == null) return false; // no file selected
                
            Protocols.setDefaultProtocolFolder(file.getParent());
            
            // add extension if omitted
            String name = file.getPath();
            
            if (!name.toLowerCase().endsWith(".xml") && !name.toLowerCase().endsWith(".protocol"))
            {
                file = new File(file.getAbsolutePath() + ".protocol");
            }
            
            if (file.exists())
            {
                int option = JOptionPane.showConfirmDialog(this, file.getPath() + " already exists. Overwrite ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
                if (option != JOptionPane.OK_OPTION) return false;
            }
            
            xmlFile = file;
        }
        
        boolean success = saveToDisk(xmlFile);
        
        if (success)
        {
            setDirty(false);
        }
        
        return success;
    }
    
    /**
     * Saves the protocol to the specified file, regardless of the local file name
     * 
     * @param f
     *            the file to write to
     * @return true if the saving operation was successful, false if canceled
     * @throws BlocksException
     *             if an error occurred while saving to disk
     */
    public boolean saveToDisk(File f) throws BlocksException
    {
        if (f == null) return false;
        
        try
        {
            BlocksML.getInstance().saveWorkFlow(workFlow, f);
        }
        catch (IOException e)
        {
            throw new IcyHandledException(e.getMessage());
        }
        
        File snapshotFile = new File(FileUtil.setExtension(f.getPath(), "_screenshot.png"));
        
        try
        {
            Saver.saveImage(snapshot(), snapshotFile, true);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to save the snapshot. Reason: " + e.getLocalizedMessage());
        }
        
        return true;
    }
    
    /**
     * Takes a visual snapshot of the current protocol and displays the generated image
     */
    public IcyBufferedImage snapshot()
    {
        BufferedImage image = new BufferedImage(workFlowContainer.getWidth(), workFlowContainer.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = image.createGraphics();
        
        workFlowContainer.paint(g2d);
        
        Rectangle bounds = workFlowContainer.getContentsBoundingBox();
        
        IcyBufferedImage icyImage = IcyBufferedImage.createFrom(image);
        
        return IcyBufferedImageUtil.getSubImage(icyImage, bounds.x, bounds.y, bounds.width, bounds.height);
    }
    
    @Override
    public void statusChanged(WorkFlow source, String message)
    {
        statusMessage.setValue(message);
    }
    
    @Override
    public void workFlowReordered(WorkFlow source)
    {
        setDirty(true);
    }
    
    public WorkFlowContainer getWorkFlowContainer()
    {
        return workFlowContainer;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName() == WORKFLOW_REPLACED)
        {
            setWorkFlow((WorkFlow) evt.getNewValue());
        }
    }
    
    private void setWorkFlow(WorkFlow workFlow)
    {
        if (this.workFlow != null)
        {
            this.workFlow.removeListener(this);
            setDirty(true);
        }
        this.workFlow = workFlow;
        this.workFlow.addListener(this);
        this.workFlow.setLogStream(new PrintStream(logStream));
    }
    
    public void dispose()
    {
        BlocksML.getInstance().removeStatusListener(statusListener);
        statusMessage.removeListeners();
        workFlow.removeListener(this);
        workFlowContainer.removePropertyChangeListener(this);
        workFlowContainer.dispose();
        mainScrollPane.removeAll();
        removeAll();
    }
    
    public void showBlocksPopupMenu(Component invoker)
    {
        JPopupMenu blocksPopupMenu = new JPopupMenu();
        new BlocksFinder().createJMenu(blocksPopupMenu, workFlowContainer, new Point());
        blocksPopupMenu.show(invoker, 0, invoker.getHeight());
    }
    
    public void showBlocksEmbedMenu(Component invoker)
    {
        JPopupMenu embedPopupMenu = new JPopupMenu();
        // new BlocksFinder().createEmbedJMenu(embedPopupMenu, container);
        
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        Container c = (Container) focusOwner;
        WorkFlowContainer wfc;
        if (!(c instanceof JLayeredPane))
        {
            workFlowContainer.requestFocus();
            wfc = workFlowContainer;
        }
        else wfc = (WorkFlowContainer) c;
        
        BlocksFinder.createEmbedJMenu(embedPopupMenu, wfc);
        
        embedPopupMenu.show(invoker, 0, invoker.getHeight());
    }
    
    public void updateRunButton(IcyButton buttonRun)
    {
        switch (workFlow.getBlockDescriptor().getStatus())
        {
        case RUNNING:
            buttonRun.setIcon(new IcyIcon(ResourceUtil.getAlphaIconAsImage("playback_stop.png")));
            break;
        default:
            buttonRun.setIcon(new IcyIcon(ResourceUtil.getAlphaIconAsImage("playback_play.png")));
        }
    }
    
    /**
     * Load the specified work flow into the protocol panel, and force its dirty status
     * 
     * @param xml
     * @param isDirty
     * @throws BlocksException
     */
    public void loadWorkFlow(Document xml, final boolean isDirty) throws BlocksException, BlocksReloadedException
    {
        BlocksML.getInstance().loadWorkFlow(xml, workFlow);
        
        ThreadUtil.invokeLater(new Runnable()
        {
            public void run()
            {
                repaint();
                setDirty(isDirty);
            }
        }, true);
    }
    
    public void addBlockListener(BlockListener blockListener)
    {
        workFlow.getBlockDescriptor().addBlockListener(blockListener);
    }
    
    public void removeBlockListener(BlockListener blockListener)
    {
        workFlow.getBlockDescriptor().removeBlockListener(blockListener);
    }
}
