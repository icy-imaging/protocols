package plugins.adufour.protocols.livebar;

import java.awt.Image;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import icy.network.NetworkUtil;
import icy.search.OnlineSearchResultProducer;
import icy.search.SearchResult;
import icy.search.SearchResultConsumer;
import icy.search.SearchResultProducer;
import icy.system.SystemUtil;
import icy.system.thread.ThreadUtil;
import icy.util.StringUtil;
import icy.util.XMLUtil;
import plugins.adufour.protocols.Protocols;

public class OnlineProtocolProducer extends OnlineSearchResultProducer {
    private static final String ID_TEXT = "string";
    private static final String ID_SEARCH_RESULT = "searchresult";
    private static final String ID_PROTOCOL = "protocol";

    @Deprecated
    @Override
    public void doSearch(Document doc, String[] words, SearchResultConsumer consumer) {
        // can't get result from website --> exit
        if (doc == null) return;

        if (getClass().getClassLoader() != SystemUtil.getSystemClassLoader() && hasWaitingSearch()) return;

        // get online result node
        final Element resultElement = XMLUtil.getElement(doc.getDocumentElement(), ID_SEARCH_RESULT);

        if (resultElement == null) return;

        final ArrayList<SearchResult> tmpResults = new ArrayList<>();

        for (Element protocol : XMLUtil.getElements(resultElement, ID_PROTOCOL)) {
            // abort
            if (getClass().getClassLoader() != SystemUtil.getSystemClassLoader() && hasWaitingSearch()) return;

            final String text = XMLUtil.getElementValue(protocol, ID_TEXT, "");

            ProtocolDescriptor onlineProtocol = new ProtocolDescriptor(protocol);

            final SearchResult result = new OnlineProtocolResult(this, onlineProtocol, text, words);

            if (result != null) tmpResults.add(result);
        }

        results = tmpResults;
        consumer.resultsChanged(this);
    }

    @Override
    public String getName() {
        return "Online Protocols";
    }

    /**
     * @author Stephane
     */
    private static class OnlineProtocolResult extends SearchResult {
        final ProtocolDescriptor protocol;
        private String description;

        public OnlineProtocolResult(SearchResultProducer provider, ProtocolDescriptor protocol, String text, String[] searchWords) {
            super(provider);

            this.protocol = protocol;

            int wi = 0;
            description = "";
            while (StringUtil.isEmpty(description) && (wi < searchWords.length)) {
                // no more than 80 characters...
                description = StringUtil.trunc(text, searchWords[wi], 80);
                wi++;
            }

            if (!StringUtil.isEmpty(description)) {
                // remove carriage return
                description = description.replace("\n", "");

                // highlight search keywords (only for more than 2 characters search)
                if ((searchWords.length > 1) || (searchWords[0].length() > 2)) {
                    // highlight search keywords
                    for (String word : searchWords)
                        description = StringUtil.htmlBoldSubstring(description, word, true);
                }
            }
        }

        @Override
        public String getTitle() {
            return protocol.getName();
        }

        @Override
        public Image getImage() {
            final ImageIcon icon = protocol.getIcon();

            if (icon != null) return icon.getImage();

            return null;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getTooltip() {
            return "Left click: Open   -   Right click: Online documentation";
            // return plugin.getDescription();
        }

        @Override
        public void execute() {
            // can take sometime, better to execute it in background
            ThreadUtil.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Protocols.loadWorkFlow(protocol.getFile());
                }
            });
        }

        @Override
        public void executeAlternate() {
            NetworkUtil.openBrowser(protocol.getWeb());
        }
    }

}
