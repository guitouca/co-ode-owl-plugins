package org.coode.cardinality.ui;

import org.coode.cardinality.model.CardinalityRow;
import org.coode.cardinality.model.CardinalityTableModel;
import org.coode.cardinality.prefs.CardinalityProperties;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;
import org.protege.editor.owl.ui.table.BasicLinkedOWLObjectTable;
import org.semanticweb.owl.model.OWLClass;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;

/**
 * Author: drummond<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: Aug 25, 2006<br><br>
 * <p/>
 * nick.drummond@cs.manchester.ac.uk<br>
 * www.cs.man.ac.uk/~drummond<br><br>
 */
public class CardinalityTable extends BasicLinkedOWLObjectTable {

    private static int[] colWidth;

    private TableCellRenderer defaultRen;
    private TableCellRenderer entityRen;
    private TableCellRenderer closureRen;

    private TableCellEditor defaultEditor;
    private TableCellEditor propEditor;
    private TableCellEditor fillerEditor;

    public CardinalityTable(OWLEditorKit eKit) {
        super(new CardinalityTableModel(eKit.getOWLModelManager()), eKit);

        // create table columns
        TableColumnModel tcm = getColumnModel();
        for (int i = getModel().getColumnCount() - 1; i >= 0; i--) {
            TableColumn tc = tcm.getColumn(i);
            if (i != CardinalityTableModel.COL_FILLER &&
                i != CardinalityTableModel.COL_PROP) {
                tc.setMinWidth(Math.max(getColumnWidth(i), 30));
                tc.setMaxWidth(getColumnWidth(i));
            }
        }

        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // create the renderers
        entityRen = new OWLCellRenderer(eKit);
        defaultRen = new DefaultTableCellRenderer();
        closureRen = new ClosureRenderer(eKit.getOWLModelManager());

        // create the editors
        propEditor = new DefaultCellEditor(new PropCombo(eKit)) {
            public boolean isCellEditable(EventObject eventObject) {
                if (eventObject instanceof MouseEvent) {
                    return ((MouseEvent) eventObject).getClickCount() >= 2;
                }
                else {
                    return super.isCellEditable(eventObject);
                }
            }
        };
        JTextField field = new JTextField();
        field.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                defaultEditor.cancelCellEditing();
            }
        });
        fillerEditor = new OWLDescriptionCellEditor(eKit);
        defaultEditor = new DefaultCellEditor(field);
    }

    public CardinalityTableModel getModel() {
        return (CardinalityTableModel) super.getModel();
    }

    public void setSubject(OWLClass instance) {
        clearSelection();
        getModel().setSubject(instance);
    }

    public OWLClass getSubject() {
        return getModel().getSubject();
    }

    public void reload() {
        getModel().reload();
    }

    public Collection<CardinalityRow> getSelection() {
        Collection<CardinalityRow> selection = new ArrayList<CardinalityRow>();

        // below necessary because getSelectedRows() does not work
        int minRow = getSelectionModel().getMinSelectionIndex();
        int maxRow = getSelectionModel().getMaxSelectionIndex();
        for (int i = minRow; i <= maxRow; i++) {
            if (getSelectionModel().isSelectedIndex(i)) {
                selection.add(getModel().getRestriction(i));
            }
        }
        return selection;
    }

    public void setSelection(CardinalityRow newRestr) {
        int rowIndex = getModel().getRow(newRestr);
        selectionModel.setSelectionInterval(rowIndex, rowIndex);
    }

    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);

        if (c != null) {
            CardinalityRow r = getModel().getRestriction(row);
            if (r.isReadOnly()) {
                c.setForeground(Color.GRAY);
            }
            else {
                c.setForeground(Color.BLACK);
            }
        }
        return c;
    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        switch (col) {
            case CardinalityTableModel.COL_PROP:
            case CardinalityTableModel.COL_FILLER:
                return entityRen;
            case CardinalityTableModel.COL_CLOSED:
                return closureRen;
            case CardinalityTableModel.COL_MIN:
            case CardinalityTableModel.COL_MAX:
            default:
                return defaultRen;
        }
    }

    public TableCellEditor getCellEditor(int row, int col) {
        switch (col) {
            case CardinalityTableModel.COL_PROP:
                return propEditor;
            case CardinalityTableModel.COL_FILLER:
                return fillerEditor;
            case CardinalityTableModel.COL_CLOSED:
                return super.getCellEditor(row, col);
            case CardinalityTableModel.COL_MIN:
            case CardinalityTableModel.COL_MAX:
            default:
                return defaultEditor;
        }
    }

    public int getColumnWidth(int index) {
        if (colWidth == null) {
            int columnCount = getModel().getColumnCount();
            colWidth = new int[columnCount];
            for (int i = 0; i < columnCount; i++) {
                String value = CardinalityProperties.getInstance().getProperty("col.width." + index);
                colWidth[i] = Integer.parseInt(value);
            }
        }
        return colWidth[index];
    }

    protected boolean isHeaderVisible() {
        return true;
    }
}
