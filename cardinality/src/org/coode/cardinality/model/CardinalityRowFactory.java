package org.coode.cardinality.model;

import org.coode.cardinality.util.ClosureUtils;
import org.coode.cardinality.util.RestrictionUtils;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
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
 * http://www.cs.man.ac.uk/~drummond/<br><br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Sep 4, 2007<br><br>
 */
public class CardinalityRowFactory {

    private ClosureUtils closureUtils;
    private OWLModelManager mngr;

    private OWLClass cls = null;

    private List<CardinalityRow> rows = new ArrayList<CardinalityRow>();

    private boolean showInherited = true;

    public CardinalityRowFactory(OWLModelManager mngr) {
        this.mngr = mngr;
        this.closureUtils = new ClosureUtils(mngr);
    }

    public static CardinalityRow createRow(OWLClass subject, OWLProperty prop, OWLObject filler,
                                           int min, int max, boolean closed, OWLModelManager mngr){
        if (subject != null && prop != null && filler != null){
            return new CardinalityRowImpl(subject, prop, filler, min, max, closed, mngr);
        }
        return null;
    }

    public static List<OWLOntologyChange> toOWL(CardinalityRow row, OWLOntology ont, OWLDataFactory df) {
        if (row.getProperty() instanceof OWLObjectProperty) {
            return handleObject(row, ont, df);
        }
        else{
            return handleData(row, ont, df);
        }
    }

    private static List<OWLOntologyChange> handleObject(CardinalityRow row, OWLOntology ont, OWLDataFactory df) {
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();

        int min = row.getMin();
        int max = row.getMax();
        OWLClass subject = row.getSubject();
        OWLObject filler = row.getFiller();
        OWLProperty prop = row.getProperty();

        if (filler instanceof OWLIndividual){
            OWLClassExpression minRestr = df.getOWLObjectHasValue((OWLObjectProperty)prop, (OWLIndividual)filler);
            changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, minRestr)));
        }
        else{
            if (min == 1) {
                OWLClassExpression minRestr = df.getOWLObjectSomeValuesFrom((OWLObjectProperty)prop, (OWLClassExpression) filler);
                changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, minRestr)));
            }
            else if (min >= 0) {
                OWLClassExpression minRestr = df.getOWLObjectMinCardinality(min, (OWLObjectProperty)prop, (OWLClassExpression) filler);
                changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, minRestr)));
            }

            if (max == 0) { // transform into a negated some restriction
                OWLObjectSomeValuesFrom someRestr = df.getOWLObjectSomeValuesFrom((OWLObjectProperty)prop, (OWLClassExpression) filler);
                OWLClassExpression maxRestr = df.getOWLObjectComplementOf(someRestr);
                changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, maxRestr)));
            }
            else if (max >= 0) {
                OWLClassExpression maxRestr = df.getOWLObjectMaxCardinality(max, (OWLObjectProperty)prop, (OWLClassExpression) filler);
                changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, maxRestr)));
            }
        }
        return changes;
    }

    private static List<OWLOntologyChange> handleData(CardinalityRow row, OWLOntology ont, OWLDataFactory df) {
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();

        int min = row.getMin();
        int max = row.getMax();
        OWLClass subject = row.getSubject();
        OWLObject filler = row.getFiller();
        OWLProperty prop = row.getProperty();

        if (filler instanceof OWLLiteral){
            OWLClassExpression minRestr = df.getOWLDataHasValue((OWLDataProperty)prop, (OWLLiteral)filler);
            changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, minRestr)));
        }
        else{
            if (min == 1) {
                OWLClassExpression minRestr = df.getOWLDataSomeValuesFrom((OWLDataProperty)prop, (OWLDataRange)filler);
                changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, minRestr)));
            }
            else if (min >= 0) {
                OWLClassExpression minRestr = df.getOWLDataMinCardinality(min, (OWLDataProperty)prop, (OWLDataRange) filler);
                changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, minRestr)));
            }

            if (max == 0) { // transform into a negated some restriction
                OWLDataSomeValuesFrom someRestr = df.getOWLDataSomeValuesFrom((OWLDataProperty)prop, (OWLDataRange) filler);
                OWLClassExpression maxRestr = df.getOWLObjectComplementOf(someRestr);
                changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, maxRestr)));
            }
            else if (max >= 0) {
                OWLClassExpression maxRestr = df.getOWLDataMaxCardinality(max, (OWLDataProperty)prop, (OWLDataRange) filler);
                changes.add(new AddAxiom(ont, df.getOWLSubClassOfAxiom(subject, maxRestr)));
            }
        }
        return changes;
    }

    public void setShowInherited(boolean showInherited){
        if (showInherited != this.showInherited){
            this.showInherited = showInherited;
            reload();
        }
    }

    public void setSubject(OWLClass newClass) {
        this.cls = newClass;
        reload();
    }

    public OWLClass getSubject() {
        return cls;
    }

    public void reload() {

        rows.clear();

        if (cls != null) {
            Set<OWLClassExpression> directSupers = RestrictionUtils.getDirectRestrictionsOnClass(cls, mngr);
            addOWLRestrictions(directSupers, false);

            if (showInherited) {
                Set<OWLClassExpression> inheritedRestrs = RestrictionUtils.getInheritedRestrictionsOnClass(cls, mngr);
                addOWLRestrictions(inheritedRestrs, true);
            }
        }

        Collections.sort(rows);
    }

    public void addOWLRestrictions(Collection<OWLClassExpression> restrs, boolean readonly) {
        for (OWLClassExpression restr : restrs) { // any restriction
            CardinalityRow newRow = createRow(restr, readonly);
            if (newRow != null) {
                if (!mergeWithExistingRows(newRow)) {
                    rows.add(newRow);
                }
            }
        }
    }

    /**
     * Atomic removal of restrictions and management of closure (must be done in a single transaction for undo)
     *
     * @param rows the rows to delete
     */
    public List<OWLOntologyChange> removeRestrictions(Collection<CardinalityRow> rows) {

        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();

        Map<OWLProperty, Set<OWLClassExpression>> closureMap = new HashMap<OWLProperty, Set<OWLClassExpression>>();

        for (CardinalityRow restr : rows) {
            if (!restr.isReadOnly()) {
                changes.addAll(restr.getDeleteChanges());

                // can't reset closure using the current state of the ontology (as nothing has been deleted yet)
                if (restr.isClosed()) {
                    // generate a map of properties to fillers that should be removed
                    Set<OWLClassExpression> fillers = closureMap.get(restr.getProperty());
                    if (fillers == null) {
                        fillers = new HashSet<OWLClassExpression>();
                        closureMap.put(restr.getProperty(), fillers);
                    }
                    fillers.add((OWLClassExpression) restr.getFiller());
                }
            }
        }

        for (OWLProperty prop : closureMap.keySet()) {
            if (prop instanceof OWLObjectProperty){
                changes.addAll(closureUtils.removeFromClosure(closureMap.get(prop),
                                                              getSubject(),
                                                              (OWLObjectProperty)prop));
            }
        }
        return changes;
    }

    public List<CardinalityRow> getRows(){
        return Collections.unmodifiableList(rows);
    }

    private CardinalityRow createRow(OWLClassExpression descr, boolean readonly) {
        CardinalityRow row = null;
        if ((RestrictionUtils.isNotSome(descr)) ||
            ((descr instanceof OWLRestriction) && !(descr instanceof OWLObjectAllValuesFrom))) {

            OWLProperty prop = RestrictionUtils.getProperty(descr);
            OWLObject filler = RestrictionUtils.getOWLFiller(descr);
            final boolean closed = closureUtils.isClosed(cls, prop, filler, mngr);
            row = new CardinalityRowImpl(cls,
                                         prop,
                                         filler,
                                         RestrictionUtils.getMinRelationships(descr),
                                         RestrictionUtils.getMaxRelationships(descr),
                                         closed,
                                         mngr);
            row.addRestriction(descr, readonly);
            row.setFactory(this);
        }
        return row;
    }

    private boolean mergeWithExistingRows(CardinalityRow newRow) {
        boolean merged = false;
        for (Iterator<CardinalityRow> j = rows.iterator(); j.hasNext() && !merged;) {
            CardinalityRow cardiRow = j.next();
            if (canMerge(cardiRow, newRow)) {
                cardiRow.merge(newRow);
                merged = true;
            }
        }
        return merged;
    }

    // Simple implementation - does not deal with subsumption of properties and fillers
    protected boolean canMerge(CardinalityRow row1, CardinalityRow row2) {
        return (row1.getProperty().equals(row2.getProperty())) &&
               (row1.getFiller().equals(row2.getFiller()));
    }

    public Set<OWLClassExpression> getFillers(OWLObjectProperty property) {
        Set<OWLClassExpression> fillers = new HashSet<OWLClassExpression>();
        for (CardinalityRow row : rows) {
            if (row.getProperty().equals(property) &&
                row.getFiller() instanceof OWLClassExpression) {
                fillers.add((OWLClassExpression) row.getFiller());
            }
        }
        return fillers;
    }

//    /**
//     * If:
//     * - both the fillers are named classes
//     * - the restriction is not an OWLAllValuesFrom
//     * - the restricted property is an OWLObjectProperty
//     * - the restriction has more specific cardinality than that which already exists
//     */
//    protected boolean canMerge(CardinalityRow row1, CardinalityRow row2) {
//        boolean canMerge = false;
//            if ((row1.getFiller() instanceof OWLClass) &&
//                    (row2.getFiller() instanceof OWLClass)) {
//                    if (row2.getMax() >= row1.getMax()) {
//                        if (row2.getMax() <= row1.getMax()) {
//                            if (specialisesCurrentFiller((OWLClass) newFiller)) {
//                                // @@TODO test the property
//                                canMerge = true;
//                            }
//                        }
//                    }
//                }
//        return canMerge;
//    }
//
//    private boolean specialisesCurrentFiller(OWLClass newValue, OWLClass oldValue) {
//        return mngr.getOWLClassHierarchyProvider().getAncestors(newValue).contains(oldValue);
//    }
}
