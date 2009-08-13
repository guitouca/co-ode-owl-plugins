package org.coode.oppl.protege.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.coode.oppl.AbstractConstraint;
import org.coode.oppl.ConstraintVisitorEx;
import org.coode.oppl.InCollectionConstraint;
import org.coode.oppl.InequalityConstraint;
import org.coode.oppl.OPPLQuery;
import org.coode.oppl.OPPLScript;
import org.coode.oppl.protege.ui.message.Error;
import org.coode.oppl.protege.ui.message.MessageListCellRenderer;
import org.coode.oppl.protege.ui.rendering.VariableOWLCellRenderer;
import org.coode.oppl.syntax.OPPLParser;
import org.coode.oppl.utils.NamedVariableDetector;
import org.coode.oppl.validation.OPPLScriptValidator;
import org.coode.oppl.variablemansyntax.ConstraintSystem;
import org.coode.oppl.variablemansyntax.Variable;
import org.coode.oppl.variablemansyntax.generated.GeneratedVariable;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.core.ui.util.VerifyingOptionPane;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLAxiomChange;
import org.semanticweb.owl.model.OWLObject;

public class OPPLBuilder extends JSplitPane implements VerifiedInputEditor {
	private final class OPPLBuilderModel {
		private final List<OWLAxiomChange> actions = new ArrayList<OWLAxiomChange>();
		private final List<OWLAxiom> assertedQueryAxioms = new ArrayList<OWLAxiom>();
		private final List<OWLAxiom> plainQueryAxioms = new ArrayList<OWLAxiom>();
		private final List<AbstractConstraint> constraints = new ArrayList<AbstractConstraint>();
		private ConstraintSystem constraintSystem = OPPLParser.getOPPLFactory()
				.createConstraintSystem();
		private final List<Variable> variables = new ArrayList<Variable>();

		public void addAction(OWLAxiomChange action) {
			boolean modified = this.actions.add(action);
			if (modified) {
				this.notifyBuilder();
			}
		}

		public void addVariable(Variable v) {
			boolean modified = this.variables.add(v);
			if (modified) {
				this.constraintSystem.importVariable(v);
				this.notifyBuilder();
			}
		}

		public boolean check() {
			boolean enoughVariables = !this.variables.isEmpty();
			boolean enoughQueries = !this.assertedQueryAxioms.isEmpty()
					|| !this.plainQueryAxioms.isEmpty()
					|| !this.constraints.isEmpty();
			boolean enoughActions = !this.actions.isEmpty();
			boolean areThereMinimalElements = enoughVariables
					&& (enoughQueries || enoughActions);
			OPPLScript builtOPPLScript = OPPLParser.getOPPLFactory()
					.buildOPPLScript(this.constraintSystem,
							this.getVariables(), this.getOPPLQuery(),
							this.getActions());
			if (!enoughVariables) {
				OPPLBuilder.this.errorListModel.addElement(new Error(
						"No variables "));
			}
			if (!areThereMinimalElements) {
				OPPLBuilder.this.errorListModel
						.addElement(new Error(
								"The must be at least either one action, or one query, or one constraint"));
			}
			boolean validated = OPPLBuilder.this.validator == null
					|| OPPLBuilder.this.validator.accept(builtOPPLScript);
			if (!validated) {
				OPPLBuilder.this.errorListModel.addElement(new Error(
						"Failed validation: "
								+ OPPLBuilder.this.validator
										.getValidationRuleDescription()));
			}
			return areThereMinimalElements && validated;
		}

		private OPPLQuery getOPPLQuery() {
			OPPLQuery query = OPPLParser.getOPPLFactory().buildNewQuery(
					this.getConstraintSystem());
			for (OWLAxiom axiom : this.getAssertedQueryAxioms()) {
				query.addAssertedAxiom(axiom);
			}
			for (OWLAxiom axiom : this.getPlainQueryAxioms()) {
				query.addAxiom(axiom);
			}
			for (AbstractConstraint constraint : this.getConstraints()) {
				query.addConstraint(constraint);
			}
			return query;
		}

		/**
		 * @return the constraintSystem
		 */
		public final ConstraintSystem getConstraintSystem() {
			return this.constraintSystem;
		}

		/**
		 * @return the variables
		 */
		public List<Variable> getVariables() {
			return new ArrayList<Variable>(this.variables);
		}

		public void notifyBuilder() {
			OPPLBuilder.this.handleChange();
		}

		private void purgeQuery(Variable v) {
			this.purgeAssertedAxioms(v);
			this.purgePlainAxioms(v);
			this.purgeConstraints(v);
		}

		private void purgeConstraints(final Variable v) {
			for (AbstractConstraint constraint : this.getConstraints()) {
				boolean affected = constraint
						.accept(new ConstraintVisitorEx<Boolean>() {
							public Boolean visit(
									InCollectionConstraint<? extends OWLObject> c) {
								boolean toReturn = c.getVariable().equals(v);
								if (!toReturn) {
									Collection<? extends OWLObject> collection = c
											.getCollection();
									Iterator<? extends OWLObject> it = collection
											.iterator();
									NamedVariableDetector variableDetector = new NamedVariableDetector(
											v, OPPLBuilderModel.this
													.getConstraintSystem());
									boolean detected = false;
									while (!detected && it.hasNext()) {
										OWLObject object = it.next();
										detected = object
												.accept(variableDetector);
									}
									toReturn = detected;
								}
								return toReturn;
							}

							public Boolean visit(InequalityConstraint c) {
								return c.getVariable().equals(v)
										|| c
												.getExpression()
												.accept(
														new NamedVariableDetector(
																v,
																OPPLBuilderModel.this
																		.getConstraintSystem()));
							}
						});
				if (affected) {
					this.constraints.remove(constraint);
				}
			}
		}

		private void purgePlainAxioms(Variable v) {
			Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
			for (OWLAxiom axiom : this.plainQueryAxioms) {
				Set<Variable> axiomVariables = this.getConstraintSystem()
						.getAxiomVariables(axiom);
				if (axiomVariables.contains(v)) {
					toRemove.add(axiom);
				}
			}
			this.plainQueryAxioms.removeAll(toRemove);
		}

		private void purgeAssertedAxioms(Variable v) {
			Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
			for (OWLAxiom axiom : this.assertedQueryAxioms) {
				Set<Variable> axiomVariables = this.getConstraintSystem()
						.getAxiomVariables(axiom);
				if (axiomVariables.contains(v)) {
					toRemove.add(axiom);
				}
			}
			this.assertedQueryAxioms.removeAll(toRemove);
		}

		/**
		 * @param v
		 */
		private void purgeActions(Variable v) {
			Set<OWLAxiomChange> toRemove = new HashSet<OWLAxiomChange>();
			for (OWLAxiomChange action : this.actions) {
				OWLAxiom axiom = action.getAxiom();
				Set<Variable> axiomVariables = this.getConstraintSystem()
						.getAxiomVariables(axiom);
				if (axiomVariables.contains(v)) {
					toRemove.add(action);
				}
			}
			this.actions.removeAll(toRemove);
		}

		public void removeAction(OWLAxiomChange action) {
			boolean modified = this.actions.remove(action);
			if (modified) {
				this.notifyBuilder();
			}
		}

		public void removeVariable(Variable v) {
			boolean modified = this.variables.remove(v);
			if (modified) {
				this.purgeQuery(v);
				this.purgeActions(v);
				this.constraintSystem.removeVariable(v);
				this.notifyBuilder();
			}
		}

		public void replaceVariable(Variable oldVariable, Variable newVariable) {
			boolean modified = this.variables.remove(oldVariable);
			if (modified) {
				if (oldVariable.getType() != newVariable.getType()) {
					this.purgeActions(oldVariable);
					this.purgeQuery(oldVariable);
				}
				this.variables.add(newVariable);
				this.constraintSystem.removeVariable(oldVariable);
				this.constraintSystem.importVariable(newVariable);
				this.notifyBuilder();
			}
		}

		public void reset() {
			this.variables.clear();
			this.assertedQueryAxioms.clear();
			this.plainQueryAxioms.clear();
			this.constraints.clear();
			this.actions.clear();
			this.notifyBuilder();
		}

		/**
		 * @return the assertedQueryAxioms
		 */
		public final List<OWLAxiom> getAssertedQueryAxioms() {
			return new ArrayList<OWLAxiom>(this.assertedQueryAxioms);
		}

		public void addPlainQueryAxiom(OWLAxiom axiom) {
			boolean modified = this.plainQueryAxioms.add(axiom);
			if (modified) {
				this.notifyBuilder();
			}
		}

		/**
		 * @return the plainQueryAxioms
		 */
		public final List<OWLAxiom> getPlainQueryAxioms() {
			return new ArrayList<OWLAxiom>(this.plainQueryAxioms);
		}

		public void addAddAssertedQueryAxiom(OWLAxiom axiom) {
			boolean modified = this.assertedQueryAxioms.add(axiom);
			if (modified) {
				this.notifyBuilder();
			}
		}

		/**
		 * @return the constraints
		 */
		public final List<AbstractConstraint> getConstraints() {
			return new ArrayList<AbstractConstraint>(this.constraints);
		}

		public void addConstraint(AbstractConstraint constraint) {
			boolean modified = this.constraints.add(constraint);
			if (modified) {
				this.notifyBuilder();
			}
		}

		/**
		 * @return the actions
		 */
		public final List<OWLAxiomChange> getActions() {
			return this.actions;
		}

		public void importOPPLScript(OPPLScript opplScript) {
			this.reset();
			this.variables.addAll(opplScript.getVariables());
			this.constraintSystem.clearVariables();
			Set<Variable> variablesToImport = opplScript.getConstraintSystem()
					.getVariables();
			for (Variable variable : variablesToImport) {
				this.constraintSystem.importVariable(variable);
			}
			this.plainQueryAxioms.addAll(opplScript.getQuery().getAxioms());
			this.assertedQueryAxioms.addAll(opplScript.getQuery()
					.getAssertedAxioms());
			this.constraints.addAll(opplScript.getQuery().getConstraints());
			this.actions.addAll(opplScript.getActions());
			this.notifyBuilder();
		}

		public void removeAssertedAxiom(OWLAxiom axiom) {
			boolean modified = this.assertedQueryAxioms.remove(axiom);
			if (modified) {
				this.notifyBuilder();
			}
		}

		public void removePlainAxiom(OWLAxiom axiom) {
			boolean modified = this.plainQueryAxioms.remove(axiom);
			if (modified) {
				this.notifyBuilder();
			}
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -6106150715610094308L;

	private class OPPLActionList extends ActionList {
		public OPPLActionList() {
			super(OPPLBuilder.this.owlEditorKit,
					OPPLBuilder.this.opplBuilderModel.getConstraintSystem(),
					true);
		}

		@Override
		protected void handleAdd() {
			final OWLAxiomChangeEditor actionEditor = new OWLAxiomChangeEditor(
					OPPLBuilder.this.owlEditorKit,
					OPPLBuilder.this.opplBuilderModel.getConstraintSystem());
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
					OPPLBuilder.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(true);
			dlg.setTitle("Action editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(OPPLBuilder.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						OWLAxiomChange action = actionEditor
								.getOwlAxiomChange();
						DefaultListModel model = (DefaultListModel) OPPLBuilder.this.actionList
								.getModel();
						model.addElement(new OPPLActionListItem(action, true,
								true));
						OPPLBuilder.this.handleChange();
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
			Object selectedValue = this.getSelectedValue();
			if (OPPLActionListItem.class.isAssignableFrom(selectedValue
					.getClass())) {
				OPPLActionListItem item = (OPPLActionListItem) selectedValue;
				OWLAxiomChange action = item.getAxiomChange();
				OPPLBuilder.this.opplBuilderModel.removeAction(action);
			}
		}

		@Override
		public void setConstraintSystem(ConstraintSystem constraintSystem) {
			this.setCellRenderer(new VariableOWLCellRenderer(
					OPPLBuilder.this.owlEditorKit,
					OPPLBuilder.this.opplBuilderModel.getConstraintSystem(),
					new OWLCellRenderer(OPPLBuilder.this.owlEditorKit)));
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -3297222035586803090L;
	}

	private class OPPLActionListItem extends ActionListItem {
		public OPPLActionListItem(OWLAxiomChange axiomChange,
				boolean isEditable, boolean isDeleteable) {
			super(axiomChange, isEditable, isDeleteable);
		}

		@Override
		public void handleEdit() {
			final OWLAxiomChangeEditor actionEditor = new OWLAxiomChangeEditor(
					OPPLBuilder.this.owlEditorKit,
					OPPLBuilder.this.opplBuilderModel.getConstraintSystem());
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
					OPPLBuilder.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(true);
			dlg.setTitle("Action editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(OPPLBuilder.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						OWLAxiomChange action = actionEditor
								.getOwlAxiomChange();
						OPPLBuilder.this.opplBuilderModel
								.removeAction(OPPLActionListItem.this
										.getAxiomChange());
						OPPLBuilder.this.opplBuilderModel.addAction(action);
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
			ConstraintSystem cs = OPPLBuilder.this.opplBuilderModel
					.getConstraintSystem();
			final AbstractVariableEditor variableEditor = this.getVariable() instanceof GeneratedVariable ? new GeneratedVariableEditor(
					OPPLBuilder.this.owlEditorKit, cs)
					: new VariableEditor(OPPLBuilder.this.owlEditorKit, cs);
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
					OPPLBuilder.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(true);
			dlg.setTitle("Action editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(OPPLBuilder.this.owlEditorKit
					.getWorkspace());
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						Variable newVariable = variableEditor.getVariable();
						Variable oldVariable = OPPLVariableListItem.this
								.getVariable();
						OPPLBuilder.this.opplBuilderModel.replaceVariable(
								oldVariable, newVariable);
					}
					variableEditor
							.removeStatusChangedListener(verificationListener);
					variableEditor.dispose();
					OPPLBuilder.this.handleChange();
				}
			});
			dlg.setVisible(true);
		}
	}

	private class OPPLVariableList extends VariableList {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2540053052502672472L;

		@Override
		protected void handleDelete() {
			Object selectedValue = this.getSelectedValue();
			if (OPPLVariableListItem.class.isAssignableFrom(selectedValue
					.getClass())) {
				OPPLVariableListItem item = (OPPLVariableListItem) selectedValue;
				OPPLBuilder.this.opplBuilderModel.removeVariable(item
						.getVariable());
			}
		}

		@Override
		protected void handleAdd() {
			final AbstractVariableEditor variableEditor = this
					.getSelectedValue() instanceof InputVariableSectionHeader ? new VariableEditor(
					OPPLBuilder.this.owlEditorKit,
					OPPLBuilder.this.opplBuilderModel.getConstraintSystem())
					: new GeneratedVariableEditor(
							OPPLBuilder.this.owlEditorKit,
							OPPLBuilder.this.opplBuilderModel
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
					OPPLBuilder.this.owlEditorKit.getWorkspace(), null);
			// The editor shouldn't be modal (or should it?)
			dlg.setModal(true);
			dlg.setTitle("Variable editor");
			dlg.setResizable(true);
			dlg.pack();
			dlg.setLocationRelativeTo(OPPLBuilder.this);
			dlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					Object retVal = optionPane.getValue();
					if (retVal != null && retVal.equals(JOptionPane.OK_OPTION)) {
						Variable variable = variableEditor.getVariable();
						OPPLBuilder.this.opplBuilderModel.addVariable(variable);
					}
					variableEditor
							.removeStatusChangedListener(verificationListener);
					variableEditor.dispose();
				}
			});
			dlg.setVisible(true);
		}

		public OPPLVariableList(OWLEditorKit owlEditorKit) {
			super(owlEditorKit, OPPLBuilder.this.opplBuilderModel
					.getConstraintSystem());
			((DefaultListModel) this.getModel())
					.addElement(new InputVariableSectionHeader());
			((DefaultListModel) this.getModel())
					.addElement(new GeneratedVariableSectionHeader());
		}

		public void clear() {
			((DefaultListModel) this.getModel()).clear();
			((DefaultListModel) this.getModel())
					.addElement(new InputVariableSectionHeader());
			((DefaultListModel) this.getModel())
					.addElement(new GeneratedVariableSectionHeader());
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
	// private transient ListDataListener selectListListener = new
	// ListDataListener() {
	// public void contentsChanged(ListDataEvent e) {
	// OPPLBuilder.this.handleChange();
	// }
	//
	// public void intervalAdded(ListDataEvent e) {
	// OPPLBuilder.this.handleChange();
	// }
	//
	// public void intervalRemoved(ListDataEvent e) {
	// OPPLBuilder.this.handleChange();
	// }
	// };
	// private transient ListDataListener constraintListListener = new
	// ListDataListener() {
	// public void contentsChanged(ListDataEvent e) {
	// OPPLBuilder.this.handleChange();
	// }
	//
	// public void intervalAdded(ListDataEvent e) {
	// OPPLBuilder.this.handleChange();
	// }
	//
	// public void intervalRemoved(ListDataEvent e) {
	// OPPLBuilder.this.handleChange();
	// }
	// };
	private OPPLConstraintList constraintList;
	private ActionList actionList;
	// private transient ListDataListener actionListListener = new
	// ListDataListener() {
	// public void contentsChanged(ListDataEvent e) {
	// OPPLBuilder.this.handleChange();
	// }
	//
	// public void intervalAdded(ListDataEvent e) {
	// OPPLBuilder.this.handleChange();
	// }
	//
	// public void intervalRemoved(ListDataEvent e) {
	// OPPLBuilder.this.handleChange();
	// }
	// };
	// private ConstraintSystem constraintSystem = OPPLParser.getOPPLFactory()
	// .createConstraintSystem();
	private OPPLScript opplScript;
	private final OPPLScriptValidator validator;
	private DefaultListModel errorListModel = new DefaultListModel();
	private JList errorList = new JList(this.errorListModel);
	private final JPanel errorPanel = new JPanel(new BorderLayout());
	private final OPPLBuilderModel opplBuilderModel = new OPPLBuilderModel();

	public OPPLBuilder(OWLEditorKit owlEditorKit) {
		this(owlEditorKit, null);
	}

	public OPPLBuilder(OWLEditorKit owlEditorKit, OPPLScriptValidator validator) {
		this.setOrientation(JSplitPane.VERTICAL_SPLIT);
		JSplitPane builderPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		builderPane.setDividerLocation(.5);
		this.owlEditorKit = owlEditorKit;
		this.validator = validator;
		// Setup the variable list on the left
		JPanel variablePanel = new JPanel(new BorderLayout());
		this.variableList = new OPPLVariableList(this.owlEditorKit);
		variablePanel.add(this.variableList);
		builderPane.add(ComponentFactory.createScrollPane(this.variableList),
				JSplitPane.LEFT);
		// Now setup the right hand side panel which will be further split into
		// queries and actions
		final JSplitPane queryActionSplitPane = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT);
		// Now setup the query split pane
		final JSplitPane queryConstraintSplitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT);
		// Now the select part
		JPanel queryPanel = new JPanel(new BorderLayout());
		this.selectList = new OPPLSelectClauseList(this.owlEditorKit,
				this.opplBuilderModel.getConstraintSystem()) {
			@Override
			protected void handleEdit() {
				if (this.getSelectedValue() instanceof OPPLSelectClauseListItem) {
					final OPPLSelectClauseListItem item = (OPPLSelectClauseListItem) this
							.getSelectedValue();
					/**
					 * @see org.protege.editor.core.ui.list.MListItem#handleEdit()
					 */
					final OPPLSelectClauseEditor editor = new OPPLSelectClauseEditor(
							this.getOwlEditorKit(), this.getConstraintSystem());
					editor.setSelectListItem(item);
					final VerifyingOptionPane optionPane = new VerifyingOptionPane(
							editor) {
						/**
							 * 
							 */
						private static final long serialVersionUID = 7816306100172449202L;

						/**
							 * 
							 */
						@Override
						public void selectInitialValue() {
							// This is overridden so that the option pane dialog
							// default
							// button
							// doesn't get the focus.
						}
					};
					final InputVerificationStatusChangedListener verificationListener = new InputVerificationStatusChangedListener() {
						public void verifiedStatusChanged(boolean verified) {
							optionPane.setOKEnabled(verified);
						}
					};
					editor.addStatusChangedListener(verificationListener);
					final JDialog dlg = optionPane.createDialog(this
							.getOwlEditorKit().getWorkspace(), null);
					// The editor shouldn't be modal (or should it?)
					dlg.setModal(true);
					dlg.setTitle("Clause editor");
					dlg.setResizable(true);
					dlg.pack();
					dlg.setLocationRelativeTo(this.getOwlEditorKit()
							.getWorkspace());
					dlg.addComponentListener(new ComponentAdapter() {
						@Override
						public void componentHidden(ComponentEvent e) {
							Object retVal = optionPane.getValue();
							if (retVal != null
									&& retVal.equals(JOptionPane.OK_OPTION)) {
								OPPLSelectClauseListItem newItem = editor
										.getSelectListItem();
								if (item.isAsserted()) {
									OPPLBuilder.this.opplBuilderModel
											.removeAssertedAxiom(item
													.getAxiom());
								} else {
									OPPLBuilder.this.opplBuilderModel
											.removePlainAxiom(item.getAxiom());
								}
								if (newItem.isAsserted()) {
									OPPLBuilder.this.opplBuilderModel
											.addAddAssertedQueryAxiom(newItem
													.getAxiom());
								} else {
									OPPLBuilder.this.opplBuilderModel
											.addPlainQueryAxiom(newItem
													.getAxiom());
								}
							}
							editor
									.removeStatusChangedListener(verificationListener);
							editor.dispose();
						}
					});
					dlg.setVisible(true);
				}
			}

			@Override
			protected void handleDelete() {
				if (this.getSelectedValue() instanceof OPPLSelectClauseListItem) {
					OPPLSelectClauseListItem item = (OPPLSelectClauseListItem) this
							.getSelectedValue();
					if (item.isAsserted()) {
						OPPLBuilder.this.opplBuilderModel
								.removeAssertedAxiom(item.getAxiom());
					} else {
						OPPLBuilder.this.opplBuilderModel.removePlainAxiom(item
								.getAxiom());
					}
				}
			}

			/**
					 * 
					 */
			private static final long serialVersionUID = -567785735962335293L;

			@Override
			protected void handleAdd() {
				final OPPLSelectClauseEditor editor = new OPPLSelectClauseEditor(
						this.getOwlEditorKit(), this.getConstraintSystem());
				final VerifyingOptionPane optionPane = new VerifyingOptionPane(
						editor) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 7816306100172449202L;

					/**
					 * 
					 */
					@Override
					public void selectInitialValue() {
						// This is overridden so that the option pane dialog
						// default
						// button
						// doesn't get the focus.
					}
				};
				final InputVerificationStatusChangedListener verificationListener = new InputVerificationStatusChangedListener() {
					public void verifiedStatusChanged(boolean verified) {
						optionPane.setOKEnabled(verified);
					}
				};
				editor.addStatusChangedListener(verificationListener);
				final JDialog dlg = optionPane.createDialog(this
						.getOwlEditorKit().getWorkspace(), null);
				// The editor shouldn't be modal (or should it?)
				dlg.setModal(true);
				dlg.setTitle("Clause editor");
				dlg.setResizable(true);
				dlg.pack();
				dlg.setLocationRelativeTo(OPPLBuilder.this);
				dlg.addComponentListener(new ComponentAdapter() {
					@Override
					public void componentHidden(ComponentEvent e) {
						Object retVal = optionPane.getValue();
						if (retVal != null
								&& retVal.equals(JOptionPane.OK_OPTION)) {
							OPPLSelectClauseListItem selectListItem = editor
									.getSelectListItem();
							if (selectListItem.isAsserted()) {
								OPPLBuilder.this.opplBuilderModel
										.addAddAssertedQueryAxiom(selectListItem
												.getAxiom());
							} else {
								OPPLBuilder.this.opplBuilderModel
										.addPlainQueryAxiom(selectListItem
												.getAxiom());
							}
						}
						editor
								.removeStatusChangedListener(verificationListener);
						editor.dispose();
					}
				});
				dlg.setVisible(true);
			}
		};
		// this.selectList.getModel().addListDataListener(this.selectListListener);
		queryPanel.add(ComponentFactory.createScrollPane(this.selectList));
		// Now the constraints
		JPanel constraintPanel = new JPanel(new BorderLayout());
		this.constraintList = new OPPLConstraintList(this.owlEditorKit,
				this.opplBuilderModel.getConstraintSystem());
		// this.constraintList.getModel().addListDataListener(
		// this.constraintListListener);
		constraintPanel.add(ComponentFactory
				.createScrollPane(this.constraintList));
		queryConstraintSplitPane.add(queryPanel, JSplitPane.LEFT);
		queryConstraintSplitPane.add(constraintPanel, JSplitPane.RIGHT);
		// Now setup the action panel
		JPanel actionPanel = new JPanel(new BorderLayout());
		this.actionList = new OPPLActionList();
		// this.actionList.getModel().addListDataListener(this.actionListListener);
		actionPanel.add(ComponentFactory.createScrollPane(this.actionList));
		queryActionSplitPane.add(queryConstraintSplitPane, JSplitPane.TOP);
		queryActionSplitPane.add(actionPanel, JSplitPane.BOTTOM);
		builderPane.add(queryActionSplitPane, JSplitPane.RIGHT);
		queryConstraintSplitPane.setDividerLocation(.5);
		queryConstraintSplitPane.setResizeWeight(.3);
		queryActionSplitPane.setDividerLocation(.5);
		queryActionSplitPane.setResizeWeight(.3);
		this.setDividerLocation(.5);
		this.setResizeWeight(.3);
		this.errorList.setCellRenderer(new MessageListCellRenderer());
		this.errorPanel.add(ComponentFactory.createScrollPane(this.errorList));
		this.errorPanel.setBorder(ComponentFactory
				.createTitledBorder("Errors:"));
		this.errorPanel.setPreferredSize(new Dimension(100, 500));
		this.add(this.errorPanel, JSplitPane.TOP);
		this.add(builderPane, JSplitPane.BOTTOM);
		builderPane.setDividerLocation(.5);
		builderPane.setResizeWeight(.3);
		this.setDividerLocation(.3);
		this.setResizeWeight(.3);
		this.opplBuilderModel.check();
	}

	public void handleChange() {
		this.opplScript = null;
		this.errorListModel.clear();
		boolean isValid = this.opplBuilderModel.check();
		if (isValid) {
			this.opplScript = OPPLParser.getOPPLFactory().buildOPPLScript(
					this.opplBuilderModel.getConstraintSystem(),
					this.opplBuilderModel.getVariables(),
					this.opplBuilderModel.getOPPLQuery(),
					this.opplBuilderModel.getActions());
		}
		this.errorPanel.setVisible(!this.errorListModel.isEmpty());
		if (this.errorPanel.isVisible()) {
			this.setDividerLocation(.3);
		}
		this.notifyListeners(isValid);
		this.updateGUI();
	}

	private void updateGUI() {
		List<Variable> variables = this.opplBuilderModel.getVariables();
		this.variableList.clear();
		for (Variable variable : variables) {
			this.variableList.placeListItem(new OPPLVariableListItem(variable,
					this.owlEditorKit, true, true));
		}
		this.selectList.clear();
		for (OWLAxiom axiom : this.opplBuilderModel.getAssertedQueryAxioms()) {
			((DefaultListModel) this.selectList.getModel())
					.addElement(new OPPLSelectClauseListItem(true, axiom));
		}
		for (OWLAxiom axiom : this.opplBuilderModel.getPlainQueryAxioms()) {
			((DefaultListModel) this.selectList.getModel())
					.addElement(new OPPLSelectClauseListItem(false, axiom));
		}
		this.constraintList.clear();
		List<AbstractConstraint> constraints = this.opplBuilderModel
				.getConstraints();
		for (AbstractConstraint constraint : constraints) {
			((DefaultListModel) this.constraintList.getModel())
					.addElement(new OPPLConstraintListItem(this.owlEditorKit,
							constraint, this.opplBuilderModel
									.getConstraintSystem()));
		}
		this.actionList.clear();
		List<OWLAxiomChange> actions = this.opplBuilderModel.getActions();
		for (OWLAxiomChange axiomChange : actions) {
			((DefaultListModel) this.actionList.getModel())
					.addElement(new OPPLActionListItem(axiomChange, true, true));
		}
	}

	/**
	 * 
	 */
	private void notifyListeners(boolean status) {
		for (InputVerificationStatusChangedListener listener : this.listeners) {
			listener.verifiedStatusChanged(status);
		}
	}

	public void addStatusChangedListener(
			InputVerificationStatusChangedListener listener) {
		this.listeners.add(listener);
		listener.verifiedStatusChanged(this.opplScript != null);
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
		this.opplBuilderModel.importOPPLScript(opplScript);
	}

	public void clear() {
		this.opplBuilderModel.reset();
	}

	@Override
	public String getName() {
		return "OPPL Builder";
	}
}