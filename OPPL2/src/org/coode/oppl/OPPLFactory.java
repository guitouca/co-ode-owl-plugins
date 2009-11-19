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
package org.coode.oppl;

import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.coode.oppl.entity.OWLEntityCreationException;
import org.coode.oppl.entity.OWLEntityCreationSet;
import org.coode.oppl.entity.OWLEntityRenderer;
import org.coode.oppl.entity.OWLEntityRendererImpl;
import org.coode.oppl.exceptions.OPPLException;
import org.coode.oppl.rendering.ManchesterSyntaxRenderer;
import org.coode.oppl.rendering.VariableOWLEntityRenderer;
import org.coode.oppl.utils.ArgCheck;
import org.coode.oppl.variablemansyntax.ConstraintSystem;
import org.coode.oppl.variablemansyntax.Variable;
import org.coode.oppl.variablemansyntax.VariableScopeChecker;
import org.semanticweb.owl.expression.OWLEntityChecker;
import org.semanticweb.owl.expression.ShortFormEntityChecker;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLAxiomChange;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDeclarationAxiom;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChange;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owl.util.SimpleShortFormProvider;

import uk.ac.manchester.cs.owl.mansyntaxrenderer.ManchesterOWLSyntaxObjectRenderer;

/**
 * @author Luigi Iannone
 * 
 */
public class OPPLFactory implements OPPLAbstractFactory {
	private class EntityFactory implements
			org.coode.oppl.entity.OWLEntityFactory {
		public EntityFactory() {
		}

		/**
		 * @param shortName
		 * @return
		 */
		private URI buildURI(String shortName) {
			return URI.create(getConstraintSystem().getOntology().getURI()
					.toString()
					+ "#" + shortName);
		}

		public OWLEntityCreationSet<OWLClass> createOWLClass(String shortName,
				URI baseURI) throws OWLEntityCreationException {
			return this.createOWLEntity(OWLClass.class, shortName, baseURI);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.coode.oppl.EntityFactory#createOWLObjectProperty(java.lang.String
		 * , java.net.URI)
		 */
		public OWLEntityCreationSet<OWLObjectProperty> createOWLObjectProperty(
				String shortName, URI baseURI)
				throws OWLEntityCreationException {
			return this.createOWLEntity(OWLObjectProperty.class, shortName,
					baseURI);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.coode.oppl.EntityFactory#createOWLDataProperty(java.lang.String,
		 * java.net.URI)
		 */
		public OWLEntityCreationSet<OWLDataProperty> createOWLDataProperty(
				String shortName, URI baseURI)
				throws OWLEntityCreationException {
			return this.createOWLEntity(OWLDataProperty.class, shortName,
					baseURI);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.coode.oppl.EntityFactory#createOWLIndividual(java.lang.String,
		 * java.net.URI)
		 */
		public OWLEntityCreationSet<OWLIndividual> createOWLIndividual(
				String shortName, URI baseURI)
				throws OWLEntityCreationException {
			return this
					.createOWLEntity(OWLIndividual.class, shortName, baseURI);
		}

		private <T extends OWLEntity> boolean isValidNewID(String shortName,
				URI baseURI, Class<T> type) {
			return baseURI.equals(getConstraintSystem().getOntology().getURI());
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.coode.oppl.EntityFactory#createOWLEntity(java.lang.Class,
		 * java.lang.String, java.net.URI)
		 */
		public <T extends OWLEntity> OWLEntityCreationSet<T> createOWLEntity(
				Class<T> type, String shortName, URI baseURI)
				throws OWLEntityCreationException {
			URI anURI = buildURI(shortName);
			T entity = this.getOWLEntity(type, anURI);
			OWLDeclarationAxiom declarationAxiom = getOWLDataFactory()
					.getOWLDeclarationAxiom(entity);
			List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
			changes.add(new AddAxiom(OPPLFactory.this.constraintSystem
					.getOntology(), declarationAxiom));
			return new OWLEntityCreationSet<T>(entity, changes);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.coode.oppl.EntityFactory#tryCreate(java.lang.Class,
		 * java.lang.String, java.net.URI)
		 */
		public void tryCreate(Class<? extends OWLEntity> type,
				String shortName, URI baseURI)
				throws OWLEntityCreationException {
			if (!this.isValidNewID(shortName, baseURI, type)) {
				throw new OWLEntityCreationException("Invalid name: "
						+ shortName + "for an " + type.getName());
			}
		}

		@SuppressWarnings("unchecked")
		private <T extends OWLEntity> T getOWLEntity(Class<T> type, URI uri) {
			if (OWLClass.class.isAssignableFrom(type)) {
				return (T) getOWLDataFactory().getOWLClass(uri);
			} else if (OWLObjectProperty.class.isAssignableFrom(type)) {
				return (T) getOWLDataFactory().getOWLObjectProperty(uri);
			} else if (OWLDataProperty.class.isAssignableFrom(type)) {
				return (T) getOWLDataFactory().getOWLDataProperty(uri);
			} else if (OWLIndividual.class.isAssignableFrom(type)) {
				return (T) getOWLDataFactory().getOWLIndividual(uri);
			}
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.coode.oppl.EntityFactory#preview(java.lang.Class,
		 * java.lang.String, java.net.URI)
		 */
		public <T extends OWLEntity> OWLEntityCreationSet<T> preview(
				Class<T> type, String shortName, URI baseURI)
				throws OWLEntityCreationException {
			return this.createOWLEntity(type, shortName, baseURI);
		}
	}

	private final OWLOntologyManager ontologyManager;
	protected ConstraintSystem constraintSystem;
	private VariableScopeChecker variableScopeChecker = null;
	private final OWLReasoner reasoner;
	private final OWLOntology ontology;

	/**
	 * @param ontologyManager
	 * @param constraintSystem
	 * @param dataFactory
	 */
	public OPPLFactory(OWLOntologyManager ontologyManager,
			OWLOntology ontology, OWLReasoner reasoner) {
		this.ontologyManager = ontologyManager;
		this.ontology = ontology;
		this.reasoner = reasoner;
	}

	/**
	 * 
	 * @see org.coode.oppl.OPPLAbstractFactory#getOWLEntityChecker()
	 */
	public OWLEntityChecker getOWLEntityChecker() {
		BidirectionalShortFormProviderAdapter bshp = new BidirectionalShortFormProviderAdapter(
				this.ontologyManager.getOntologies(),
				new SimpleShortFormProvider());
		// XXX fix for missing Thing
		bshp.add(this.ontologyManager.getOWLDataFactory().getOWLThing());
		return new ShortFormEntityChecker(bshp);
	}

	/**
	 * @return the variableScopeChecker
	 * @throws OPPLException
	 */
	public VariableScopeChecker getVariableScopeChecker() throws OPPLException {
		if (this.variableScopeChecker == null && this.reasoner != null) {
			this.variableScopeChecker = new VariableScopeChecker(
					this.ontologyManager, this.reasoner);
		}
		return this.variableScopeChecker;
	}

	public OWLEntityRenderer getOWLEntityRenderer(ConstraintSystem cs) {
		ArgCheck.checkNullArgument("The constraint system", cs);
		OWLEntityRendererImpl defaultRenderer = new OWLEntityRendererImpl();
		return new VariableOWLEntityRenderer(cs, defaultRenderer);
	}

	public org.coode.oppl.entity.OWLEntityFactory getOWLEntityFactory() {
		return new EntityFactory();
	}

	public OPPLScript buildOPPLScript(ConstraintSystem constraintSystem1,
			List<Variable> variables, OPPLQuery opplQuery,
			List<OWLAxiomChange> actions) {
		return new OPPLScriptImpl(constraintSystem1, variables, opplQuery,
				actions);
	}

	public OPPLQuery buildNewQuery(ConstraintSystem constraintSystem1) {
		return new OPPLQueryImpl(constraintSystem1);
	}

	public ManchesterOWLSyntaxObjectRenderer getOWLObjectRenderer(
			StringWriter writer) {
		ManchesterOWLSyntaxObjectRenderer renderer = new ManchesterOWLSyntaxObjectRenderer(
				writer);
		renderer.setShortFormProvider(new SimpleVariableShortFormProvider(
				getConstraintSystem()));
		return renderer;
	}

	/**
	 * @return the constraintSystem
	 */
	protected final ConstraintSystem getConstraintSystem() {
		return this.constraintSystem == null ? createConstraintSystem()
				: this.constraintSystem;
	}

	public ConstraintSystem createConstraintSystem() {
		this.constraintSystem = this.reasoner == null ? new ConstraintSystem(
				this.ontology, this.ontologyManager) : new ConstraintSystem(
				this.ontology, this.ontologyManager, this.reasoner);
		return this.constraintSystem;
	}

	/**
	 * @return the OWLDataFactory instance used by the internal
	 *         OWLOntologyManager instance
	 * @see org.coode.oppl.OPPLAbstractFactory#getOWLDataFactory()
	 */
	public OWLDataFactory getOWLDataFactory() {
		return this.ontologyManager.getOWLDataFactory();
	}

	public ManchesterSyntaxRenderer getManchesterSyntaxRenderer(
			ConstraintSystem cs) {
		ArgCheck.checkNullArgument("The constraint system", cs);
		return new ManchesterSyntaxRenderer(this.ontologyManager,
				getOWLEntityRenderer(cs), cs);
	}

	public OWLOntologyManager getOntologyManager() {
		return this.ontologyManager;
	}
}