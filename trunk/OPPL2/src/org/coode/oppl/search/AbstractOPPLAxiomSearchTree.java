package org.coode.oppl.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.coode.oppl.log.Logging;
import org.coode.oppl.utils.VariableExtractor;
import org.coode.oppl.variablemansyntax.ConstraintSystem;
import org.coode.oppl.variablemansyntax.PartialOWLObjectInstantiator;
import org.coode.oppl.variablemansyntax.Variable;
import org.coode.oppl.variablemansyntax.VariableTypeVisitorEx;
import org.coode.oppl.variablemansyntax.bindingtree.Assignment;
import org.coode.oppl.variablemansyntax.bindingtree.BindingNode;
import org.coode.oppl.variablemansyntax.variabletypes.CLASSVariable;
import org.coode.oppl.variablemansyntax.variabletypes.CONSTANTVariable;
import org.coode.oppl.variablemansyntax.variabletypes.DATAPROPERTYVariable;
import org.coode.oppl.variablemansyntax.variabletypes.INDIVIDUALVariable;
import org.coode.oppl.variablemansyntax.variabletypes.OBJECTPROPERTYVariable;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLClassAssertionAxiom;
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLDataValueRestriction;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLDescriptionVisitor;
import org.semanticweb.owl.model.OWLDisjointClassesAxiom;
import org.semanticweb.owl.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObject;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLSubClassAxiom;
import org.semanticweb.owl.util.OWLAxiomVisitorAdapter;
import org.semanticweb.owl.util.OWLDescriptionVisitorAdapter;

public abstract class AbstractOPPLAxiomSearchTree extends
		SearchTree<OPPLOWLAxiomSearchNode> {
	private final class ConstantCollector extends OWLAxiomVisitorAdapter {
		private final Set<OWLConstant> toReturn;
		private final OWLDescriptionVisitor constantExtractor;

		ConstantCollector(Set<OWLConstant> toReturn,
				OWLDescriptionVisitor constantExtractor) {
			this.toReturn = toReturn;
			this.constantExtractor = constantExtractor;
		}

		@Override
		public void visit(OWLClassAssertionAxiom axiom) {
			axiom.getDescription().accept(this.constantExtractor);
		}

		@Override
		public void visit(OWLDataPropertyAssertionAxiom axiom) {
			this.toReturn.add(axiom.getObject());
		}

		@Override
		public void visit(OWLDisjointClassesAxiom axiom) {
			for (OWLDescription description : axiom.getDescriptions()) {
				description.accept(this.constantExtractor);
			}
		}

		@Override
		public void visit(OWLEquivalentClassesAxiom axiom) {
			for (OWLDescription description : axiom.getDescriptions()) {
				description.accept(this.constantExtractor);
			}
		}

		@Override
		public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
			this.toReturn.add(axiom.getObject());
		}

		@Override
		public void visit(OWLSubClassAxiom axiom) {
			axiom.getSubClass().accept(this.constantExtractor);
			axiom.getSuperClass().accept(this.constantExtractor);
		}
	}

	private final ConstraintSystem constraintSystem;
	private final Set<OWLClass> allClasses = new HashSet<OWLClass>();
	private final Set<OWLObjectProperty> allObjectProperties = new HashSet<OWLObjectProperty>();
	private final Set<OWLDataProperty> allDataProperties = new HashSet<OWLDataProperty>();
	private final Set<OWLIndividual> allIndividuals = new HashSet<OWLIndividual>();
	private final Set<OWLConstant> allConstants = new HashSet<OWLConstant>();

	protected AbstractOPPLAxiomSearchTree(ConstraintSystem constraintSystem) {
		if (constraintSystem == null) {
			throw new NullPointerException(
					"The constraint system cannot be null");
		}
		this.constraintSystem = constraintSystem;
	}

	@Override
	protected abstract boolean goalReached(OPPLOWLAxiomSearchNode start);

	/**
	 * @see org.coode.oppl.search.SearchTree#getChildren(java.lang.Object)
	 */
	@Override
	protected List<OPPLOWLAxiomSearchNode> getChildren(
			OPPLOWLAxiomSearchNode node) {
		Set<BindingNode> leaves = this.getConstraintSystem().getLeaves();
		List<OPPLOWLAxiomSearchNode> toReturn = new ArrayList<OPPLOWLAxiomSearchNode>();
		VariableExtractor variableExtractor = new VariableExtractor(this
				.getConstraintSystem());
		Set<Variable> variables = node.getAxiom().accept(variableExtractor);
		BindingNode binding = node.getBinding();
		for (Variable variable : variables) {
			Collection<OWLObject> values = new HashSet<OWLObject>();
			if (leaves == null) {
				values.addAll(this.getAssignableValues(variable));
			} else {
				for (BindingNode bindingNode : leaves) {
					if (bindingNode.getAssignedVariables().contains(variable)) {
						values.add(bindingNode.getAssignmentValue(variable));
					} else {
						values.addAll(this.getAssignableValues(variable));
					}
				}
			}
			for (OWLObject value : values) {
				Assignment assignment = new Assignment(variable, value);
				BindingNode childBinding = new BindingNode(binding
						.getAssignments(), binding.getUnassignedVariables());
				childBinding.addAssignment(assignment);
				PartialOWLObjectInstantiator instantiator = new PartialOWLObjectInstantiator(
						childBinding, this.getConstraintSystem());
				OWLAxiom instantiatedAxiom = (OWLAxiom) node.getAxiom().accept(
						instantiator);
				OPPLOWLAxiomSearchNode child = new OPPLOWLAxiomSearchNode(
						instantiatedAxiom, childBinding);
				toReturn.add(child);
			}
		}
		return toReturn;
	}

	private Collection<OWLClass> getAllClasses() {
		Set<OWLClass> toReturn = new HashSet<OWLClass>();
		for (OWLOntology owlOntology : this.getConstraintSystem()
				.getOntologyManager().getOntologies()) {
			toReturn.addAll(owlOntology.getReferencedClasses());
		}
		return toReturn;
	}

	private Collection<? extends OWLConstant> getAllConstants() {
		final Set<OWLConstant> toReturn = new HashSet<OWLConstant>();
		final OWLDescriptionVisitor constantExtractor = new OWLDescriptionVisitorAdapter() {
			@Override
			public void visit(OWLDataValueRestriction desc) {
				toReturn.add(desc.getValue());
			}
		};
		ConstantCollector visitor = new ConstantCollector(toReturn,
				constantExtractor);
		for (OWLOntology owlOntology : this.getConstraintSystem()
				.getOntologyManager().getOntologies()) {
			for (OWLAxiom axiomToVisit : owlOntology.getAxioms()) {
				axiomToVisit.accept(visitor);
			}
		}
		return toReturn;
	}

	private Collection<OWLDataProperty> getAllDataProperties() {
		Set<OWLDataProperty> toReturn = new HashSet<OWLDataProperty>();
		for (OWLOntology owlOntology : this.getConstraintSystem()
				.getOntologyManager().getOntologies()) {
			toReturn.addAll(owlOntology.getReferencedDataProperties());
		}
		return toReturn;
	}

	private Collection<OWLIndividual> getAllIndividuals() {
		Set<OWLIndividual> toReturn = new HashSet<OWLIndividual>();
		for (OWLOntology owlOntology : this.getConstraintSystem()
				.getOntologyManager().getOntologies()) {
			toReturn.addAll(owlOntology.getReferencedIndividuals());
		}
		return toReturn;
	}

	private final VariableTypeVisitorEx<Set<? extends OWLObject>> assignableValuesVisitor = new VariableTypeVisitorEx<Set<? extends OWLObject>>() {
		public Set<? extends OWLObject> visit(Variable v) {
			// TODO Auto-generated method stub
			return Collections.emptySet();
		}

		public Set<? extends OWLObject> visit(INDIVIDUALVariable v) {
			return AbstractOPPLAxiomSearchTree.this.allIndividuals;
		}

		public Set<? extends OWLObject> visit(DATAPROPERTYVariable v) {
			return AbstractOPPLAxiomSearchTree.this.allDataProperties;
		}

		public Set<? extends OWLObject> visit(OBJECTPROPERTYVariable v) {
			return AbstractOPPLAxiomSearchTree.this.allObjectProperties;
		}

		public Set<? extends OWLObject> visit(CONSTANTVariable v) {
			return AbstractOPPLAxiomSearchTree.this.allConstants;
		}

		public Set<? extends OWLObject> visit(CLASSVariable v) {
			return AbstractOPPLAxiomSearchTree.this.allClasses;
		}
	};

	private Collection<? extends OWLObject> getAssignableValues(
			Variable variable) {
		Set<OWLObject> toReturn = new HashSet<OWLObject>();
		toReturn.addAll(variable.accept(this.assignableValuesVisitor));
		// VariableType type = variable.getType();
		// switch (type) {
		// case CLASS:
		// toReturn.addAll(this.allClasses);
		// break;
		// case DATAPROPERTY:
		// toReturn.addAll(this.allDataProperties);
		// break;
		// case OBJECTPROPERTY:
		// toReturn.addAll(this.allObjectProperties);
		// break;
		// case INDIVIDUAL:
		// toReturn.addAll(this.allIndividuals);
		// break;
		// case CONSTANT:
		// toReturn.addAll(this.allConstants);
		// break;
		// default:
		// break;
		// }
		return toReturn;
	}

	private void initAssignableValues() {
		this.allClasses.addAll(this.getAllClasses());
		Logging.getQueryLogger().log(Level.FINE,
				"Possible class values " + this.allClasses.size());
		this.allDataProperties.addAll(this.getAllDataProperties());
		Logging.getQueryLogger().log(
				Level.FINE,
				"Possible data property values "
						+ this.allDataProperties.size());
		this.allObjectProperties.addAll(this.getObjectProperties());
		Logging.getQueryLogger().log(
				Level.FINE,
				"Possible object property values "
						+ this.allObjectProperties.size());
		this.allIndividuals.addAll(this.getAllIndividuals());
		Logging.getQueryLogger().log(Level.FINE,
				"Possible individual  values " + this.allIndividuals.size());
		this.allConstants.addAll(this.getAllConstants());
		Logging.getQueryLogger().log(Level.FINE,
				"Possible constant  values " + this.allConstants.size());
	}

	/**
	 * @return the constraintSystem
	 */
	public ConstraintSystem getConstraintSystem() {
		return this.constraintSystem;
	}

	private Collection<OWLObjectProperty> getObjectProperties() {
		Set<OWLObjectProperty> toReturn = new HashSet<OWLObjectProperty>();
		for (OWLOntology owlOntology : this.getConstraintSystem()
				.getOntologyManager().getOntologies()) {
			toReturn.addAll(owlOntology.getReferencedObjectProperties());
		}
		return toReturn;
	}

	/**
	 * @see org.coode.oppl.search.SearchTree#exhaustiveSearchTree(java.lang.Object,
	 *      java.util.List)
	 */
	@Override
	public boolean exhaustiveSearchTree(OPPLOWLAxiomSearchNode start,
			List<List<OPPLOWLAxiomSearchNode>> solutions) {
		this.initAssignableValues();
		Set<BindingNode> existingLeaves = this.getConstraintSystem()
				.getLeaves();
		boolean found = false;
		if (existingLeaves != null) {
			Logging.getQueryTestLogging().log(Level.INFO,
					"Existing leaves count: " + existingLeaves.size());
			int leafIndex = 1;
			for (BindingNode bindingNode : existingLeaves) {
				Logging.getQueryTestLogging().log(
						Level.FINER,
						"Exhaustive search on leaf: " + leafIndex++
								+ " out of " + existingLeaves.size());
				PartialOWLObjectInstantiator partialObjectInstantiator = new PartialOWLObjectInstantiator(
						bindingNode, this.getConstraintSystem());
				OWLAxiom newStartAxiom = (OWLAxiom) start.getAxiom().accept(
						partialObjectInstantiator);
				OPPLOWLAxiomSearchNode newStart = new OPPLOWLAxiomSearchNode(
						newStartAxiom, bindingNode);
				List<List<OPPLOWLAxiomSearchNode>> bindingNodeSolutions = new ArrayList<List<OPPLOWLAxiomSearchNode>>();
				boolean bindingNodeSearch = super.exhaustiveSearchTree(
						newStart, bindingNodeSolutions);
				found = found || bindingNodeSearch;
				if (bindingNodeSearch) {
					solutions.addAll(bindingNodeSolutions);
				}
			}
		} else {
			found = super.exhaustiveSearchTree(start, solutions);
		}
		Set<BindingNode> newLeaves = new HashSet<BindingNode>();
		for (List<OPPLOWLAxiomSearchNode> path : solutions) {
			OPPLOWLAxiomSearchNode leafSerachNode = path.get(path.size() - 1);
			BindingNode newLeaf = leafSerachNode.getBinding();
			newLeaves.add(newLeaf);
		}
		this.constraintSystem.setLeaves(newLeaves);
		return found;
	}
}