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
package uk.ac.manchester.mae.visitor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLTypedConstant;
import org.semanticweb.owl.vocab.XSDVocabulary;

import uk.ac.manchester.mae.ArithmeticsParserVisitor;
import uk.ac.manchester.mae.ConflictStrategy;
import uk.ac.manchester.mae.ConflictStrategyFactory;
import uk.ac.manchester.mae.Constants;
import uk.ac.manchester.mae.EvaluationException;
import uk.ac.manchester.mae.MAEAdd;
import uk.ac.manchester.mae.MAEBigSum;
import uk.ac.manchester.mae.MAEBinding;
import uk.ac.manchester.mae.MAEConflictStrategy;
import uk.ac.manchester.mae.MAEIdentifier;
import uk.ac.manchester.mae.MAEIntNode;
import uk.ac.manchester.mae.MAEMult;
import uk.ac.manchester.mae.MAEPower;
import uk.ac.manchester.mae.MAEPropertyChain;
import uk.ac.manchester.mae.MAEPropertyFacet;
import uk.ac.manchester.mae.MAEStart;
import uk.ac.manchester.mae.MAEStoreTo;
import uk.ac.manchester.mae.MAEmanSyntaxClassExpression;
import uk.ac.manchester.mae.MoreThanOneValueForFunctionalPropertyException;
import uk.ac.manchester.mae.SimpleNode;
import uk.ac.manchester.mae.UnsupportedDataTypeException;
import uk.ac.manchester.mae.report.EvaluationReport;
import uk.ac.manchester.mae.report.ExceptionReportWriter;
import uk.ac.manchester.mae.report.FormulaReportWriter;

/**
 * @author Luigi Iannone
 * 
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Apr 22, 2008
 */
public class Writer implements ArithmeticsParserVisitor {
	protected ConflictStrategy conflictStrategy = null;
	protected OWLOntologyManager ontologyManager;
	protected EvaluationReport evaluationReport;
	protected OWLOntology startingOntology;
	protected Set<OWLOntology> ontologies;
	protected OWLIndividual currentIndividual;
	protected OWLDataProperty dataProperty;
	protected Object results;
	protected MAEStart startingFormula;
	protected FacetExtractor facetExtractor;
	protected OWLReasoner reasoner;

	/**
	 * @param currentIndividual
	 * @param dataProperty
	 * @param results
	 * @param startingOntology
	 * @param ontologyManager
	 */
	public Writer(OWLIndividual currentIndividual,
			OWLDataProperty dataProperty, Object results,
			OWLOntology startingOntology, OWLReasoner reasoner,
			OWLOntologyManager ontologyManager,
			EvaluationReport evaluationReport, FacetExtractor facetExtractor) {
		this.evaluationReport = evaluationReport == null ? new EvaluationReport()
				: evaluationReport;
		this.currentIndividual = currentIndividual;
		this.dataProperty = dataProperty;
		this.results = results;
		this.startingOntology = startingOntology;
		this.reasoner = reasoner;
		this.ontologyManager = ontologyManager;
		this.ontologies = ontologyManager.getImportsClosure(startingOntology);
		this.facetExtractor = facetExtractor;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.SimpleNode,
	 *      java.lang.Object)
	 */
	public Object visit(SimpleNode node, Object data) {
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEStart,
	 *      java.lang.Object)
	 */
	public Object visit(MAEStart node, Object data) {
		this.startingFormula = node;
		ConflictStrategyExtractor conflictStrategyExtractor = new ConflictStrategyExtractor();
		node.jjtAccept(conflictStrategyExtractor, data);
		this.conflictStrategy = conflictStrategyExtractor
				.getExtractedConflictStrategy();
		MAEPropertyChain storageChainModel = null;
		StorageExtractor storageExtractor = new StorageExtractor();
		node.jjtAccept(storageExtractor, data);
		storageChainModel = storageExtractor.getExtractedStorage();
		if (storageChainModel != null) {
			storageChainModel.jjtAccept(this, data);
		} else {
			try {
				this.write(this.currentIndividual, this.dataProperty,
						this.results);
			} catch (OWLOntologyChangeException e) {
				FormulaReportWriter resultReportWriter = new ExceptionReportWriter(
						this.dataProperty, node, e);
				this.evaluationReport.accept(resultReportWriter, null);
			} catch (UnsupportedDataTypeException e) {
				FormulaReportWriter resultReportWriter = new ExceptionReportWriter(
						this.dataProperty, node, e);
				this.evaluationReport.accept(resultReportWriter, null);
			} catch (EvaluationException e) {
				FormulaReportWriter resultReportWriter = new ExceptionReportWriter(
						this.dataProperty, node, e);
				this.evaluationReport.accept(resultReportWriter, null);
			}
		}
		return storageChainModel;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEConflictStrategy,
	 *      java.lang.Object)
	 */
	public Object visit(MAEConflictStrategy node, Object data) {
		String strategyName = node.getStrategyName();
		ConflictStrategyFactory.getStrategy(strategyName);
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEStoreTo,
	 *      java.lang.Object)
	 */
	public Object visit(MAEStoreTo node, Object data) {
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEmanSyntaxClassExpression,
	 *      java.lang.Object)
	 */
	public Object visit(MAEmanSyntaxClassExpression node, Object data) {
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEBinding,
	 *      java.lang.Object)
	 */
	public Object visit(MAEBinding node, Object data) {
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEPropertyChain,
	 *      java.lang.Object)
	 */
	public Object visit(MAEPropertyChain node, Object data) {
		String propertyName = node.getPropertyName();
		try {
			node.jjtAccept(this.facetExtractor, data);
			OWLDescription facetDescription = this.facetExtractor
					.getExtractedDescription() == null ? this.ontologyManager
					.getOWLDataFactory().getOWLThing() : this.facetExtractor
					.getExtractedDescription();
			this.walkProperty(propertyName, facetDescription);
			if (node.isEnd()) {
				this.write(this.currentIndividual, this.dataProperty,
						this.results);
			} else {
				node.childrenAccept(this, data);
			}
		} catch (Exception e) {
			ExceptionReportWriter erw = new ExceptionReportWriter(
					this.dataProperty, this.startingFormula, e);
			this.evaluationReport.accept(erw, null);
		}
		return null;
	}

	/**
	 * @param propertyName
	 * @param facetDescription
	 * @throws URISyntaxException
	 * @throws UnsupportedDataTypeException
	 * @throws OWLOntologyChangeException
	 * @throws OWLReasonerException
	 */
	private void walkProperty(String propertyName,
			OWLDescription facetDescription) throws URISyntaxException,
			UnsupportedDataTypeException, OWLOntologyChangeException,
			OWLReasonerException {
		Collection<Object> fillers = this.fetch(this.currentIndividual,
				propertyName, false, facetDescription);
		if (fillers != null && !fillers.isEmpty()) {
			this.currentIndividual = (OWLIndividual) fillers.iterator().next();
		} else {
			OWLIndividual newFiller = this.ontologyManager.getOWLDataFactory()
					.getOWLIndividual(
							URI.create(Constants.FORMULA_NAMESPACE_URI_STRING
									+ System.nanoTime()));
			AddAxiom addAxiom = new AddAxiom(this.startingOntology,
					this.ontologyManager.getOWLDataFactory()
							.getOWLObjectPropertyAssertionAxiom(
									this.currentIndividual,
									this.ontologyManager.getOWLDataFactory()
											.getOWLObjectProperty(
													URI.create(propertyName)),
									newFiller));
			this.ontologyManager.applyChange(addAxiom);
			addAxiom = new AddAxiom(this.startingOntology, this.ontologyManager
					.getOWLDataFactory().getOWLClassAssertionAxiom(newFiller,
							facetDescription));
			this.ontologyManager.applyChange(addAxiom);
			this.currentIndividual = newFiller;
		}
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEAdd,
	 *      java.lang.Object)
	 */
	public Object visit(MAEAdd node, Object data) {
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEMult,
	 *      java.lang.Object)
	 */
	public Object visit(MAEMult node, Object data) {
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEPower,
	 *      java.lang.Object)
	 */
	public Object visit(MAEPower node, Object data) {
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEIntNode,
	 *      java.lang.Object)
	 */
	public Object visit(MAEIntNode node, Object data) {
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEIdentifier,
	 *      java.lang.Object)
	 */
	public Object visit(MAEIdentifier node, Object data) {
		return null;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEBigSum,
	 *      java.lang.Object)
	 */
	public Object visit(MAEBigSum node, Object data) {
		return null;
	}

	@SuppressWarnings("unchecked")
	private void write(OWLIndividual individual, OWLDataProperty dataProperty,
			Object object) throws EvaluationException,
			OWLOntologyChangeException, UnsupportedDataTypeException {
		Set<OWLConstant> oldValues = individual.getDataPropertyValues(
				this.startingOntology).get(dataProperty);
		if (!dataProperty.isFunctional(this.ontologies) || oldValues == null
				|| oldValues.isEmpty()) {
			if (object instanceof Collection) {
				for (Object newValue : (Collection<Object>) object) {
					this.writeSingleValue(individual, dataProperty, newValue);
				}
			} else {
				this.writeSingleValue(individual, dataProperty, object);
			}
		} else if (dataProperty.isFunctional(this.ontologies)) {
			if (object instanceof Collection
					&& ((Collection<Object>) object).size() > 1) {
				throw new MoreThanOneValueForFunctionalPropertyException(
						"More than one value for the functional property "
								+ dataProperty + " for the individual "
								+ individual.getURI().toString());
			}
			if (oldValues != null && !oldValues.isEmpty()) {
				OWLDataPropertyAssertionAxiom oldAssertion = this.ontologyManager
						.getOWLDataFactory().getOWLDataPropertyAssertionAxiom(
								individual, dataProperty,
								oldValues.iterator().next());
				if (this.conflictStrategy != null) {
					this.conflictStrategy
							.solve(
									individual,
									oldAssertion,
									this
											.convert2OWLConstant(object instanceof Collection ? ((Collection) object)
													.iterator().next()
													: object), this.ontologies,
									this.ontologyManager);
				} else {
					this
							.writeSingleValue(
									individual,
									dataProperty,
									object instanceof Collection ? ((Collection) object)
											.iterator().next()
											: object);
				}
			}
		}
	}

	private void writeSingleValue(OWLIndividual individual,
			OWLDataProperty dataProperty, Object newValue)
			throws UnsupportedDataTypeException, OWLOntologyChangeException {
		OWLConstant valueAsOWLConstant = this.convert2OWLConstant(newValue);
		AddAxiom addAxiom = new AddAxiom(this.startingOntology,
				this.ontologyManager.getOWLDataFactory()
						.getOWLDataPropertyAssertionAxiom(individual,
								dataProperty, valueAsOWLConstant));
		this.ontologyManager.applyChange(addAxiom);
	}

	private OWLConstant convert2OWLConstant(Object newValue)
			throws UnsupportedDataTypeException {
		OWLConstant toReturn = null;
		if (newValue instanceof Double) {
			toReturn = this.ontologyManager.getOWLDataFactory()
					.getOWLTypedConstant((Double) newValue);
		} else {
			throw new UnsupportedDataTypeException(newValue.getClass()
					.getName());
		}
		return toReturn;
	}

	private Collection<Object> fetch(OWLIndividual currentIndividual,
			String propertyName, boolean isDatatype,
			OWLDescription facetDescription) throws URISyntaxException,
			UnsupportedDataTypeException, OWLReasonerException {
		Collection<Object> toReturn = null;
		Iterator<OWLOntology> it = this.ontologies.iterator();
		boolean found = false;
		OWLOntology ontology;
		while (!found && it.hasNext()) {
			ontology = it.next();
			if (isDatatype) {
				OWLDataProperty dataProperty = this.ontologyManager
						.getOWLDataFactory().getOWLDataProperty(
								new URI(propertyName));
				Set<OWLConstant> values = currentIndividual
						.getDataPropertyValues(ontology).get(dataProperty);
				toReturn = new ArrayList<Object>();
				if (!(values == null || values.isEmpty())) {
					for (OWLConstant value : values) {
						toReturn.add(this.convertValue(value
								.asOWLTypedConstant()));
					}
				}
			} else {
				OWLObjectProperty objectProperty = this.ontologyManager
						.getOWLDataFactory().getOWLObjectProperty(
								new URI(propertyName));
				Set<OWLIndividual> fillers = currentIndividual
						.getObjectPropertyValues(ontology).get(objectProperty);
				toReturn = new HashSet<Object>();
				if (!(fillers == null || fillers.isEmpty())) {
					for (OWLIndividual filler : fillers) {
						if (this.reasoner.hasType(filler, facetDescription,
								false)) {
							toReturn.add(filler);
						}
					}
				}
			}
		}
		return toReturn;
	}

	private Object convertValue(OWLTypedConstant typedConstant)
			throws UnsupportedDataTypeException {
		OWLDataType type = typedConstant.getDataType();
		Object toReturn = null;
		// Rough conversion big if
		if (type.getURI().equals(XSDVocabulary.INT.getURI())
				|| type.getURI().equals(XSDVocabulary.INTEGER.getURI())
				|| type.getURI().equals(XSDVocabulary.DOUBLE.getURI())
				|| type.getURI().equals(XSDVocabulary.DECIMAL.getURI())
				|| type.getURI().equals(XSDVocabulary.SHORT.getURI())) {
			toReturn = new Double(Double
					.parseDouble(typedConstant.getLiteral()));
		} else {
			throw new UnsupportedDataTypeException(
					"Currently unsuported data type - "
							+ type.getURI().toString());
		}
		return toReturn;
	}

	/**
	 * @see uk.ac.manchester.mae.ArithmeticsParserVisitor#visit(uk.ac.manchester.mae.MAEPropertyFacet,
	 *      java.lang.Object)
	 */
	public Object visit(MAEPropertyFacet node, Object data) {
		return null;
	}
}
