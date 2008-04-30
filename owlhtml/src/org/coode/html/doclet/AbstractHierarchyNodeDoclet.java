/*
* Copyright (C) 2007, University of Manchester
*/
package org.coode.html.doclet;

import org.coode.html.OWLHTMLServer;
import org.coode.html.util.URLUtils;
import org.coode.html.hierarchy.TreeFragment;
import org.coode.html.renderer.OWLHTMLRenderer;
import org.coode.owl.mngr.ServerConstants;
import org.semanticweb.owl.model.OWLNamedObject;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Author: Nick Drummond<br>
 * http://www.cs.man.ac.uk/~drummond/<br><br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Feb 7, 2008<br><br>
 */
public abstract class AbstractHierarchyNodeDoclet<O extends OWLNamedObject> extends AbstractOWLDocDoclet<O>{

    private static final Logger logger = Logger.getLogger(AbstractHierarchyNodeDoclet.class);

    private final TreeFragment<O> model;

    private boolean autoExpandSubs = false;

    private boolean showSubs = false;


    public AbstractHierarchyNodeDoclet(OWLHTMLServer server, TreeFragment<O> model) {
        super(server);
        setPinned(true); // you will never change the subs as they will be regenerated each time this changed
        this.model = model;
    }


    public final void setAutoExpandEnabled(boolean enabled) {
        this.autoExpandSubs = enabled;
        for (HTMLDoclet subdoclet : getDoclets()){
            if (subdoclet instanceof AbstractHierarchyNodeDoclet){
                ((AbstractHierarchyNodeDoclet)subdoclet).setAutoExpandEnabled(enabled);
            }
        }
    }


    public final void setShowSubs(boolean enabled) {
        this.showSubs = enabled;
    }


    protected TreeFragment<O> getModel() {
        return model;
    }


    protected final boolean isAutoExpandSubs() {
        return autoExpandSubs;
    }

    protected final boolean isShowSubsEnabled(){
        return showSubs;
    }

    public String getID() {
        return getServer().getNameRenderer().getShortForm(getUserObject());
    }

    protected void renderNode(O node, OWLHTMLRenderer objRenderer, URL pageURL, PrintWriter out) {
        if (!model.isLeaf(node)){
            out.print("<li class='expandable'>");
            renderExpandLink(node, pageURL, out);
        }
        else{
            out.print("<li>");
        }
        objRenderer.render(node, pageURL, out);
        out.print("</li>");
    }

    protected void renderExpandLink(O node, URL pageURL, PrintWriter out) {
        try {
            if (isRenderSubExpandLinksEnabled()){
                String link = URLUtils.createRelativeURL(pageURL, getServer().getURLScheme().getURLForNamedObject(node));
                if (!link.contains("?")){
                    link += "?";
                }
                else{
                    link += "&";
                }

                URL expandLinkURL = new URL(link + "expanded=true");
                out.println(" <a href='" + expandLinkURL + "'>[+]</a>");
            }
            else{
                out.println(" +");
            }
        }
        catch (MalformedURLException e) {
            logger.error("Could not render tree expand link for " + node.getURI(), e);
        }
    }


    protected boolean isRenderSubExpandLinksEnabled() {
        return getServer().getProperties().isSet(ServerConstants.OPTION_RENDER_SUB_EXPAND_LINKS);
    }
}