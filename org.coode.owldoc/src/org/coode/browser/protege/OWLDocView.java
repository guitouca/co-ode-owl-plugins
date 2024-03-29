package org.coode.browser.protege;

import org.apache.log4j.Logger;
import org.coode.html.OWLHTMLKit;
import org.coode.html.doclet.HTMLDoclet;
import org.coode.html.doclet.HierarchyRootDoclet;
import org.coode.html.hierarchy.OWLClassHierarchyTreeFragment;
import org.coode.html.hierarchy.TreeFragment;
import org.coode.html.impl.OWLHTMLKitImpl;
import org.coode.html.impl.OWLHTMLProperty;
import org.coode.html.summary.*;
import org.coode.owl.mngr.OWLServer;
import org.protege.editor.core.ui.util.NativeBrowserLauncher;
import org.protege.editor.core.ui.view.DisposableAction;
import org.semanticweb.owlapi.model.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

/*
 * Copyright (C) 2007, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

/**
 * Author: Nick Drummond<br>
 *
 * http://www.cs.man.ac.uk/~drummond<br><br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Jun 22, 2007<br><br>
 * <p/>
 */
public class OWLDocView extends AbstractBrowserView {

    private static final Logger logger = Logger.getLogger(OWLDocView.class);

    private static final String OWLDOC_CSS = "resources/owldocview.css";

    private PipedReader r;
    private PrintWriter w;

    private OWLObject currentSelection;

    private boolean renderSubs = false;

    private HyperlinkListener linkListener = new HyperlinkListener(){
        public void hyperlinkUpdate(HyperlinkEvent event) {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
                handleHyperlink(event.getURL());
            }
        }
    };


    private DisposableAction srcAction = new DisposableAction("Show source", null){
        public void actionPerformed(ActionEvent event) {
            handleShowSrc();
        }

        public void dispose() {
            // do nothing
        }
    };

    private OWLHTMLKit kit;


    protected void initialiseOWLView() throws Exception {
        super.initialiseOWLView();

        getBrowser().setNavigateActive(false);

        OWLServer server = new ProtegeServerImpl(getOWLModelManager());
        kit = new OWLHTMLKitImpl("id", server, new URL("http://www.co-ode.org/"));
        kit.getHTMLProperties().set(OWLHTMLProperty.optionDefaultCSS, null);
        kit.setURLScheme(new ProtegeURLScheme(kit));

        getBrowser().addLinkListener(linkListener);

        refresh(getOWLWorkspace().getOWLSelectionModel().getSelectedEntity());

        addAction(srcAction, "A", "A");
    }

    protected String getCSS() {
        return OWLDOC_CSS;
    }

    protected void disposeOWLView() {
        super.disposeOWLView();
        getBrowser().removeLinkListener(linkListener);
    }


    protected void refresh(OWLEntity object) {

        this.currentSelection = object;

        if (currentSelection != null){
            Runnable generateHTML = new Runnable(){
                public void run() {
                    try{
                        HTMLDoclet ren = getRenderer();
                        ren.renderAll(kit.getURLScheme().getURLForOWLObject(OWLDocView.this.currentSelection), w);
                        w.close();
                    }
                    catch(Throwable e){
                        e.printStackTrace();
//                        logger.error(e);
                    }
                }
            };

            try {
                r = new PipedReader();
                w = new PrintWriter(new PipedWriter(r));
                new Thread(generateHTML).start();
                getBrowser().setContent(r, kit.getURLScheme().getURLForOWLObject(currentSelection));
            }
            catch (Exception e) {
                logger.error(e);
            }
        }
        else{
            getBrowser().clear();
        }
    }

    private AbstractOWLEntitySummaryHTMLPage getRenderer(){
        AbstractOWLEntitySummaryHTMLPage ren = null;
        if (currentSelection instanceof OWLClass){
            TreeFragment<OWLClass> tree = new OWLClassHierarchyTreeFragment(kit, kit.getOWLServer().getClassHierarchyProvider(), "Class Hierarchy");
            HierarchyRootDoclet<OWLClass> clsHierarchyRen = new HierarchyRootDoclet<OWLClass>(kit, tree);
            clsHierarchyRen.setShowSubs(renderSubs);
            ren = new OWLClassSummaryHTMLPage(kit);
            ren.setOWLHierarchyRenderer(clsHierarchyRen);
        }
        else if (currentSelection instanceof OWLObjectProperty){
            ren = new OWLObjectPropertySummaryHTMLPage(kit);
        }
        else if (currentSelection instanceof OWLDataProperty){
            ren = new OWLDataPropertySummaryHTMLPage(kit);
        }
        else if (currentSelection instanceof OWLIndividual){
            ren = new OWLIndividualSummaryHTMLPage(kit);
        }
        else if (currentSelection instanceof OWLAnnotationProperty){
            ren = new OWLAnnotationPropertySummaryHTMLPage(kit);
        }
        else if (currentSelection instanceof OWLDatatype){
            ren = new OWLDatatypeSummaryHTMLPage(kit);
        }

        if (ren != null){
            ren.setUserObject(currentSelection);
        }
        return ren;
    }

    private void updateGlobalSelection(URL url) {
        OWLObject tempEntity = kit.getURLScheme().getOWLObjectForURL(url);
        if (tempEntity != null){
            currentSelection = tempEntity;
            if (tempEntity instanceof OWLEntity){
                getOWLWorkspace().getOWLSelectionModel().setSelectedEntity((OWLEntity) currentSelection);
            }
        }
        else{
            NativeBrowserLauncher.openURL(url.toString());
        }
    }


    private void handleHyperlink(URL linkURL) {
        if (!linkURL.equals(kit.getURLScheme().getURLForOWLObject(currentSelection))){
            if (linkURL.toString().endsWith("#")){ //@@TODO tidyup - this is a hack for now
                renderSubs = true;
                final OWLObject owlObject = kit.getURLScheme().getOWLObjectForURL(linkURL);
                if (owlObject instanceof OWLEntity){
                    refresh((OWLEntity)owlObject);
                }
            }
            else{
                renderSubs = false;
                updateGlobalSelection(linkURL);
            }
        }
    }

    private void handleShowSrc() {
        HTMLDoclet ren = getRenderer();
        final StringWriter stringWriter = new StringWriter();
        PrintWriter stringRenderer = new PrintWriter(stringWriter);
        ren.renderAll(kit.getURLScheme().getURLForOWLObject(OWLDocView.this.currentSelection), stringRenderer);
        stringRenderer.flush();
        JTextArea t = new JTextArea(stringWriter.getBuffer().toString());
        t.setWrapStyleWord(true);
        final JScrollPane scroller = new JScrollPane(t);
        scroller.setPreferredSize(new Dimension(600, 500));
        JOptionPane.showMessageDialog(null, scroller, "Source for " + getOWLModelManager().getRendering(currentSelection), JOptionPane.OK_OPTION);
    }
}
