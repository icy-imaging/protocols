package plugins.adufour.protocols.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import icy.gui.component.button.IcyButton;
import icy.gui.component.button.IcyToggleButton;
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons;
import org.w3c.dom.Document;

import icy.common.listener.AcceptListener;
import icy.file.Loader;
import icy.gui.component.CloseableTabbedPane;
import icy.gui.component.CloseableTabbedPane.CloseableTabbedPaneListener;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginInstaller;
import icy.plugin.PluginLoader;
import icy.system.FileDrop;
import icy.system.FileDrop.FileDropListener;
import icy.system.IcyExceptionHandler;
import icy.system.IcyHandledException;
import icy.system.thread.ThreadUtil;
import icy.util.XMLUtil;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.util.BlockListener;
import plugins.adufour.blocks.util.BlocksException;
import plugins.adufour.blocks.util.BlocksML;
import plugins.adufour.blocks.util.BlocksReloadedException;
import plugins.adufour.blocks.util.LinkCutException;
import plugins.adufour.blocks.util.NoSuchVariableException;
import plugins.adufour.protocols.Protocols;
import plugins.adufour.protocols.gui.block.WorkFlowContainer;
import plugins.adufour.vars.lang.Var;

public class MainFrame extends IcyFrame implements IcyFrameListener, ActionListener, CloseableTabbedPaneListener, AcceptListener, ChangeListener, BlockListener, FileDropListener {
    private static final String UNTITLED = "untitled";

    private final JToolBar toolBar;

    private final IcyButton bNew = new IcyButton("New", GoogleMaterialDesignIcons.NOTE_ADD);
    private final IcyButton bLoad = new IcyButton("Load", GoogleMaterialDesignIcons.FOLDER_OPEN);
    private final IcyButton bSave = new IcyButton("Save", GoogleMaterialDesignIcons.SAVE);
    private final IcyButton bSaveAs = new IcyButton("Save as", GoogleMaterialDesignIcons.FILE_DOWNLOAD);
    private final IcyButton bEmbed = new IcyButton("Embed", GoogleMaterialDesignIcons.OPEN_IN_NEW);
    private final IcyToggleButton bRun = new IcyToggleButton("Run", GoogleMaterialDesignIcons.PLAY_ARROW, GoogleMaterialDesignIcons.STOP);
    private final IcyButton bClean = new IcyButton("Reset", GoogleMaterialDesignIcons.RESTORE);

    private final CloseableTabbedPane closeableTabbedPane = new CloseableTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

    private final ArrayList<ProtocolPanel> protocolPanels = new ArrayList<>();

    private final BlockSearchPanel blockSearchPanel = new BlockSearchPanel();

    private final JSplitPane splitPane;

    // Copy-paste support
    private static final HashMap<BlockDescriptor, BlockDescriptor> blocksClipBoard = new HashMap<>();
    private static final HashMap<BlockDescriptor, BlockDescriptor> blocksClipBoardBackup = new HashMap<>();
    private static final ArrayList<Link<?>> linksClipBoard = new ArrayList<>();
    private static final ArrayList<Link<?>> linksClipBoardBackup = new ArrayList<>();

    private static final String LINKS_CUT_MSG = "Some links will not be copied. Continue?";

    private final Protocols pluginInstance;

    private boolean isFrameClosing = false;

    public MainFrame(Protocols pluginInstance) {
        super("Protocols editor (" + pluginInstance.getFriendlyVersion() + ")", true, true, true, true, true);

        this.pluginInstance = pluginInstance;

        // handle close event locally
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addFrameListener(this);

        setPreferredSize(new Dimension(800, 640));

        // Menus

        toolBar = new JToolBar("Blocks toolbar", SwingConstants.HORIZONTAL);

        getContentPane().add(toolBar, BorderLayout.NORTH);

        for (IcyButton button : new IcyButton[]{bNew, bLoad, bSave, bSaveAs, bEmbed, bClean})
            button.addActionListener(this);

        for (IcyToggleButton button : new IcyToggleButton[]{bRun})
            button.addActionListener(this);

        bNew.setToolTipText("Create a new empty protocol");
        bLoad.setToolTipText("Load a protocol from a file");
        bSave.setToolTipText("Save the current protocol");
        bSaveAs.setToolTipText("Save the protocol to a new file");
        bEmbed.setToolTipText(
                "Embed the current selection into\r\n" +
                "a new work flow or a batch"
        );
        bRun.setToolTipText("Run the current protocol");
        bClean.setToolTipText(
                "Clear all intermediate results\r\n" +
                "(may help saving memory)"
        );

        toolBar.add(bNew);
        toolBar.add(bLoad);
        toolBar.add(bSave);
        toolBar.add(bSaveAs);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(bEmbed);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(bRun);
        toolBar.add(bClean);

        closeableTabbedPane.addCloseableTabbedPaneListener(this);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, blockSearchPanel, closeableTabbedPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        splitPane.setUI(new MySplitPaneUI());
        getContentPane().add(splitPane, BorderLayout.CENTER);

        getContentPane().addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent key) {
                if ((key.getModifiersEx() & Protocols.MENU_SHORTCUT_KEY) == Protocols.MENU_SHORTCUT_KEY
                        && key.getComponent() instanceof WorkFlowContainer) {
                    WorkFlow wf = ((WorkFlowContainer) key.getComponent()).getWorkFlow();

                    switch (key.getKeyCode()) {
                        case KeyEvent.VK_C:
                            copySelection(wf, false);
                            break;
                        case KeyEvent.VK_V:
                            pasteSelection(wf, false);
                            break;
                        case KeyEvent.VK_X:
                            copySelection(wf, false);
                            deleteSelection(wf);
                            break;
                        case KeyEvent.VK_BACK_SPACE:
                        case KeyEvent.VK_DELETE:
                            deleteSelection(wf);
                    }

                    key.getComponent().requestFocus(); // enable successive operations
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
            }
        });

        Icy.getMainInterface().addCanExitListener(this);

        pack();
    }

    public static void copySelection(WorkFlow wf, boolean embedding) throws LinkCutException {
        // do not modify clip-board when embedding/dis-embedding
        if (embedding) {
            // blocksClipBoardBackup = new HashMap<BlockDescriptor,
            // BlockDescriptor>(blocksClipBoard);
            // linksClipBoardBackup = new ArrayList<Link<?>>(linksClipBoard);
            blocksClipBoardBackup.clear();
            blocksClipBoardBackup.putAll(blocksClipBoard);
            linksClipBoardBackup.clear();
            linksClipBoardBackup.addAll(linksClipBoard);
        }

        blocksClipBoard.clear();
        linksClipBoard.clear();

        // copy the whole workflow
        if (wf.getBlockSelection().isEmpty()) {
            for (BlockDescriptor bd : wf)
                blocksClipBoard.put(bd, bd.clone(embedding));
            for (Link<?> l : wf.getLinksIterator())
                linksClipBoard.add(l);
        }
        // copy the selection
        else {
            for (BlockDescriptor bd : wf.getBlockSelection())
                blocksClipBoard.put(bd, bd.clone(embedding));
            linksClipBoard.addAll(wf.getLinkSelection());
        }

        if (embedding && linkCut(wf.getLinksIterator()))
            throw new LinkCutException(LINKS_CUT_MSG);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void pasteSelection(WorkFlow wf, boolean embedding) {
        for (BlockDescriptor bd : blocksClipBoard.keySet()) {
            // avoid getting the selection back in the container
            if (bd.getBlock() instanceof WorkFlow)
                ((WorkFlow) blocksClipBoard.get(bd).getBlock()).newSelection();

            wf.addBlock(blocksClipBoard.get(bd));
        }

        for (Link<?> l : linksClipBoard) {
            Link clone = linkClone(wf, l);

            if (clone == null) {
                System.err.println("Warning: cannot copy a link to an exposed variable");
                continue;
            }

            wf.addLink(clone.srcBlock, clone.srcVar, clone.dstBlock, clone.dstVar);
        }

        // re-copy the first copy
        // avoid pasting an altered state of the block
        blocksClipBoard.replaceAll((b, v) -> blocksClipBoard.get(b).clone(embedding));

        if (embedding) {
            // restore the backup
            // blocksClipBoard = new HashMap<BlockDescriptor,
            // BlockDescriptor>(blocksClipBoardBackup);
            // linksClipBoard = new ArrayList<Link<?>>(linksClipBoardBackup);
            blocksClipBoard.clear();
            blocksClipBoard.putAll(blocksClipBoardBackup);
            linksClipBoard.clear();
            linksClipBoard.addAll(linksClipBoardBackup);
        }
    }

    private static void deleteSelection(WorkFlow wf) {
        if (wf.getBlockSelection().isEmpty()) {
            ArrayList<Link<?>> tmp = new ArrayList<>();
            for (Link<?> l : wf.getLinksIterator())
                tmp.add(l); // avoid concurrent modification exception
            for (Link<?> l : tmp)
                wf.removeLink(l.dstVar);

            for (int i = 0; i < wf.size(); )
                wf.removeBlock(wf.getBlock(i), false);
        }
        else {
            for (Link<?> l : wf.getLinkSelection())
                wf.removeLink(l.dstVar);

            // avoid infinite loop when canceling
            ArrayList<BlockDescriptor> tmp = new ArrayList<>(wf.getBlockSelection());
            for (BlockDescriptor bd : tmp)
                wf.removeBlock(bd, false);
        }
    }

    private static boolean linkCut(Iterable<Link<?>> links) {
        for (Link<?> l : links)
            if (blocksClipBoard.containsKey(l.dstBlock) ^ blocksClipBoard.containsKey(l.srcBlock))
                return true;
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Link<?> linkClone(WorkFlow wf, Link<?> l) {
        Var<?> newSrc, newDst;

        try {
            // get link input from block inputs
            newSrc = blocksClipBoard.get(l.srcBlock).inputVars.get(l.srcBlock.inputVars.getID(l.srcVar));
            newDst = blocksClipBoard.get(l.dstBlock).inputVars.get(l.dstBlock.inputVars.getID(l.dstVar));
        }
        catch (NoSuchVariableException nsve) {
            // get link input from block outputs
            newSrc = blocksClipBoard.get(l.srcBlock).outputVars.get(l.srcBlock.outputVars.getID(l.srcVar));
            newDst = blocksClipBoard.get(l.dstBlock).inputVars.get(l.dstBlock.inputVars.getID(l.dstVar));
        }

        // if the link contains an exposed variable of a work flow, newSrc or newDst == null
        // (because the ID of the cloned variable is different in the cloned work flow)
        if (newSrc == null || newDst == null) {
            return null;
        }

        return new Link(wf, blocksClipBoard.get(l.srcBlock), newSrc, blocksClipBoard.get(l.dstBlock), newDst);
    }

    public void addProtocolPane(ProtocolPanel protocol) {
        getProtocolPanels().add(protocol);
        protocol.addPropertyChangeListener(ProtocolPanel.WORKFLOW_MODIFIED, this);
        protocol.addBlockListener(this);

        new FileDrop(protocol, false, this);

        if (protocol.getFile() != null) {
            File f = protocol.getFile();
            protocol.setName(f.getName());
            closeableTabbedPane.addTab(f.getName(), protocol);
            closeableTabbedPane.setToolTipTextAt(getProtocolPanels().size() - 1, f.getAbsolutePath());
        }
        else {
            protocol.setName(UNTITLED);
            closeableTabbedPane.addTab(UNTITLED, protocol);
        }

        closeableTabbedPane.setSelectedIndex(getProtocolPanels().size() - 1);
    }

    public ProtocolPanel getActiveProtocol() {
        int index = closeableTabbedPane.getSelectedIndex();
        return (index == -1) ? null : getProtocolPanels().get(index);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == bNew) {
            addProtocolPane(new ProtocolPanel(this));
        }
        else if (e.getSource() == bLoad) {
            loadWorkFlow(null);
        }
        else if (e.getSource() == bSave) {
            saveWorkFlow(getActiveProtocol(), true);
        }
        else if (e.getSource() == bSaveAs) {
            ProtocolPanel protocol = getActiveProtocol();

            if (protocol == null)
                return;

            // save the old file to restore it in case a problem occurs
            File oldFile = protocol.getFile();

            // erase the file to force an open file dialog
            protocol.setFile(null);

            if (!saveWorkFlow(protocol, true)) {
                // work flow was not saved => restore the old file
                protocol.setFile(oldFile);
            }
        }
        else if (e.getSource() == bRun) {
            ProtocolPanel protocol = getActiveProtocol();

            if (protocol == null)
                return;

            WorkFlow wf = protocol.getWorkFlow();
            if (wf.getBlockDescriptor().getStatus() == BlockStatus.RUNNING) {
                wf.interrupt();
            }
            else {
                wf.runWorkFlow();
            }
        }
        else if (e.getSource() == bEmbed) {
            ProtocolPanel panel = getActiveProtocol();
            if (panel != null)
                panel.showBlocksEmbedMenu(bEmbed);
        }
        else if (e.getSource() == bClean) {
            ProtocolPanel protocol = getActiveProtocol();

            if (protocol == null)
                return;

            WorkFlow wf = protocol.getWorkFlow();
            wf.reset();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ProtocolPanel.WORKFLOW_MODIFIED) && !isFrameClosing) {
            ProtocolPanel protocolPanel = (ProtocolPanel) evt.getSource();
            int index = getProtocolPanels().indexOf(protocolPanel);

            if (index == -1) {
                String message = "in method \"icy.adufour.protocols.Protocols.propertyChange(PropertyChangeEvent)\":\n";
                message += "A protocol was modified, but its panel is already closed (index = -1)";
                IcyExceptionHandler.handleException(pluginInstance.getDescriptor(),
                        new ArrayIndexOutOfBoundsException(message), true);
                return;
            }

            if (closeableTabbedPane.getTabCount() <= index)
                return;

            if (((Boolean) evt.getNewValue()).booleanValue()) {
                closeableTabbedPane.setTitleAt(index, "* " + closeableTabbedPane.getTitleAt(index));
            }
            else if (protocolPanel.getFile() != null) {
                closeableTabbedPane.setTitleAt(index, protocolPanel.getFile().getName());
            }
        }
        else {
            super.propertyChange(evt);
        }
    }

    @Override
    public void tabClosed(int index, String title) {
    }

    @Override
    public boolean tabClosing(int index, String title) {
        ProtocolPanel protocol = getProtocolPanels().get(index);
        if (protocol.isDirty() && !protocol.isEmpty() && !Protocols.isReloading()) {
            String name = protocol.getFile() == null ? UNTITLED : protocol.getFile().getName();
            int option = JOptionPane.showConfirmDialog(protocol,
                    name + " has not been saved.\nSave workflow before closing ?", "Confirmation",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (option == JOptionPane.CANCEL_OPTION)
                return false;

            if (option == JOptionPane.YES_OPTION && !saveWorkFlow(protocol, false))
                return false;
        }
        protocol.removePropertyChangeListener(this);
        protocol.removeBlockListener(this);
        protocol.dispose();

        // Remove drag'n'drop hooks
        FileDrop.remove(protocol);
        getProtocolPanels().remove(protocol);

        return true;
    }

    /**
     * @param protocol
     * @param updateTabName
     * @return true if the saving operation was successful, false if canceled
     * @throws BlocksException if an error occurred while saving to disk
     */
    private boolean saveWorkFlow(ProtocolPanel protocol, boolean updateTabName) throws BlocksException {
        if (protocol == null)
            return false;

        boolean success = protocol.saveToDisk();

        if (!success)
            return false;

        File savedFile = protocol.getFile();

        if (updateTabName)
            closeableTabbedPane.setTitleAt(closeableTabbedPane.getSelectedIndex(), savedFile.getName());

        return true;
    }

    private File getFile() {
        final JFileChooser jfc = new JFileChooser(Protocols.getDefaultProtocolFolder());

        jfc.setFileFilter(BlocksML.XML_FILE_FILTER);
        jfc.setDialogTitle("Load an Icy protocol...");

        if (jfc.showOpenDialog(getFrame()) != JFileChooser.APPROVE_OPTION)
            return null;

        return jfc.getSelectedFile();
    }

    /**
     * @return the protocolPanels
     */
    public ArrayList<ProtocolPanel> getProtocolPanels() {
        return protocolPanels;
    }

    /**
     * Loads the specified file as a new workflow
     *
     * @param file
     * @throws IllegalArgumentException if the file is not a protocol
     */
    public void loadWorkFlow(File file) throws IllegalArgumentException {
        if (file == null)
            file = getFile();

        if (file == null)
            return;

        final File workingFile = file;

        Protocols.setDefaultProtocolFolder(file.getParent());

        // check that the work flow is not already opened
        for (ProtocolPanel protocol : getProtocolPanels())
            if (file.equals(protocol.getFile()))
                return;

        // make sure the xml is well-formed
        final Document xml = XMLUtil.loadDocument(file);

        boolean isFileValid = false;

        if (xml != null) {
            String rootName = XMLUtil.getRootElement(xml).getNodeName();
            isFileValid = rootName.equalsIgnoreCase("protocol") || rootName.equalsIgnoreCase("workspace");
        }

        if (!isFileValid)
            throw new IcyHandledException(
                    "Error while loading \"" + file.getPath() + "\":\nThe selected file is not an Icy protocol.");

        final ProtocolPanel panel = new ProtocolPanel(this);
        panel.setFile(file);
        addProtocolPane(panel);

        ThreadUtil.bgRun(() -> {
            AnnounceFrame announce = new AnnounceFrame("Loading protocol " + workingFile.getName() + "...");

            try {
                PluginInstaller.waitInstall();
                PluginLoader.waitWhileLoading();

                ArrayList<PluginDescriptor> plugins = PluginLoader.getPlugins(Block.class, true, false, false);

                if (plugins.size() == 0)
                    throw new BlocksReloadedException();

                panel.loadWorkFlow(xml, false);
            }
            catch (BlocksReloadedException e) {
                pluginInstance.reload(xml, workingFile.getAbsolutePath());
            }
            catch (final BlocksException e) {
                ThreadUtil.invokeLater(() -> {
                    String message = e.getMessage() + ".\n\n";
                    message += "A bug was caught while loading the protocol.\n";
                    message += " - Yes: send the report and the protocol (recommended for diagnosis).\n";
                    message += " - No: send the report but not the protocol (recommended for privacy).\n";
                    message += " - Cancel: discard this message (nothing will be sent)";
                    int answer = JOptionPane.showConfirmDialog(getFrame(), message, "Couldn't load protocol",
                            JOptionPane.YES_NO_CANCEL_OPTION);

                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));

                    message += "\n\n" + sw;

                    switch (answer) {
                        case JOptionPane.YES_OPTION:

                            try {
                                message += "\n\n" + BlocksML.getInstance().toString(xml);
                            }
                            catch (TransformerFactoryConfigurationError | TransformerException e2) {
                                e2.printStackTrace();
                            }

                            //$FALL-THROUGH$ Send the report anyway
                        case JOptionPane.NO_OPTION:
                            IcyExceptionHandler.report(pluginInstance.getDescriptor(), message);
                            break;
                        default:
                            break;
                    }
                });
            }
            finally {
                announce.close();
            }
        });
    }

    @Override
    public void icyFrameOpened(IcyFrameEvent e) {
    }

    @Override
    public void icyFrameClosing(IcyFrameEvent e) {
        if (e.getFrame() == this) {
            isFrameClosing = true;

            if (!accept(this))
                isFrameClosing = false;
        }
    }

    @Override
    public void icyFrameClosed(IcyFrameEvent e) {
        Protocols.setReloading(false);
    }

    @Override
    public void icyFrameIconified(IcyFrameEvent e) {
    }

    @Override
    public void icyFrameDeiconified(IcyFrameEvent e) {
    }

    @Override
    public void icyFrameActivated(IcyFrameEvent e) {
    }

    @Override
    public void icyFrameDeactivated(IcyFrameEvent e) {
    }

    @Override
    public void icyFrameInternalized(IcyFrameEvent e) {
    }

    @Override
    public void icyFrameExternalized(IcyFrameEvent e) {
    }

    @Override
    public void filesDropped(File[] files) {
        for (File file : files) {
            try {
                loadWorkFlow(file);
            }
            catch (IllegalArgumentException e) {
                if (Loader.isSupportedImageFile(file.getPath()))
                    Loader.load(file.getPath(), true);
            }
        }
    }

    @Override
    public boolean accept(Object source) {
        while (closeableTabbedPane.getTabCount() > 0) {
            int tabCount = closeableTabbedPane.getTabCount();

            for (int i = 0; i < tabCount; i++) {
                if (closeableTabbedPane.getSelectedIndex() == i) {
                    boolean closeAccepted = tabClosing(i, null);

                    // if "cancel" is clicked for either tab, stop the closing process
                    if (!closeAccepted)
                        return false;

                    if (closeableTabbedPane.getTabCount() > i)
                        closeableTabbedPane.removeTabAt(i);

                    break;
                }
            }
        }

        for (Component component : toolBar.getComponents()) {
            if (component instanceof JButton)
                ((JButton) component).removeActionListener(this);

            toolBar.remove(component);
        }

        getContentPane().removeAll();

        removeFrameListener(this);

        // close manually

        setVisible(false);

        close();

        Icy.getMainInterface().removeCanExitListener(this);

        Protocols.close();

        return true;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ProtocolPanel panel = getActiveProtocol();

        if (e.getSource() == closeableTabbedPane && panel != null) {
            // tab was selected, added or removed
            // => update the status of the run button
            updateRunButton(getActiveProtocol().getWorkFlow().getBlockDescriptor().getStatus());
        }
    }

    private void updateRunButton(BlockStatus status) {
        switch (status) {
            case RUNNING:
                bRun.setSelected(true);
                break;
            default:
                bRun.setSelected(false);
        }
    }

    @Override
    public void blockCollapsed(BlockDescriptor block, boolean collapsed) {

    }

    @Override
    public void blockDimensionChanged(BlockDescriptor block, int newWidth, int newHeight) {
    }

    @Override
    public void blockLocationChanged(BlockDescriptor block, int newX, int newY) {
    }

    @Override
    public void blockStatusChanged(BlockDescriptor block, BlockStatus status) {
        updateRunButton(status);
    }

    @Override
    public void blockVariableAdded(BlockDescriptor block, Var<?> variable) {
    }

    @Override
    public <T> void blockVariableChanged(BlockDescriptor block, Var<T> variable, T newValue) {
    }

    private static class MySplitPaneUI extends BasicSplitPaneUI {
        private static class MySplitPaneDivider extends BasicSplitPaneDivider {

            public MySplitPaneDivider(BasicSplitPaneUI ui) {
                super(ui);
            }

            protected JButton createLeftOneTouchButton() {
                JButton button = super.createLeftOneTouchButton();
                button.setPreferredSize(new Dimension(20, 40));
                return button;
            }

            protected JButton createRightOneTouchButton() {
                JButton button = super.createRightOneTouchButton();
                button.setPreferredSize(new Dimension(20, 40));
                return button;
            }
        }

        public BasicSplitPaneDivider createDefaultDivider() {
            return new MySplitPaneDivider(this);
        }
    }
}
