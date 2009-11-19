/**
 * Copyright (C) 2008, University of Manchester
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
package org.coode.patterns.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.coode.oppl.protege.ui.AbstractVariableEditor;
import org.coode.oppl.protege.ui.ActionList;
import org.coode.oppl.protege.ui.ActionListItem;
import org.coode.oppl.protege.ui.ActionListModel;
import org.coode.oppl.protege.ui.GeneratedVariableEditor;
import org.coode.oppl.protege.ui.GeneratedVariableSectionHeader;
import org.coode.oppl.protege.ui.InputVariableSectionHeader;
import org.coode.oppl.protege.ui.OWLAxiomChangeEditor;
import org.coode.oppl.protege.ui.VariableEditor;
import org.coode.oppl.protege.ui.VariableList;
import org.coode.oppl.protege.ui.VariableListItem;
import org.coode.oppl.protege.ui.message.Error;
import org.coode.oppl.protege.ui.message.MessageListCellRenderer;
import org.coode.oppl.variablemansyntax.ConstraintSystem;
import org.coode.oppl.variablemansyntax.Variable;
import org.coode.oppl.variablemansyntax.generated.GeneratedVariable;
import org.coode.patterns.EmptyActionListException;
import org.coode.patterns.EmptyVariableListException;
import org.coode.patterns.PatternConstraintSystem;
import org.coode.patterns.PatternModel;
import org.coode.patterns.PatternModelChangeListener;
import org.coode.patterns.UnsuitableOPPLScriptException;
import org.coode.patterns.syntax.PatternParser;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.core.ui.util.VerifyingOptionPane;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.frame.AbstractOWLFrameSectionRowObjectEditor;
import org.protege.editor.owl.ui.frame.OWLFrameSectionRowObjectEditor;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLAxiomChange;

/**
 * @author Luigi Iannone
 * 
 *         Jun 10, 2008
 */
public class PatternBuilder extends
		AbstractOWLFrameSectionRowObjectEditor<PatternModel> implements
		OWLFrameSectionRowObjectEditor<PatternModel>, VerifiedInputEditor,
		PatternModelChangeListener {
	private final class PatternBuilderModel {
		private String name = "";
		private final List<Variable> variables = new ArrayList<Variable>();
		private final List<OWLAxiomChange> actions = new ArrayList<OWLAxiomChange>();
		private Variable returnVariable = null;
		private String modelRendering = "";
		private PatternConstraintSystem constraintSystem = PatternParser
				.getPatternModelFactory().createConstraintSystem();

		/**
		 * @return the name
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * @param name
		 *            the name to set
		 */
		public void setName(String name) {
			boolean changed = this.name == null && name != null
					|| !this.name.equals(name);
			if (changed) {
				this.name = name;
				notifyBuilder();
			}
		}

		/**
		 * @return the returnVariable
		 */
		public Variable getReturnVariable() {
			return this.returnVariable;
		}

		/**
		 * @param returnVariable
		 *            the returnVariable to set
		 */
		public void setReturnVariable(Variable returnVariable) {
			boolean changed = this.returnVariable == null
					&& returnVariable != null
					|| !this.returnVariable.equals(returnVariable);
			if (changed) {
				this.returnVariable = returnVariable;
				notifyBuilder();
			}
		}

		/**
		 * @return the variables
		 */
		public List<Variable> getVariables() {
			return new ArrayList<Variable>(this.variables);
		}

		public void addVariable(Variable v) {
			boolean modified = this.variables.add(v);
			if (modified) {
				this.constraintSystem.importVariable(v);
				notifyBuilder();
			}
		}

		public void removeVariable(Variable v) {
			boolean modified = this.variables.remove(v);
			if (modified) {
				purgeActions(v);
				this.constraintSystem.removeVariable(v);
				notifyBuilder();
			}
		}

		/**
		 * @return the actions
		 */
		public List<OWLAxiomChange> getActions() {
			return new ArrayList<OWLAxiomChange>(this.actions);
		}

		public void addAction(OWLAxiomChange action) {
			boolean modified = this.actions.add(action);
			if (modified) {
				notifyBuilder();
			}
		}

		public void removeAction(OWLAxiomChange action) {
			boolean modified = this.actions.remove(action);
			if (modified) {
				notifyBuilder();
			}
		}

		public boolean check() {
			boolean validName = this.name.matches("\\S+");
			if (!validName) {
				PatternBuilder.this.errorListModel.addElement(new Error(
						"Invalid name"));
			}
			boolean enoughVariables = !this.variables.isEmpty();
			boolean enoughActions = !this.actions.isEmpty();
			if (!enoughVariables) {
				PatternBuilder.this.errorListModel.addElement(new Error(
						"No Variables"));
			}
			if (!enoughActions) {
				PatternBuilder.this.errorListModel.addElement(new Error(
						"No actions"));
			}
			StringTokenizer tokenizer = new StringTokenizer(this.modelRendering);
			boolean found = false;
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.startsWith("?")) {
					String variableName = token.trim();
					found = !existsVariable(variableName);
					if (found) {
						PatternBuilder.this.errorListModel
								.addElement(new Error("Invalid variable name "
										+ variableName));
					}
				}
			}
			return validName && enoughVariables && enoughActions && !found;
		}

		private boolean existsVariable(String variableName) {
			boolean found = false;
			Iterator<Variable> it = this.variables.iterator();
			while (!found && it.hasNext()) {
				found = it.next().getName().compareTo(variableName) == 0;
			}
			return found;
		}

		/**
		 * @return the rendering
		 */
		public String getRendering() {
			return this.modelRendering;
		}

		/**
		 * @param rendering
		 *            the rendering to set
		 */
		public void setRendering(String rendering) {
			boolean changed = this.modelRendering == null && rendering != null
					|| this.modelRendering.compareTo(rendering) != 0;
			if (changed) {
				this.modelRendering = rendering;
				notifyBuilder();
			}
		}

		public void notifyBuilder() {
			handleChange();
		}

		public void importPatternModel(PatternModel patternModel) {
			this.constraintSystem = patternModel.getConstraintSystem();
			this.name = patternModel.getName();
			this.variables.clear();
			this.variables.addAll(patternModel.getVariables());
			this.actions.clear();
			this.actions.addAll(patternModel.getActions());
			this.modelRendering = patternModel.getRendering();
			this.returnVariable = patternModel.getReturnVariable();
			notifyBuilder();
		}

		public void reset() {
			this.name = "";
			this.variables.clear();
			this.actions.clear();
			this.returnVariable = null;
			this.modelRendering = "";
			notifyBuilder();
		}

		/**
		 * @return the constraintSystem
		 */
		public final PatternConstraintSystem getConstraintSystem() {
			return this.constraintSystem;
		}

		/**
		 * @param v
		 */
		private void purgeActions(Variable v) {
			Set<OWLAxiomChange> toRemove = new HashSet<OWLAxiomChange>();
			for (OWLAxiomChange action : this.actions) {
				OWLAxiom axiom = action.getAxiom();
				Set<Variable> axiomVariables = PatternBuilder.this.patternBuilderModel
						.getConstraintSystem().getAxiomVariables(axiom);
				if (axiomVariables.contains(v)) {
					toRemove.add(action);
				}
			}
			this.actions.removeAll(toRemove);
		}

		public void replaceVariable(Variable oldVariable, Variable newVariable) {
			boolean modified = this.variables.remove(oldVariable);
			if (modified) {
				if (oldVariable.getType() != newVariable.getType()) {
					purgeActions(oldVariable);
				}
				this.variables.add(newVariable);
				this.constraintSystem.removeVariable(oldVariable);
				this.constraintSystem.importVariable(newVariable);
				notifyBuilder();
			}
		}
	}

	private class PatternBuilderActionList extends ActionList {
		public PatternBuilderActionList() {
			super(PatternBuilder.this.owlEditorKit,
					PatternBuilder.this.patternBuilderModel
							.getConstraintSystem(), true);
		}

		@Override
		protected void handleAdd() {
			final OWLAxiomChangeEditor actionEditor = new OWLAxiomChangeEditor(
					PatternBuilder.this.owlEditorKit,
					PatternBuilder.this.patternBuilderModel
							.getConstraintSystem());
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
					PatternBuilder.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(false);
			dlg.setTitle("Action editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(PatternBuilder.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						OWLAxiomChange action = actionEditor
								.getOwlAxiomChange();
						PatternBuilder.this.patternBuilderModel
								.addAction(action);
					}
					actionEditor
							.removeStatusChangedListener(verificationListener);
					actionEditor.dispose();
				}
			});
			dlg.setVisible(true);
		}

		@Override
		protected void handleDelete() {
			Object selectedValue = getSelectedValue();
			if (PatternBuilderActionListItem.class
					.isAssignableFrom(selectedValue.getClass())) {
				PatternBuilderActionListItem item = (PatternBuilderActionListItem) selectedValue;
				OWLAxiomChange action = item.getAxiomChange();
				PatternBuilder.this.patternBuilderModel.removeAction(action);
			}
		}

		/**
		 *
		 */
		private static final long serialVersionUID = -3297222035586803090L;
	}

	private class PatternBuilderActionListItem extends ActionListItem {
		public PatternBuilderActionListItem(OWLAxiomChange axiomChange,
				boolean isEditable, boolean isDeleteable) {
			super(axiomChange, isEditable, isDeleteable);
		}

		@Override
		public void handleEdit() {
			ConstraintSystem cs = PatternBuilder.this.patternBuilderModel
					.getConstraintSystem();
			final OWLAxiomChangeEditor actionEditor = new OWLAxiomChangeEditor(
					PatternBuilder.this.owlEditorKit, cs);
			actionEditor.setOWLAxiomChange(getAxiomChange());
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
					PatternBuilder.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(false);
			dlg.setTitle("Action editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(PatternBuilder.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						OWLAxiomChange action = actionEditor
								.getOwlAxiomChange();
						DefaultListModel model = (DefaultListModel) PatternBuilder.this.actionList
								.getModel();
						model.removeElement(PatternBuilder.this.actionList
								.getSelectedValue());
						model.addElement(new PatternBuilderActionListItem(
								action, true, true));
						handleChange();
					}
					actionEditor
							.removeStatusChangedListener(verificationListener);
					actionEditor.dispose();
				}
			});
			dlg.setVisible(true);
		}
	}

	private class PatternVariableList extends VariableList implements
			ListDataListener {
		/**
		 *
		 */
		private static final long serialVersionUID = -2540053052502672472L;

		@Override
		protected void handleAdd() {
			final AbstractVariableEditor variableEditor = getSelectedValue() instanceof InputVariableSectionHeader ? new VariableEditor(
					PatternBuilder.this.owlEditorKit,
					PatternBuilder.this.patternBuilderModel
							.getConstraintSystem())
					: new GeneratedVariableEditor(
							PatternBuilder.this.owlEditorKit,
							PatternBuilder.this.patternBuilderModel
									.getConstraintSystem());
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
					PatternBuilder.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(false);
			dlg.setTitle("Variable editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(PatternBuilder.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						Variable variable = variableEditor.getVariable();
						PatternBuilder.this.patternBuilderModel
								.addVariable(variable);
					}
					variableEditor
							.removeStatusChangedListener(verificationListener);
					variableEditor.dispose();
				}
			});
			dlg.setVisible(true);
		}

		@Override
		protected void handleDelete() {
			Object selectedValue = getSelectedValue();
			if (PatternBuilderVariableListItem.class
					.isAssignableFrom(selectedValue.getClass())) {
				PatternBuilderVariableListItem item = (PatternBuilderVariableListItem) selectedValue;
				PatternBuilder.this.patternBuilderModel.removeVariable(item
						.getVariable());
			}
		}

		public PatternVariableList(OWLEditorKit owlEditorKit) {
			super(owlEditorKit, PatternBuilder.this.patternBuilderModel
					.getConstraintSystem());
			((DefaultListModel) getModel())
					.addElement(new InputVariableSectionHeader());
			((DefaultListModel) getModel())
					.addElement(new GeneratedVariableSectionHeader());
			getModel().addListDataListener(this);
		}

		public void contentsChanged(ListDataEvent e) {
			updatePatternModel();
		}

		/**
		 *
		 */
		private void updatePatternModel() {
			ListModel model = getModel();
			for (int i = 0; i < model.getSize(); i++) {
				Object element = model.getElementAt(i);
				if (element instanceof PatternBuilderVariableListItem) {
					PatternBuilderVariableListItem item = (PatternBuilderVariableListItem) element;
					if (PatternBuilder.this.patternModel != null) {
						if (!PatternBuilder.this.patternModel.getVariables()
								.contains(item.getVariable())) {
							PatternBuilder.this.patternModel.addVariable(item
									.getVariable());
						}
					}
				}
			}
		}

		public void clear() {
			((DefaultListModel) getModel()).clear();
			((DefaultListModel) getModel())
					.addElement(new InputVariableSectionHeader());
			((DefaultListModel) getModel())
					.addElement(new GeneratedVariableSectionHeader());
		}

		public void intervalAdded(ListDataEvent e) {
			updatePatternModel();
		}

		public void intervalRemoved(ListDataEvent e) {
			updatePatternModel();
		}

		/**
		 * @param listItem
		 */
		protected void placeListItem(PatternBuilderVariableListItem listItem) {
			DefaultListModel model = (DefaultListModel) PatternVariableList.this
					.getModel();
			int i = -1;
			if (listItem.getVariable() instanceof GeneratedVariable<?>) {
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

	/**
	 * @author Luigi Iannone
	 * 
	 */
	public class PatternBuilderVariableListItem extends VariableListItem {
		/**
		 * @param variable
		 * @param owlEditorKit
		 */
		public PatternBuilderVariableListItem(Variable variable,
				OWLEditorKit owlEditorKit, boolean isEditable,
				boolean isDeleatable) {
			super(variable, owlEditorKit, isEditable, isDeleatable);
		}

		/**
		 * @see org.protege.editor.core.ui.list.MListItem#getTooltip()
		 */
		@Override
		public String getTooltip() {
			return getVariable().toString();
		}

		/**
		 * @see org.protege.editor.core.ui.list.MListItem#handleEdit()
		 */
		@Override
		public void handleEdit() {
			ConstraintSystem cs = PatternBuilder.this.patternBuilderModel
					.getConstraintSystem();
			final AbstractVariableEditor variableEditor = getVariable() instanceof GeneratedVariable<?> ? new GeneratedVariableEditor(
					PatternBuilder.this.owlEditorKit, cs)
					: new VariableEditor(PatternBuilder.this.owlEditorKit, cs);
			variableEditor.setVariable(getVariable());
			final VerifyingOptionPane optionPane = new VerifyingOptionPane(
					variableEditor) {
				private static final long serialVersionUID = 7816306100172449202L;

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
					PatternBuilder.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(true);
			dlg.setTitle("Action editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(PatternBuilder.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						Object selectedValue = PatternBuilder.this.variableList
								.getSelectedValue();
						if (selectedValue instanceof VariableListItem) {
							VariableListItem item = (VariableListItem) selectedValue;
							Variable oldVariable = item.getVariable();
							// PatternBuilder.this.patternBuilderModel
							// .removeVariable(oldVariable);
							Variable variable = variableEditor.getVariable();
							PatternBuilder.this.patternBuilderModel
									.replaceVariable(oldVariable, variable);
						}
					}
					variableEditor
							.removeStatusChangedListener(verificationListener);
					variableEditor.dispose();
					handleChange();
				}
			});
			dlg.setVisible(true);
		}
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -4071865934355642992L;
	private Set<InputVerificationStatusChangedListener> listeners = new HashSet<InputVerificationStatusChangedListener>();
	private OWLEditorKit owlEditorKit;
	private PatternModel patternModel = null;
	private JPanel mainPanel = new JPanel();
	private PatternVariableList variableList;
	private ActionList actionList;
	// private ExpressionEditor<String> nameEditor;
	private final JTextField nameEditor = new JTextField();
	private JTextField rendering = new JTextField();
	private JCheckBox allowReturnValueCheckBox;
	private final DefaultComboBoxModel returnValueListModel = new DefaultComboBoxModel();
	private final JComboBox returnValuesComboBox = new JComboBox(
			this.returnValueListModel);
	// private PatternConstraintSystem constraintSystem = PatternParser
	// .getPatternModelFactory().createConstraintSystem();
	private PatternBuilderModel patternBuilderModel = new PatternBuilderModel();
	private final JPanel errorPanel = new JPanel(new BorderLayout());
	private final DefaultListModel errorListModel = new DefaultListModel();
	private final JList errorList = new JList(this.errorListModel);

	public PatternBuilder(OWLEditorKit owlEditorKit) {
		this.mainPanel.setLayout(new BorderLayout());
		this.mainPanel.setName("Pattern Builder");
		this.errorPanel.setBorder(ComponentFactory
				.createTitledBorder("Errors:"));
		this.errorPanel.add(ComponentFactory.createScrollPane(this.errorList));
		this.errorPanel.setPreferredSize(new Dimension(200, 75));
		this.errorList.setCellRenderer(new MessageListCellRenderer());
		JPanel builderPanel = new JPanel(new BorderLayout());
		this.owlEditorKit = owlEditorKit;
		this.nameEditor.getDocument().addDocumentListener(
				new DocumentListener() {
					public void changedUpdate(DocumentEvent e) {
						PatternBuilder.this.patternBuilderModel
								.setName(PatternBuilder.this.nameEditor
										.getText());
					}

					public void insertUpdate(DocumentEvent e) {
						PatternBuilder.this.patternBuilderModel
								.setName(PatternBuilder.this.nameEditor
										.getText());
					}

					public void removeUpdate(DocumentEvent e) {
						PatternBuilder.this.patternBuilderModel
								.setName(PatternBuilder.this.nameEditor
										.getText());
					}
				});
		JPanel patternNamePanel = new JPanel(new BorderLayout());
		patternNamePanel.add(this.nameEditor);
		patternNamePanel.setBorder(ComponentFactory
				.createTitledBorder("Pattern name"));
		builderPanel.add(patternNamePanel, BorderLayout.NORTH);
		removeKeyListeners();
		JPanel patternBodyPanel = new JPanel(new BorderLayout());
		this.variableList = new PatternVariableList(this.owlEditorKit);
		this.actionList = new PatternBuilderActionList();
		// this.actionList.getModel().addListDataListener(this.actionListListener);
		JScrollPane variablePane = ComponentFactory
				.createScrollPane(this.variableList);
		patternBodyPanel.add(variablePane, BorderLayout.NORTH);
		patternBodyPanel.add(
				ComponentFactory.createScrollPane(this.actionList),
				BorderLayout.CENTER);
		builderPanel.add(patternBodyPanel, BorderLayout.CENTER);
		this.rendering.getDocument().addDocumentListener(
				new DocumentListener() {
					public void changedUpdate(DocumentEvent e) {
						PatternBuilder.this.patternBuilderModel
								.setRendering(PatternBuilder.this.rendering
										.getText());
					}

					public void insertUpdate(DocumentEvent e) {
						PatternBuilder.this.patternBuilderModel
								.setRendering(PatternBuilder.this.rendering
										.getText());
					}

					public void removeUpdate(DocumentEvent e) {
						PatternBuilder.this.patternBuilderModel
								.setRendering(PatternBuilder.this.rendering
										.getText());
					}
				});
		JPanel renderingPanelBorder = new JPanel(new BorderLayout());
		renderingPanelBorder.setBorder(ComponentFactory
				.createTitledBorder("Rendering "));
		renderingPanelBorder.add(ComponentFactory
				.createScrollPane(this.rendering));
		JPanel returnPanelBorder = new JPanel(new BorderLayout());
		returnPanelBorder.setBorder(ComponentFactory
				.createTitledBorder("Return"));
		this.allowReturnValueCheckBox = new JCheckBox("Allow Return Value");
		this.allowReturnValueCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PatternBuilder.this.returnValuesComboBox
						.setEnabled(PatternBuilder.this.allowReturnValueCheckBox
								.isSelected());
			}
		});
		this.returnValuesComboBox.setEnabled(false);
		this.returnValuesComboBox.setRenderer(this.variableList
				.getVariableListCellRenderer());
		this.returnValuesComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object selectedItem = PatternBuilder.this.returnValuesComboBox
						.getSelectedItem();
				if (selectedItem instanceof VariableListItem) {
					VariableListItem item = (VariableListItem) selectedItem;
					Variable variable = item.getVariable();
					PatternBuilder.this.patternBuilderModel
							.setReturnVariable(variable);
				}
			}
		});
		returnPanelBorder
				.add(this.allowReturnValueCheckBox, BorderLayout.NORTH);
		returnPanelBorder.add(this.returnValuesComboBox, BorderLayout.CENTER);
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(renderingPanelBorder, BorderLayout.NORTH);
		southPanel.add(returnPanelBorder, BorderLayout.CENTER);
		builderPanel.add(southPanel, BorderLayout.SOUTH);
		builderPanel.revalidate();
		this.mainPanel.add(this.errorPanel, BorderLayout.NORTH);
		this.mainPanel.add(builderPanel, BorderLayout.CENTER);
	}

	/**
	 * @see org.protege.editor.owl.ui.frame.VerifiedInputEditor#addStatusChangedListener(org.protege.editor.owl.ui.frame.InputVerificationStatusChangedListener)
	 */
	public void addStatusChangedListener(
			InputVerificationStatusChangedListener listener) {
		this.listeners.add(listener);
		notifyListener(listener, this.patternBuilderModel.check());
	}

	private void updateGUI() {
		String name = this.patternBuilderModel.getName();
		if (name.compareTo(this.nameEditor.getText()) != 0) {
			this.nameEditor.setText(name);
		}
		List<Variable> variables = this.patternBuilderModel.getVariables();
		this.variableList.clear();
		for (Variable variable : variables) {
			PatternBuilderVariableListItem variableListItem = new PatternBuilderVariableListItem(
					variable, this.owlEditorKit, true, true);
			this.variableList.placeListItem(variableListItem);
		}
		this.actionList.clear();
		List<OWLAxiomChange> actions = this.patternBuilderModel.getActions();
		for (OWLAxiomChange axiomChange : actions) {
			PatternBuilderActionListItem actionItem = new PatternBuilderActionListItem(
					axiomChange, true, true);
			((ActionListModel) this.actionList.getModel())
					.addElement(actionItem);
		}
		Variable returnVariable = this.patternBuilderModel.getReturnVariable();
		this.returnValueListModel.removeAllElements();
		HashSet<Variable> returnVariables = new HashSet<Variable>(variables);
		returnVariables.add(this.patternBuilderModel.getConstraintSystem()
				.getThisClassVariable());
		for (Variable variable : returnVariables) {
			PatternBuilderVariableListItem item = new PatternBuilderVariableListItem(
					variable, this.owlEditorKit, false, false);
			if (this.returnValueListModel.getIndexOf(item) == -1) {
				this.returnValueListModel.addElement(item);
			}
			if (variable.equals(returnVariable)) {
				this.returnValuesComboBox.setSelectedItem(item);
			}
		}
		if (this.rendering.getText().compareTo(
				this.patternBuilderModel.getRendering()) != 0) {
			this.rendering.setText(this.patternBuilderModel.getRendering());
		}
		this.mainPanel.revalidate();
	}

	/**
	 * @see org.protege.editor.owl.ui.frame.VerifiedInputEditor#removeStatusChangedListener(org.protege.editor.owl.ui.frame.InputVerificationStatusChangedListener)
	 */
	public void removeStatusChangedListener(
			InputVerificationStatusChangedListener listener) {
		this.listeners.remove(listener);
	}

	public PatternModel getPatternModel() {
		return this.patternModel;
	}

	public void handleChange() {
		this.patternModel = null;
		this.errorListModel.clear();
		boolean newState = this.patternBuilderModel.check();
		if (newState) {
			List<Variable> variables = this.patternBuilderModel.getVariables();
			List<OWLAxiomChange> actions = this.patternBuilderModel
					.getActions();
			try {
				this.patternModel = PatternParser.getPatternModelFactory()
						.createPatternModel(this.nameEditor.getText(),
								variables, actions, null,
								this.rendering.getText(),
								this.patternBuilderModel.getConstraintSystem());
				if (this.allowReturnValueCheckBox.isSelected()) {
					this.patternModel
							.setReturnVariable(((VariableListItem) this.returnValuesComboBox
									.getSelectedItem()).getVariable());
				}
			} catch (EmptyVariableListException e) {
				this.patternModel = null;
				this.errorListModel.addElement(new Error("No variables"));
			} catch (EmptyActionListException e) {
				this.patternModel = null;
				this.errorListModel.addElement(new Error("No actions"));
			} catch (UnsuitableOPPLScriptException e) {
				this.patternModel = null;
				this.errorListModel.addElement(new Error("Failed "
						+ e.getMessage()));
			}
		} else {
			this.patternModel = null;
		}
		this.errorPanel.setVisible(!this.errorListModel.isEmpty());
		updateGUI();
		notifyListeners(newState);
	}

	/**
	 * @param newState
	 */
	private void notifyListeners(boolean newState) {
		for (InputVerificationStatusChangedListener listener : this.listeners) {
			notifyListener(listener, newState);
		}
	}

	/**
	 * @param listener
	 */
	private void notifyListener(
			InputVerificationStatusChangedListener listener, boolean newState) {
		listener.verifiedStatusChanged(newState);
	}

	public void clear() {
		this.patternBuilderModel.reset();
	}

	public void dispose() {
	}

	public PatternModel getEditedObject() {
		return this.patternModel;
	}

	public JComponent getEditorComponent() {
		return this.mainPanel;
	}

	/**
	 * @param patternModel
	 *            the patternModel to set
	 */
	public void setPatternModel(PatternModel patternModel) {
		clear();
		this.patternBuilderModel.importPatternModel(patternModel);
	}

	/**
	 *
	 */
	private void removeKeyListeners() {
		KeyListener[] keyListeners = this.nameEditor.getKeyListeners();
		for (KeyListener keyListener : keyListeners) {
			this.nameEditor.removeKeyListener(keyListener);
		}
	}
}