/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.node;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.Image;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Crawl a website and save images as sub nodes of parent node.
 *
 * @author amorgner
 */
public class SaveImagesFromWebsiteUrl extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(SaveImagesFromWebsiteUrl.class.getName());

    /**
     * Takes three parameters.
     * 
     * <ul>
     * <li>1: User
     * <li>2: URL string
     * <li>3: Parent node
     * </ul>
     *
     * @param parameters
     * @return
     */
    @Override
    public Object execute(Object... parameters) {

        User user = null;
        String urlString = null;
        AbstractNode parentNode = null;

        List<Image> result = new LinkedList<Image>();

        switch (parameters.length) {

            case 3:

                if (parameters[0] instanceof User) {
                    user = (User) parameters[0];
                }
                if (parameters[1] instanceof String) {
                    urlString = (String) parameters[1];
                }
                if (parameters[2] instanceof AbstractNode) {
                    parentNode = (AbstractNode) parameters[2];
                }
                break;

            default:

                throw new UnsupportedArgumentError("Wrong number of arguments");

        }

        if (user != null && urlString != null && parentNode != null) {

            result = saveImagesFromWebsiteUrl(user, urlString, parentNode);

        }

        return result;

    }

    /**
     * Save all images from the given website as image nodes
     * 
     * @param imageNode
     */
    private List<Image> saveImagesFromWebsiteUrl(final User user, final String urlString, final AbstractNode parentNode) {

        Command saveImage = Services.command(securityContext, SaveImageFromUrl.class);
        List<Image> result = new LinkedList<Image>();

        try {
            Parser parser = new Parser(urlString);

            NodeList imgNodes = parser.extractAllNodesThatMatch(new TagNameFilter("img"));

            Node[] nodeArray = imgNodes.toNodeArray();

            for (Node imgNode : nodeArray) {

                //Node imgNode = imgNodes.elements().nextNode();

                if (imgNode instanceof ImageTag) {

                    ImageTag imageTag = (ImageTag) imgNode;
                    String imageUrl = imageTag.getImageURL();

                    Image newImageNode = (Image) saveImage.execute(user, imageUrl, parentNode);

                    result.add(newImageNode);

                }

            }

        } catch (ParserException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
}
