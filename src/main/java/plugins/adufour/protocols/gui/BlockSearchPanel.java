package plugins.adufour.protocols.gui;

import icy.gui.frame.progress.AnnounceFrame;
import icy.network.NetworkUtil;
import icy.plugin.PluginDescriptor;
import icy.plugin.abstract_.Plugin;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.util.StringUtil;
import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.JavadocComment;
import japa.parser.ast.body.TypeDeclaration;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

import org.pushingpixels.substance.internal.ui.SubstanceSplitPaneUI;
import org.pushingpixels.substance.internal.utils.SubstanceSplitPaneDivider;

import plugins.adufour.blocks.util.BlocksFinder;
import plugins.adufour.blocks.util.BlocksFinder.DND_MenuItem;
import plugins.adufour.blocks.util.MenuItemListener;

/**
 * A search panel for the Protocols interface, where blocks can be found by keyword search
 * 
 * @author Ludovic Laborde, Alexandre Dufour
 */
@SuppressWarnings("serial")
public class BlockSearchPanel extends JPanel implements MenuItemListener
{
    private static final int    MIN_CHARACTERS_FOR_SEARCH = 2;
    private static final int    SEARCH_TEXT_FONT_SIZE     = 14;
    private static final String DEFAULT_SEARCH_TEXT       = "Search for blocks...";
    
    private static final int    SCROLL_SPEED              = 32;
    
    private static final String DEFAULT_INFO_TEXT         = "<html><body style=\"font-family: Tahoma\"><i>Search and click on a block above to see its documentation.</i></body></html>";
    
    private JTextField          searchField;
    private JLabel              searchIcon;
    private String              currentSearchText;
    private Box                 searchBar;
    
    private BlocksFinder        finder;
    private Box                 blockList;
    private JScrollPane         blockListDisplay;
    
    private JTextPane           blockInfo;
    private JScrollPane         blockInfoDisplay;
    
    private JSplitPane          results;
    
    public BlockSearchPanel()
    {
        searchIcon = new JLabel(new IcyIcon(ResourceUtil.ICON_SEARCH));
        searchField = new JTextField(DEFAULT_SEARCH_TEXT);
        currentSearchText = searchField.getText();
        searchBar = Box.createHorizontalBox();
        
        finder = new BlocksFinder();
        blockList = Box.createVerticalBox();
        blockListDisplay = new JScrollPane(blockList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        blockInfo = new JTextPane();
        blockInfo.setContentType("text/html");
        blockInfo.setText(DEFAULT_INFO_TEXT);
        blockInfoDisplay = new JScrollPane(blockInfo, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        results = new JSplitPane(JSplitPane.VERTICAL_SPLIT, blockListDisplay, blockInfoDisplay);
        
        // prevents drifting to the center when moving the slide bar
        searchIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.setMinimumSize(new Dimension(180, 28)); // prevent buggy display of the
                                                            // textfield when the panel is small
        searchField.setFont(new Font("Arial", Font.PLAIN, SEARCH_TEXT_FONT_SIZE));
        searchField.setPreferredSize(searchField.getMinimumSize());
        searchField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusLost(FocusEvent arg0)
            {
                searchField.setText(currentSearchText);
            }
            
            @Override
            public void focusGained(FocusEvent arg0)
            {
                if (searchField.getText().equals(DEFAULT_SEARCH_TEXT)) searchField.setText("");
            }
        });
        
        // calls the search function when typing in the search bar
        searchField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void removeUpdate(DocumentEvent arg0)
            {
                blockList.removeAll();
                if (searchField.getText().length() >= MIN_CHARACTERS_FOR_SEARCH) updateBlockList();
                else blockList.setVisible(false);
                currentSearchText = searchField.getText();
            }
            
            @Override
            public void insertUpdate(DocumentEvent arg0)
            {
                blockList.removeAll();
                if (searchField.getText().length() >= MIN_CHARACTERS_FOR_SEARCH)
                {
                    updateBlockList();
                    if (!blockList.isVisible()) blockList.setVisible(true);
                }
                currentSearchText = searchField.getText();
            }
            
            @Override
            public void changedUpdate(DocumentEvent arg0)
            {
            }
        });
        searchBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        searchBar.add(searchIcon);
        searchBar.add(Box.createHorizontalStrut(5));
        searchBar.add(searchField);
        searchBar.add(Box.createHorizontalGlue()); // prevents drifting to the center when moving
                                                   // the slide bar
        
        blockList.setVisible(false);
        blockListDisplay.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);
        blockListDisplay.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), new LineBorder(Color.GRAY)));
        blockListDisplay.setPreferredSize(new Dimension(blockListDisplay.getWidth(), 250));
        
        blockInfo.setEditable(false);
        blockInfo.setBackground(Color.LIGHT_GRAY);
        // opens a browser when clicking a hyperlink
        blockInfo.addHyperlinkListener(new HyperlinkListener()
        {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent event)
            {
                if (event.getEventType().equals(EventType.ACTIVATED) && Desktop.isDesktopSupported())
                {
                    if (event.getURL() != null)
                    {
                        NetworkUtil.openBrowser(event.getURL().toString());
                    }
                    else new AnnounceFrame("Error looking up online documentation.");
                }
            }
        });
        blockInfoDisplay.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));
        blockInfoDisplay.setPreferredSize(new Dimension(200, 150));
        blockInfoDisplay.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);
        
        results.setOneTouchExpandable(true);
        results.setContinuousLayout(true);
        results.setUI(new MySplitPaneUI());
        
        setLayout(new BorderLayout());
        setBorder(new CompoundBorder(getBorder(), new EmptyBorder(0, 5, 5, 5)));
        setPreferredSize(getMinimumSize());
        add(searchBar, BorderLayout.NORTH);
        add(results, BorderLayout.CENTER);
    }
    
    public Component add(Component c)
    {
        return blockList.add(c);
    }
    
    private void updateBlockList()
    {
        finder.createSearchMenu(this, searchField.getText());
        
        revalidate();
        repaint();
    }
    
    /**
     * updates the block documentation with it's annotations
     * 
     * @param d
     *            the plugin descriptor
     */
    public void displayDoc(PluginDescriptor d)
    {
        // highlight the selected block
        for (Component c : blockList.getComponents())
        {
            if (d == ((DND_MenuItem) c).getDescriptor())
            {
                c.setBackground(c.getBackground());
            }
            else
            {
                c.setBackground(c.getForeground());
            }
        }
        
        Class<? extends Plugin> _class = d.getPluginClass();
        
        String name = d.getName();
        String description = "<h3>Public description:</h3>";
        String author = d.getAuthor();
        
        // if (_class.isAnnotationPresent(BlockSearchAnnotation.class))
        // {
        // BlockSearchAnnotation annotation = _class.getAnnotation(BlockSearchAnnotation.class);
        //
        // name = annotation.name();
        // description = annotation.description();
        // author = annotation.author();
        // }
        
        if (d.getDescription().isEmpty())
        {
            description += "<i>No description available.\nPlease leave a comment on the plugin page to request it.</i>";
        }
        else if (d.getDescription().equalsIgnoreCase(name + " plugin"))
        {
            name = StringUtil.getFlattened(_class.getSimpleName());
            if (name.toLowerCase().endsWith(" block")) name = name.substring(0, name.toLowerCase().lastIndexOf(" block"));
            
            // plug-in has no online descriptor
            // => either part of a bundle
            // => or not published yet (and accessed via the Eclipse debugger)
            description += "<i>This plug-in has no description.</i>";
        }
        else
        {
            description += "<i>" + d.getDescription() + "</i>";
        }
        
        // add JavaDoc (if available) to the description
        try
        {
            InputStream stream = d.getPluginClass().getResourceAsStream(d.getSimpleClassName() + ".java");
            CompilationUnit unit = JavaParser.parse(stream); // retrieve the block's source file
            stream.close();
            
            if (unit.getComments() != null)
            {
                // get the first declared type (should be the main class per Java convention)
                TypeDeclaration declaration = unit.getTypes().get(0);
                JavadocComment doc = declaration.getJavaDoc();
                
                // the "real" new line markers are "<br/>\n" or "* \n"
                // to split them all at once, replace one by the other
                
                String docString = doc.getContent().trim().replace("* \n", "<br/>\n");
                // now remove "fake" new line markers
                docString = docString.replace("* ", "").replace("\n", "");
                String[] lines = docString.split("<br/> ");
                
                if (lines.length > 0)
                {
                    description += "<h3>Developer documentation:</h3>";
                    
                    description += "<i>";
                    for (String line : lines)
                    {
                        if (line.startsWith("@"))
                        {
                            // watch out for the "@author" annotation
                            if (line.startsWith("@author") && author.isEmpty()) author = line.replace("@author ", "");
                            // but discard the rest for now
                        }
                        else
                        {
                            while (line.contains("@link"))
                            {
                                // hide links (for now)
                                int start = line.indexOf("{@link");
                                int end = line.indexOf("}", start);
                                String link = line.substring(start, end + 1);
                                // if the link has a name after the target, use it
                                String[] linkWords = link.substring(7, link.length() - 1).split(" ");
                                String linkReplacement = "<b>";
                                if (linkWords.length == 1)
                                {
                                    linkReplacement += linkWords[0];
                                }
                                else
                                {
                                    linkReplacement += linkWords[1];
                                    for (int i = 2; i < linkWords.length; i++)
                                        linkReplacement += " " + linkWords[i];
                                }
                                linkReplacement += "</b>";
                                line = line.replace(link, linkReplacement);
                            }
                            
                            description += line + "<br/>";
                        }
                    }
                    description += "</i>";
                }
            }
        }
        catch (Exception e)
        {
            // the source file probably wasn't found. Don't bother then...
        }
        
        String doc = "<html><body style=\"font-family: Tahoma\">";
        doc += "<h2 align=\"center\">" + name + "</h2>";
        doc += description;
        if (!author.isEmpty()) doc += "<p><b>Author: </b>" + author + "</p>";
        if (!d.getWeb().isEmpty()) doc += "<p><a href=\"" + d.getWeb() + "\" ><b>More information...</b></a><br/>";
        doc += "</body></html>";
        
        blockInfo.setText(doc);
        blockInfo.setCaretPosition(0);
        blockInfoDisplay.revalidate();
        blockInfoDisplay.repaint();
    }
    
    private class MySplitPaneUI extends SubstanceSplitPaneUI
    {
        
        private class MySplitPaneDivider extends SubstanceSplitPaneDivider
        {
            
            public MySplitPaneDivider(SubstanceSplitPaneUI ui)
            {
                super(ui);
            }
            
            protected JButton createLeftOneTouchButton()
            {
                JButton button = super.createLeftOneTouchButton();
                button.setPreferredSize(new Dimension(40, 40));
                return button;
            }
            
            protected JButton createRightOneTouchButton()
            {
                JButton button = super.createRightOneTouchButton();
                button.setPreferredSize(new Dimension(40, 40));
                return button;
            }
        }
        
        public BasicSplitPaneDivider createDefaultDivider()
        {
            return new MySplitPaneDivider(this);
        }
    }
}
