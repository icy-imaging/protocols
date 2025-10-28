/*
 * Copyright (c) 2010-2025. Institut Pasteur.
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

package plugins.adufour.protocols.livebar;

import org.bioimageanalysis.icy.common.string.StringUtil;
import org.bioimageanalysis.icy.io.xml.XMLUtil;
import org.bioimageanalysis.icy.network.NetworkUtil;
import org.bioimageanalysis.icy.network.search.OnlineSearchResultProducer;
import org.bioimageanalysis.icy.network.search.SearchResult;
import org.bioimageanalysis.icy.network.search.SearchResultConsumer;
import org.bioimageanalysis.icy.network.search.SearchResultProducer;
import org.bioimageanalysis.icy.system.SystemUtil;
import org.bioimageanalysis.icy.system.thread.ThreadUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import plugins.adufour.protocols.Protocols;

import java.awt.*;
import java.util.ArrayList;

public class OnlineProtocolProducer extends OnlineSearchResultProducer {
    private static final String ID_TEXT = "string";
    private static final String ID_SEARCH_RESULT = "searchresult";
    private static final String ID_PROTOCOL = "protocol";

    @Override
    public void doSearch(final Document doc, final String words, final SearchResultConsumer consumer) {
        // can't get any result from website --> exit
        if (doc == null) return;

        if (getClass().getClassLoader() != SystemUtil.getSystemClassLoader() && hasWaitingSearch()) return;

        // get online result node
        final Element resultElement = XMLUtil.getElement(doc.getDocumentElement(), ID_SEARCH_RESULT);

        if (resultElement == null) return;

        final ArrayList<SearchResult> tmpResults = new ArrayList<>();

        for (final Element protocol : XMLUtil.getElements(resultElement, ID_PROTOCOL)) {
            // abort
            if (getClass().getClassLoader() != SystemUtil.getSystemClassLoader() && hasWaitingSearch()) return;

            final String text = XMLUtil.getElementValue(protocol, ID_TEXT, "");

            final ProtocolDescriptor onlineProtocol = new ProtocolDescriptor(protocol);

            final SearchResult result = new OnlineProtocolResult(this, onlineProtocol, text, words.split(" "));

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

        public OnlineProtocolResult(final SearchResultProducer provider, final ProtocolDescriptor protocol, final String text, final String[] searchWords) {
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
                    for (final String word : searchWords)
                        description = StringUtil.htmlBoldSubstring(description, word, true);
                }
            }
        }

        @Override
        public String getTitle() {
            return protocol.getName();
        }

        @Override
        public @Nullable Image getImage() {
            return protocol.getIconAsImage();

            //if (icon != null) return icon.getImage();

            //return null;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Contract(pure = true)
        @Override
        public @NotNull String getTooltip() {
            return "Left click: Open   -   Right click: Online documentation";
            // return plugin.getDescription();
        }

        @Override
        public void execute() {
            // can take sometime, better to execute it in background
            ThreadUtil.invokeLater(() -> Protocols.loadWorkFlow(protocol.getFile()));
        }

        @Override
        public void executeAlternate() {
            NetworkUtil.openBrowser(protocol.getWeb());
        }
    }

}
