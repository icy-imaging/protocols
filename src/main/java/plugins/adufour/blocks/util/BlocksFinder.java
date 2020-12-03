package plugins.adufour.blocks.util;

import icy.gui.menu.PluginMenuItem;
import icy.image.ImageUtil;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.Plugin;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.system.SystemUtil;
import icy.util.ClassUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.TransferHandler;

import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.Loop;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.tools.Display;
import plugins.adufour.blocks.tools.ReLoop;
import plugins.adufour.blocks.tools.ToolsBlock;
import plugins.adufour.blocks.tools.ij.IJBlock;
import plugins.adufour.blocks.tools.input.InputBlock;
import plugins.adufour.blocks.tools.io.IOBlock;
import plugins.adufour.blocks.tools.roi.ROIBlock;
import plugins.adufour.blocks.tools.sequence.SequenceBlock;
import plugins.adufour.protocols.gui.BlockSearchPanel;
import plugins.adufour.protocols.gui.block.WorkFlowContainer;

public class BlocksFinder
{
    /**
     * Indicates whether annotated methods should be parsed and made available as
     * blocks
     */
    public final boolean parseAnnotations = false;

    /**
     * class for handling Drag and Drop on the menu items
     * 
     * @author Ludovic Laborde, Alexandre Dufour
     */
    @SuppressWarnings("serial")
    public class DND_MenuItem extends PluginMenuItem
            implements MouseListener, Transferable, DragSourceListener, DragGestureListener
    {
        private DragSource source;
        private TransferHandler handler;
        private PluginDescriptor descriptor;

        private List<MenuItemListener> menuItemListeners;

        public DND_MenuItem(final PluginDescriptor d)
        {
            super(d);
            setOpaque(false);

            for (MouseListener listener : getMouseListeners())
                removeMouseListener(listener);

            descriptor = d;
            menuItemListeners = new ArrayList<MenuItemListener>();

            handler = new TransferHandler()
            {
                public Transferable createTransferable(JComponent c)
                {
                    return new DND_MenuItem(descriptor);
                }
            };

            source = new DragSource();
            source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, this);

            setTransferHandler(handler);
            addMouseListener(this);
        }

        public PluginDescriptor getDescriptor()
        {
            return descriptor;
        }

        @Override
        public void dragGestureRecognized(DragGestureEvent dge)
        {
            source.startDrag(dge, DragSource.DefaultMoveDrop, new DND_MenuItem(descriptor), this);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
        {
            return this;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            try
            {
                return new DataFlavor[] {
                        new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.ArrayList")};
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return true;
        }

        @Override
        public void mouseClicked(MouseEvent arg0)
        {
            setBackground(Color.GRAY);
            notifyListeners();
        }

        public void addMenuItemListener(MenuItemListener l)
        {
            menuItemListeners.add(l);
        }

        // updates the search panel
        private void notifyListeners()
        {
            for (MenuItemListener l : menuItemListeners)
                l.displayDoc(descriptor);
        }

        @Override
        public void dragDropEnd(DragSourceDropEvent dsde)
        {
            setBackground(Color.GRAY);
            notifyListeners();
        }

        @Override
        public void dragEnter(DragSourceDragEvent dsde)
        {
        }

        @Override
        public void dragExit(DragSourceEvent dse)
        {
        }

        @Override
        public void dragOver(DragSourceDragEvent dsde)
        {
        }

        @Override
        public void dropActionChanged(DragSourceDragEvent dsde)
        {
        }

        @Override
        public void mouseEntered(MouseEvent arg0)
        {
        }

        @Override
        public void mouseExited(MouseEvent arg0)
        {
        }

        @Override
        public void mousePressed(MouseEvent arg0)
        {
        }

        @Override
        public void mouseReleased(MouseEvent arg0)
        {
        }
    }

    /**
     * Searches for a block using the specified search text, and populates the
     * specified search panel with the search results
     * 
     * @param menuContainer BlockSearchPanel
     * @param searchText String
     */
    public void createSearchMenu(BlockSearchPanel menuContainer, String searchText)
    {
        ArrayList<PluginDescriptor> plugins = PluginLoader.getPlugins(Block.class, true, false, false);

        if (plugins.size() == 0)
        {
            JMenuItem item = new JMenuItem(
                    "The plug-in list has been updated.\nPlease close and re-open the Protocols editor.");
            item.setFont(item.getFont().deriveFont(Font.ITALIC));
            menuContainer.add(item);
            return;
        }

        for (PluginDescriptor descriptor : plugins)
        {

            Class<? extends Plugin> _class = descriptor.getPluginClass();
            final Class<? extends Block> blockClass = _class.asSubclass(Block.class);

            if (ClassUtil.isAbstract(blockClass))
                continue;
            if (ClassUtil.isPrivate(blockClass))
                continue;

            // ALEX (2014-06-17): removing annotations for now
            // // handle default annotated blocks defined in protocols
            // if (blockClass.isAnnotationPresent(BlockSearchAnnotation.class))
            // {
            // Annotation annotation =
            // blockClass.getAnnotation(BlockSearchAnnotation.class);
            // BlockSearchAnnotation blockSearchAnnotation = (BlockSearchAnnotation)
            // annotation;
            //
            // // search conditions
            // if
            // (!(blockSearchAnnotation.name().toLowerCase().contains(searchText.toLowerCase())
            // ||
            // blockSearchAnnotation.category().toLowerCase().contains(searchText.toLowerCase())
            // || blockSearchAnnotation.description().toLowerCase()
            // .contains(searchText.toLowerCase()))) continue;
            // }

            // handle other external plugins
            // search conditions

            String testString = "block";
            testString += descriptor.getName();
            testString += blockClass.getSimpleName();
            testString += descriptor.getDescription();
            testString += descriptor.getAuthor();
            testString = testString.toLowerCase();

            searchText = searchText.trim().toLowerCase();
            String searchTextNoSpaces = searchText.replace(" ", "");

            if (testString.contains(searchText) || testString.contains(searchTextNoSpaces))
            {
                DND_MenuItem menuItem = new DND_MenuItem(descriptor);
                menuItem.addMenuItemListener(menuContainer);
                menuItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
                menuItem.setAlignmentX(Component.LEFT_ALIGNMENT);

                String name = blockClass.getSimpleName();
                name = descriptor.getName().equalsIgnoreCase(name) ? getFlattened(name) : descriptor.getName();

                // Remove the "block" word at the end of the class name
                if (name.toLowerCase().endsWith("block"))
                    name = name.substring(0, name.length() - 5);

                if (blockClass.isAnnotationPresent(Deprecated.class))
                {
                    name = "(deprecated) " + name;
                }
                menuItem.setText(name);
                menuContainer.add(menuItem);
            }
        }
    }

    /**
     * Create a JMenu component with all existing classes implementing {@link Block}
     *
     * @param menuContainer Container
     * @param workFlowPane WorkFlowContainer
     * @param location Point
     */
    public final void createJMenu(Container menuContainer, final WorkFlowContainer workFlowPane, final Point location)
    {
        JMenu mnBlocks = new JMenu("Blocks...");
        mnBlocks.setIcon(new IcyIcon(ResourceUtil.getAlphaIconAsImage("box.png"), 22));

        JMenu mnInput = new JMenu("Read...");
        mnInput.setIcon(new IcyIcon(ResourceUtil.ICON_REDO, 22));

        JMenu mnIO = new JMenu("I/O...");
        mnIO.setIcon(new IcyIcon(ResourceUtil.ICON_SAVE, 22));

        JMenu mnSeq = new JMenu("Sequence...");
        mnSeq.setIcon(new IcyIcon(ResourceUtil.ICON_PHOTO, 22));

        JMenu mnROI = new JMenu("ROI...");
        mnROI.setIcon(new IcyIcon(ResourceUtil.ICON_ROI_POLYGON, 22));

        JMenu mnImageJ = new JMenu("ImageJ...");
        mnImageJ.setIcon(new IcyIcon(ResourceUtil.ICON_TOIJ, 22));

        JMenu mnLoops = new JMenu("Loop / Batch...");
        mnLoops.setIcon(new IcyIcon(ResourceUtil.ICON_RELOAD, 22));

        JMenu mnTools = new JMenu("Tools...");
        mnTools.setIcon(new IcyIcon(ResourceUtil.ICON_TOOLS, 22));

        ArrayList<PluginDescriptor> plugins = PluginLoader.getPlugins(Block.class, true, false, false);

        if (plugins.size() == 0)
        {
            JMenuItem item = new JMenuItem(
                    "The plug-in list has been updated.\nPlease close and re-open the Protocols editor.");
            item.setFont(item.getFont().deriveFont(Font.ITALIC));
            menuContainer.add(item);
            return;
        }

        for (PluginDescriptor descriptor : plugins)
        {
            Class<? extends Plugin> clazz = descriptor.getPluginClass();
            try
            {
                final Class<? extends Block> blockClass = clazz.asSubclass(Block.class);

                if (ClassUtil.isAbstract(blockClass))
                    continue;
                if (ClassUtil.isPrivate(blockClass))
                    continue;
                if (blockClass.isAnnotationPresent(Deprecated.class))
                    continue;

                // create the menu item
                PluginMenuItem menuItem = new PluginMenuItem(descriptor);

                // remove the internal action listener
                menuItem.removeActionListener(menuItem);

                String name = blockClass.getSimpleName();

                name = descriptor.getName().equalsIgnoreCase(name) ? getFlattened(name) : descriptor.getName();

                // Remove the "block" word at the end of the class name
                if (name.toLowerCase().endsWith("block"))
                    name = name.substring(0, name.length() - 5);

                menuItem.setText(name);

                if (!descriptor.getDescription().isEmpty())
                {
                    menuItem.setToolTipText(
                            "<html>" + descriptor.getDescription().replaceAll("\n", "<br/>") + "</html>");
                }

                menuItem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent arg0)
                    {
                        try
                        {
                            addBlock(workFlowPane, blockClass.newInstance(), location);
                        }
                        catch (InstantiationException e1)
                        {
                            e1.printStackTrace();
                        }
                        catch (IllegalAccessException e1)
                        {
                            e1.printStackTrace();
                        }
                    }
                });

                // place the menu item where appropriate

                if (blockClass == Display.class)
                {
                    // will be dealt with outside this loop
                    continue;
                }
                else if (ClassUtil.isSubClass(blockClass, Loop.class)
                        && !blockClass.isAnnotationPresent(Deprecated.class))
                {
                    final JMenuItem item = new JMenuItem(getFlattened(blockClass.getSimpleName()));
                    item.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent arg0)
                        {
                            try
                            {
                                addBlock(workFlowPane, blockClass.newInstance(), location);
                            }
                            catch (InstantiationException e1)
                            {
                                e1.printStackTrace();
                            }
                            catch (IllegalAccessException e1)
                            {
                                e1.printStackTrace();
                            }
                        }
                    });
                    mnLoops.add(item);
                }
                else if (InputBlock.class.isAssignableFrom(blockClass))
                {
                    mnInput.add(menuItem);
                }
                else if (SequenceBlock.class.isAssignableFrom(blockClass))
                {
                    mnSeq.add(menuItem);
                }
                else if (IOBlock.class.isAssignableFrom(blockClass))
                {
                    mnIO.add(menuItem);
                }
                else if (ROIBlock.class.isAssignableFrom(blockClass))
                {
                    mnROI.add(menuItem);
                }
                else if (IJBlock.class.isAssignableFrom(blockClass))
                {
                    mnImageJ.add(menuItem);
                }
                else if (ToolsBlock.class.isAssignableFrom(blockClass))
                {
                    if (blockClass == ReLoop.class)
                        continue; // TODO

                    mnTools.add(menuItem);
                }
                else
                {
                    // default case: put the rest in the "Blocks" menu
                    mnBlocks.add(menuItem);
                }

                if (parseAnnotations)
                {
                    // find annotated methods
                    ArrayList<BlockAnnotations> blocks = BlockAnnotations.findBlockMethods(clazz);

                    for (final BlockAnnotations annotatedMethod : blocks)
                    {
                        JMenuItem item2 = new JMenuItem(clazz.getSimpleName() + "." + annotatedMethod.getName());
                        item2.setToolTipText(annotatedMethod.getDescription());
                        item2.addActionListener(new ActionListener()
                        {
                            @Override
                            public void actionPerformed(ActionEvent e)
                            {
                                addBlock(workFlowPane, annotatedMethod.createBlock(), location);
                            }
                        });
                        mnBlocks.add(item2);
                    }
                }
            }
            catch (ClassCastException e1)
            {
            }
        }

        splitLongMenus(mnBlocks, 15);
        splitLongMenus(mnInput, 15);
        splitLongMenus(mnSeq, 15);
        splitLongMenus(mnROI, 15);
        splitLongMenus(mnIO, 15);
        splitLongMenus(mnImageJ, 15);
        splitLongMenus(mnLoops, 15);
        splitLongMenus(mnTools, 15);

        menuContainer.add(mnBlocks);
        menuContainer.add(mnInput);
        menuContainer.add(mnSeq);
        menuContainer.add(mnROI);
        menuContainer.add(mnIO);
        menuContainer.add(mnImageJ);
        menuContainer.add(mnLoops);
        menuContainer.add(mnTools);

        // and add the Display block last

        JMenuItem mnDisp = new JMenuItem("Display");
        mnDisp.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    addBlock(workFlowPane, Display.class.newInstance(), location);
                }
                catch (InstantiationException e1)
                {
                    e1.printStackTrace();
                }
                catch (IllegalAccessException e1)
                {
                    e1.printStackTrace();
                }
            }
        });
        mnDisp.setIcon(new IcyIcon(ImageUtil.scaleQuality(ResourceUtil.getAlphaIconAsImage("eye_open.png"), 22, 22)));
        menuContainer.add(mnDisp);

        // // 3rd-party block providers (very soon...)
        //
        // JMenu mnOthers = new JMenu("Others...");
        // mnOthers.setIcon(new IcyIcon(ResourceUtil.getAlphaIconAsImage("box.png"),
        // 22));
        //
        // for (PluginDescriptor providerPlugin :
        // PluginLoader.getPlugins(BlockProvider.class, true,
        // false, false))
        // {
        // try
        // {
        // BlockProvider provider =
        // providerPlugin.getPluginClass().asSubclass(BlockProvider.class).newInstance();
        //
        // Map<String, Block> blocks = provider.getBlocks();
        //
        // for(String blockName : blocks.keySet())
        // {
        // final Block block = blocks.get(blockName);
        // final JMenuItem item = new JMenuItem(blockName);
        // item.addActionListener(new ActionListener()
        // {
        // @Override
        // public void actionPerformed(ActionEvent arg0)
        // {
        // try
        // {
        // addBlock(workFlowPane, block.getClass().newInstance(), location);
        // }
        // catch (InstantiationException e1)
        // {
        // e1.printStackTrace();
        // }
        // catch (IllegalAccessException e1)
        // {
        // e1.printStackTrace();
        // }
        // }
        // });
        // mnOthers.add(item);
        // }
        // }
        // catch (Exception e)
        // {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // }
        // splitLongMenus(mnOthers, 15);
        // menuContainer.add(mnOthers);
    }

    /**
     * Create a JMenu component with all existing classes implementing {@link Block}
     *
     * @param menuContainer Container
     * @param workFlowPane WorkFlowContainer
     */
    public static void createEmbedJMenu(final Container menuContainer, final WorkFlowContainer workFlowPane)
    {
        // One day, when Java 8 rules...
        // PluginLoader.getPlugins(WorkFlow.class).stream()
        // // get the plug-in class
        // .map(PluginDescriptor::getPluginClass)
        // // Create a menu item for each element
        // .forEach(enclosure -> {
        // String menuName = getFlattened(enclosure.getSimpleName());
        // JMenuItem menuItem = new JMenuItem(menuName);
        // menuItem.addActionListener(a -> {
        // try
        // {
        // workFlowPane.embedWorkFlow((Class<? extends WorkFlow>) enclosure);
        // }
        // catch (Exception e)
        // {
        // throw new RuntimeException(e);
        // }
        // });
        // menuContainer.add(menuItem);
        // });

        // In the meantime...
        for (PluginDescriptor plugin : PluginLoader.getPlugins(WorkFlow.class))
        {
            JMenuItem menuItem = new JMenuItem(getFlattened(plugin.getName()));
            final PluginDescriptor pf = plugin;

            menuItem.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent arg0)
                {
                    try
                    {
                        workFlowPane.embedWorkFlow(pf.getPluginClass().asSubclass(WorkFlow.class));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            menuContainer.add(menuItem);
        }
    }

    private static void addBlock(WorkFlowContainer workFlowPane, Block block, Point location)
    {
        BlockDescriptor blockDesc = (block instanceof WorkFlow ? ((WorkFlow) block).getBlockDescriptor()
                : new BlockDescriptor(-1, block));

        blockDesc.setLocation(location.x, location.y);

        workFlowPane.getWorkFlow().addBlock(blockDesc);
    }

    /**
     * Breaks the list of items in the specified menu, by creating sub-menus
     * containing the specified number of items, and a "More..." menu to access
     * subsequent items.
     * 
     * @param menu
     *        the menu to break into smaller sub-menus
     * @param maxItemsPerMenu
     *        the maximum number of items to display in each sub-menu
     */
    private void splitLongMenus(JMenu menu, int maxItemsPerMenu)
    {
        ArrayList<Component> components = new ArrayList<Component>(Arrays.asList(menu.getPopupMenu().getComponents()));

        if (components.size() > maxItemsPerMenu)
        {
            menu.removeAll();

            JMenu currentMenu = menu;

            while (components.size() > 0)
            {
                int n = Math.min(components.size(), maxItemsPerMenu - 1);

                for (int i = 0; i < n; i++)
                    currentMenu.add(components.remove(0));

                if (components.size() > 0)
                    currentMenu = (JMenu) currentMenu.add(new JMenu("More..."));
            }

            if (components.size() > 0)
                throw new RuntimeException("Error while splitting menus: " + components.size() + " are remaining.");
        }

        // do this recursively for sub-menus
        for (Component component : menu.getPopupMenu().getComponents())
        {
            if (component instanceof JMenu)
                splitLongMenus((JMenu) component, maxItemsPerMenu);
        }
    }

    /**
     * Creates a flattened version of the provided String. The flattening operation
     * splits the string by inserting spaces between words starting with an upper
     * case letter, and converts upper case letters to lower case (with the
     * exception of the first word). Note that <b>consecutive upper case letters</b>
     * <b>will remain grouped</b>, as they are considered to represent an acronym.<br>
     * <br>
     * <u>NOTE:</u> This method is optimized for class names that follow the Java
     * naming convention. <br>
     * Examples:<br>
     * MyGreatClass -&lt; "My great class"<br>
     * MyXYZClass -&lt; "My XYZ class"
     * 
     * @param string
     *        the string to flatten
     * @return a flattened (i.e. pretty-printed) String based on the name of the
     *         string
     */
    public static String getFlattened(String string)
    {
        String[] words = string.split("(?=[A-Z])");

        String output = words[0];
        if (words.length > 1)
        {
            int nextWordIndex = 1;

            final int javaVersion = (int) SystemUtil.getJavaVersionAsNumber();
            if (javaVersion < 8)
            {
                output = words[1];
                nextWordIndex++;
            }

            for (int i = nextWordIndex; i < words.length; i++)
            {
                String word = words[i];
                if (word.length() == 1)
                {
                    // single letter
                    if (words[i - 1].length() == 1)
                    {
                        // append to the previous letter (acronym)
                        output += word;
                    }
                    else
                    {
                        // new isolated letter or acronym
                        output += " " + word;
                    }
                }
                else
                    output += " " + word.toLowerCase();
            }
        }

        return output;
    }

}
