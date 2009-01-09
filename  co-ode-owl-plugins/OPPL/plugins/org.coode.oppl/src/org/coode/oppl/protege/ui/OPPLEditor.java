package org.coode.oppl.protege.ui;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.coode.oppl.OPPLQuery;
import org.coode.oppl.OPPLScript;
import org.coode.oppl.syntax.OPPLParser;
import org.coode.oppl.variablemansyntax.ConstraintSystem;
import org.coode.oppl.variablemansyntax.Variable;
import org.coode.oppl.variablemansyntax.generated.GeneratedVariable;
import org.protege.editor.core.ui.list.MList;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.core.ui.util.VerifyingOptionPane;
import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLAxiomChange;

public class OPPLEditor extends JSplitPane implements VerifiedInputEditor {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6106150715610094308L;

	private class OPPLActionList extends ActionList {
		public OPPLActionList() {
			super(OPPLEditor.this.owlEditorKit,
					OPPLEditor.this.constraintSystem);
			((DefaultListModel) this.getModel())
					.addElement(new ActionListSectionHeader());
		}

		@Override
		protected void handleAdd() {
			final OWLAxiomChangeEditor actionEditor = new OWLAxiomChangeEditor(
					OPPLEditor.this.owlEditorKit,
					OPPLEditor.this.constraintSystem);
			final VerifyingOptionPane optionPane = new VerifyingOptionPane(
					actionEditor) {
				/**
				 * 
				 */
				private static final long serialVersionUID = 7816306100172449202L;

				/**
				 * 
				 */
				@Override
				public void selectInitialValue() {
					// This is overridden so that the option pane dialog default
					// button
					// doesn't get the focus.
				}
			};
			final InputVerificationStatusChangedListener verificationListener = new InputVerificationStatusChangedListener() {
				public void verifiedStatusChanged(boolean verified) {
					optionPane.setOKEnabled(verified);
				}
			};
			actionEditor.addStatusChangedListener(verificationListener);
			final JDialog dlg = optionPane.createDialog(
					OPPLEditor.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(false);
			dlg.setTitle("Action editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(OPPLEditor.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						OWLAxiomChange action = actionEditor
								.getOwlAxiomChange();
						DefaultListModel model = (DefaultListModel) OPPLEditor.this.actionList
								.getModel();
						model.addElement(new OPPLEditorActionListItem(action,
								true, true));
						OPPLEditor.this.handleChange();
					}
					actionEditor
							.removeStatusChangedListener(verificationListener);
					actionEditor.dispose();
				}
			});
			dlg.setVisible(true);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -3297222035586803090L;
	}

	private class OPPLEditorActionListItem extends ActionListItem {
		public OPPLEditorActionListItem(OWLAxiomChange axiomChange,
				boolean isEditable, boolean isDeleteable) {
			super(axiomChange, isEditable, isDeleteable);
		}

		@Override
		public void handleEdit() {
			final OWLAxiomChangeEditor actionEditor = new OWLAxiomChangeEditor(
					OPPLEditor.this.owlEditorKit,
					OPPLEditor.this.constraintSystem);
			actionEditor.setOWLAxiomChange(this.getAxiomChange());
			final VerifyingOptionPane optionPane = new VerifyingOptionPane(
					actionEditor) {
				/**
				 * 
				 */
				private static final long serialVersionUID = 7816306100172449202L;

				/**
				 * 
				 */
				@Override
				public void selectInitialValue() {
					// This is overridden so that the option pane dialog default
					// button
					// doesn't get the focus.
				}
			};
			final InputVerificationStatusChangedListener verificationListener = new InputVerificationStatusChangedListener() {
				public void verifiedStatusChanged(boolean verified) {
					optionPane.setOKEnabled(verified);
				}
			};
			actionEditor.addStatusChangedListener(verificationListener);
			final JDialog dlg = optionPane.createDialog(
					OPPLEditor.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(false);
			dlg.setTitle("Action editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(OPPLEditor.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						OWLAxiomChange action = actionEditor
								.getOwlAxiomChange();
						DefaultListModel model = (DefaultListModel) OPPLEditor.this.actionList
								.getModel();
						model.removeElement(OPPLEditor.this.actionList
								.getSelectedValue());
						model.addElement(new OPPLEditorActionListItem(action,
								true, true));
						OPPLEditor.this.handleChange();
					}
					actionEditor
							.removeStatusChangedListener(verificationListener);
					actionEditor.dispose();
				}
			});
			dlg.setVisible(true);
		}
	}

	/**
	 * @author Luigi Iannone
	 * 
	 */
	public class OPPLVariableListItem extends VariableListItem {
		/**
		 * @param variable
		 * @param owlEditorKit
		 */
		public OPPLVariableListItem(Variable variable,
				OWLEditorKit owlEditorKit, boolean isEditable,
				boolean isDeleatable) {
			super(variable, owlEditorKit, isEditable, isDeleatable);
		}

		/**
		 * @see org.protege.editor.core.ui.list.MListItem#getTooltip()
		 */
		@Override
		public String getTooltip() {
			return this.getVariable().toString();
		}

		/**
		 * @see org.protege.editor.core.ui.list.MListItem#handleEdit()
		 */
		@Override
		public void handleEdit() {
			ConstraintSystem cs = OPPLEditor.this.constraintSystem;
			final AbstractVariableEditor variableEditor = this.getVariable() instanceof GeneratedVariable ? new GeneratedVariableEditor(
					OPPLEditor.this.owlEditorKit, cs)
					: new VariableEditor(OPPLEditor.this.owlEditorKit, cs);
			variableEditor.setVariable(this.getVariable());
			final VerifyingOptionPane optionPane = new VerifyingOptionPane(
					variableEditor) {
				/**
				 * 
				 */
				private static final long serialVersionUID = 7816306100172449202L;

				/**
				 * 
				 */
				@Override
				public void selectInitialValue() {
					// This is overridden so that the option pane dialog default
					// button
					// doesn't get the focus.
				}
			};
			final InputVerificationStatusChangedListener verificationListener = new InputVerificationStatusChangedListener() {
				public void verifiedStatusChanged(boolean verified) {
					optionPane.setOKEnabled(verified);
				}
			};
			variableEditor.addStatusChangedListener(verificationListener);
			final JDialog dlg = optionPane.createDialog(
					OPPLEditor.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(false);
			dlg.setTitle("Action editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(OPPLEditor.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						Variable variable = variableEditor.getVariable();
						DefaultListModel model = (DefaultListModel) OPPLEditor.this.variableList
								.getModel();
						model.removeElement(OPPLEditor.this.variableList
								.getSelectedValue());
						OPPLEditor.this.variableList
								.placeListItem(new OPPLVariableListItem(
										variable, OPPLEditor.this.owlEditorKit,
										true, true));
						OPPLVariableListItem.this.purgeActions(variable);
						OPPLEditor.this.handleChange();
					}
					variableEditor
							.removeStatusChangedListener(verificationListener);
					variableEditor.dispose();
					OPPLEditor.this.handleChange();
				}
			});
			dlg.setVisible(true);
		}

		@Override
		public boolean handleDelete() {
			Variable v = this.getVariable();
			this.purgeActions(v);
			OPPLEditor.this.handleChange();
			return true;
		}

		/**
		 * @param v
		 */
		private void purgeActions(Variable v) {
			DefaultListModel model = (DefaultListModel) OPPLEditor.this.actionList
					.getModel();
			for (int i = 0; i < model.getSize(); i++) {
				Object e = model.getElementAt(i);
				if (e instanceof OPPLEditorActionListItem) {
					OWLAxiomChange action = ((OPPLEditorActionListItem) e)
							.getAxiomChange();
					OWLAxiom axiom = action.getAxiom();
					Set<Variable> axiomVariables = OPPLEditor.this.constraintSystem
							.getAxiomVariables(axiom);
					if (axiomVariables.contains(v)) {
						model.remove(i);
					}
				}
			}
		}
	}

	private class OPPLVariableList extends VariableList implements
			ListDataListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2540053052502672472L;

		@Override
		protected void handleAdd() {
			final AbstractVariableEditor variableEditor = this
					.getSelectedValue() instanceof InputVariableSectionHeader ? new VariableEditor(
					OPPLEditor.this.owlEditorKit,
					OPPLEditor.this.constraintSystem)
					: new GeneratedVariableEditor(OPPLEditor.this.owlEditorKit,
							OPPLEditor.this.constraintSystem);
			final VerifyingOptionPane optionPane = new VerifyingOptionPane(
					variableEditor) {
				/**
				 * 
				 */
				private static final long serialVersionUID = 7217535942418544769L;

				@Override
				public void selectInitialValue() {
					// This is overridden so that the option pane dialog default
					// button
					// doesn't get the focus.
				}
			};
			final InputVerificationStatusChangedListener verificationListener = new InputVerificationStatusChangedListener() {
				public void verifiedStatusChanged(boolean verified) {
					optionPane.setOKEnabled(verified);
				}
			};
			variableEditor.addStatusChangedListener(verificationListener);
			final JDialog dlg = optionPane.createDialog(
					OPPLEditor.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(false);
			dlg.setTitle("Variable editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(OPPLEditor.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						Variable variable = variableEditor.getVariable();
						OPPLVariableListItem listItem = new OPPLVariableListItem(
								variable, OPPLEditor.this.owlEditorKit, true,
								true);
						OPPLVariableList.this.placeListItem(listItem);
						OPPLEditor.this.handleChange();
					}
					variableEditor
							.removeStatusChangedListener(verificationListener);
					variableEditor.dispose();
				}
			});
			dlg.setVisible(true);
		}

		public OPPLVariableList(OWLEditorKit owlEditorKit) {
			super(owlEditorKit);
			((DefaultListModel) this.getModel())
					.addElement(new InputVariableSectionHeader());
			((DefaultListModel) this.getModel())
					.addElement(new GeneratedVariableSectionHeader());
			this.getModel().addListDataListener(this);
		}

		public void contentsChanged(ListDataEvent e) {
			this.updatePatternModel();
		}

		/**
		 * 
		 */
		private void updatePatternModel() {
			ListModel model = this.getModel();
			for (int i = 0; i < model.getSize(); i++) {
				Object element = model.getElementAt(i);
				if (element instanceof OPPLVariableListItem) {
					OPPLVariableListItem item = (OPPLVariableListItem) element;
					if (OPPLEditor.this.opplScript != null) {
						if (!OPPLEditor.this.opplScript.getVariables()
								.contains(item.getVariable())) {
							OPPLEditor.this.opplScript.addVariable(item
									.getVariable());
						}
					}
				}
			}
		}

		public void intervalAdded(ListDataEvent e) {
			this.updatePatternModel();
		}

		public void intervalRemoved(ListDataEvent e) {
			this.updatePatternModel();
		}

		/**
		 * @param listItem
		 */
		protected void placeListItem(OPPLVariableListItem listItem) {
			DefaultListModel model = (DefaultListModel) OPPLVariableList.this
					.getModel();
			int i = -1;
			if (listItem.getVariable() instanceof GeneratedVariable) {
				i = model.getSize();
			} else {
				Enumeration<?> elements = model.elements();
				boolean found = false;
				while (!found && elements.hasMoreElements()) {
					i++;
					Object element = elements.nextElement();
					found = element instanceof GeneratedVariableSectionHeader;
				}
				if (!found) {
					throw new RuntimeException("Section lost");
				}
			}
			model.add(i, listItem);
		}
	}

	private Set<InputVerificationStatusChangedListener> listeners = new HashSet<InputVerificationStatusChangedListener>();
	private OWLEditorKit owlEditorKit;
	private OPPLVariableList variableList;
	private OPPLSelectClauseList selectList;
	private MList constraintList;
	private ActionList actionList;
	private ConstraintSystem constraintSystem = OPPLParser.getOPPLFactory()
			.createConstraintSystem();
	private OPPLScript opplScript;

	public OPPLEditor(OWLEditorKit owlEditorKit) {
		this.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				OPPLEditor.this.setDividerLocation(.5);
			}
		});
		this.owlEditorKit = owlEditorKit;
		// Setup the variable list on the left
		JPanel variablePanel = new JPanel(new BorderLayout());
		this.variableList = new OPPLVariableList(this.owlEditorKit);
		variablePanel.add(this.variableList);
		this.add(ComponentFactory.createScrollPane(this.variableList),
				JSplitPane.LEFT);
		// Now setup the right hand side panel which will be further split into
		// queries and actions
		final JSplitPane queryActionSplitPane = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				queryActionSplitPane.setDividerLocation(.5);
			}
		});
		// Now setup the query split pane
		final JSplitPane queryConstraintSplitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				queryConstraintSplitPane.setDividerLocation(.5);
			}
		});
		// Now the select part
		JPanel queryPanel = new JPanel(new BorderLayout());
		this.selectList = new OPPLSelectClauseList(this.owlEditorKit,
				this.constraintSystem);
		this.selectList.getModel().addListDataListener(new ListDataListener() {
			public void contentsChanged(ListDataEvent e) {
				OPPLEditor.this.handleChange();
			}

			public void intervalAdded(ListDataEvent e) {
				OPPLEditor.this.handleChange();
			}

			public void intervalRemoved(ListDataEvent e) {
				OPPLEditor.this.handleChange();
			}
		});
		queryPanel.add(ComponentFactory.createScrollPane(this.selectList));
		// Now the constraints
		JPanel constraintPanel = new JPanel(new BorderLayout());
		this.constraintList = new OPPLConstraintList(this.owlEditorKit,
				this.constraintSystem);
		this.constraintList.getModel().addListDataListener(
				new ListDataListener() {
					public void contentsChanged(ListDataEvent e) {
						OPPLEditor.this.handleChange();
					}

					public void intervalAdded(ListDataEvent e) {
						OPPLEditor.this.handleChange();
					}

					public void intervalRemoved(ListDataEvent e) {
						OPPLEditor.this.handleChange();
					}
				});
		constraintPanel.add(ComponentFactory
				.createScrollPane(this.constraintList));
		queryConstraintSplitPane.add(queryPanel, JSplitPane.LEFT);
		queryConstraintSplitPane.add(constraintPanel, JSplitPane.RIGHT);
		// Now setup the action panel
		JPanel actionPanel = new JPanel(new BorderLayout());
		this.actionList = new OPPLActionList();
		actionPanel.add(ComponentFactory.createScrollPane(this.actionList));
		queryActionSplitPane.add(queryConstraintSplitPane, JSplitPane.TOP);
		queryActionSplitPane.add(actionPanel, JSplitPane.BOTTOM);
		this.add(queryActionSplitPane, JSplitPane.RIGHT);
	}

	private boolean check() {
		// The numbers include the section headers
		return this.variableList.getModel().getSize() > 2
				&& (this.selectList.getModel().getSize() > 1 || this.actionList
						.getModel().getSize() > 1);
	}

	public void handleChange() {
		boolean isValid = this.check();
		if (isValid) {
			this.opplScript = OPPLParser.getOPPLFactory().buildOPPLScript(
					this.constraintSystem, this.getVariables(),
					this.getOPPLQuery(), this.getActions());
		}
		this.notifyListeners(isValid);
	}

	private void notifyListeners(boolean status) {
		for (InputVerificationStatusChangedListener listener : this.listeners) {
			listener.verifiedStatusChanged(status);
		}
	}

	private OPPLQuery getOPPLQuery() {
		OPPLQuery toReturn = OPPLParser.getOPPLFactory().buildNewQuery(
				this.constraintSystem);
		ListModel model = this.selectList.getModel();
		for (int i = 0; i < model.getSize(); i++) {
			Object e = model.getElementAt(i);
			if (e instanceof OPPLSelectClauseListItem) {
				OPPLSelectClauseListItem selectListItem = (OPPLSelectClauseListItem) e;
				OWLAxiom axiom = selectListItem.getAxiom();
				if (selectListItem.isAsserted()) {
					toReturn.addAssertedAxiom(axiom);
				} else {
					toReturn.addAxiom(axiom);
				}
			}
		}
		model = this.constraintList.getModel();
		for (int i = 0; i < model.getSize(); i++) {
			Object e = model.getElementAt(i);
			if (e instanceof OPPLConstraintListItem) {
				OPPLConstraintListItem constraintListItem = (OPPLConstraintListItem) e;
				toReturn.addConstraint(constraintListItem.getConstraint());
			}
		}
		return toReturn;
	}

	private List<OWLAxiomChange> getActions() {
		ListModel model = this.actionList.getModel();
		// There is a section header so the initial capacity is the size -1
		List<OWLAxiomChange> toReturn = new ArrayList<OWLAxiomChange>(model
				.getSize() - 1);
		for (int i = 0; i < model.getSize(); i++) {
			Object elementAt = model.getElementAt(i);
			if (elementAt instanceof ActionListItem) {
				ActionListItem actionListItem = (ActionListItem) elementAt;
				toReturn.add(actionListItem.getAxiomChange());
			}
		}
		return toReturn;
	}

	private List<Variable> getVariables() {
		ListModel model = this.variableList.getModel();
		// There is a section header so the initial capacity is the size -1
		List<Variable> toReturn = new ArrayList<Variable>(model.getSize() - 1);
		for (int i = 0; i < model.getSize(); i++) {
			Object elementAt = model.getElementAt(i);
			if (elementAt instanceof VariableListItem) {
				VariableListItem variableListItem = (VariableListItem) elementAt;
				toReturn.add(variableListItem.getVariable());
			}
		}
		return toReturn;
	}

	public void addStatusChangedListener(
			InputVerificationStatusChangedListener listener) {
		this.listeners.add(listener);
		listener.verifiedStatusChanged(this.check());
	}

	public void removeStatusChangedListener(
			InputVerificationStatusChangedListener listener) {
		this.listeners.remove(listener);
	}

	/**
	 * @return the opplScript
	 */
	public OPPLScript getOPPLScript() {
		return this.opplScript;
	}

	/**
	 * @param opplScript
	 *            the opplScript to set
	 */
	public void setOPPLScript(OPPLScript opplScript) {
		this.opplScript = opplScript;
	}

	public void clear() {
		((DefaultListModel) this.variableList.getModel()).clear();
		((DefaultListModel) this.selectList.getModel()).clear();
		((DefaultListModel) this.constraintList.getModel()).clear();
		((DefaultListModel) this.actionList.getModel()).clear();
		this.constraintSystem = OPPLParser.getOPPLFactory()
				.createConstraintSystem();
	}
}