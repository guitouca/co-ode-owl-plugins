package org.coode.oae.ui;

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.frame.AbstractOWLFrameSection;
import org.protege.editor.owl.ui.frame.OWLFrame;
import org.protege.editor.owl.ui.frame.OWLFrameSectionRow;
import org.protege.editor.owl.ui.frame.OWLFrameSectionRowObjectEditor;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.model.OWLAnnotationAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLEntityAnnotationAxiom;
import org.semanticweb.owl.model.OWLOntology;

import uk.ac.manchester.mae.evaluation.FormulaModel;
import uk.ac.manchester.mae.parser.MAEStart;

public class OWLCalculationsFormulaClassFrameSection
		extends
		AbstractOWLFrameSection<OWLClass, OWLAnnotationAxiom<OWLDataProperty>, FormulaModel> {
	private static final String LABEL = "Formulas";
	protected Map<MAEStart, URI> formulaAnnotationURIs = new HashMap<MAEStart, URI>();
	protected Map<MAEStart, OWLDataProperty> formulaProperties = new HashMap<MAEStart, OWLDataProperty>();
	protected boolean inferredFormulas = true;

	protected OWLCalculationsFormulaClassFrameSection(OWLEditorKit editorKit,
			OWLFrame<? extends OWLClass> frame) {
		super(editorKit, LABEL, frame);
	}

	@Override
	protected void clear() {
	}

	@Override
	public boolean canAddRows() {
		return false;
	}

	@Override
	protected OWLAnnotationAxiom<OWLDataProperty> createAxiom(
			FormulaModel object) {
		return null;
	}

	@Override
	public OWLFrameSectionRowObjectEditor<FormulaModel> getObjectEditor() {
		// return new OWLArithmeticFormulaEditor(this.getOWLEditorKit(), this
		// .getRootObject(), false, this.formulaAnnotationURIs);
		return new OWLCalculationsFormulaEditor(getOWLEditorKit());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void refill(OWLOntology ontology) {
		for (OWLDataProperty dataProperty : ontology
				.getReferencedDataProperties()) {
			Set<OWLAnnotationAxiom> annotationAxioms = dataProperty
					.getAnnotationAxioms(ontology);
			for (OWLAnnotationAxiom annotationAxiom : annotationAxioms) {
				OWLArithmeticsAxiomFormulaExtractor visitor = new OWLArithmeticsAxiomFormulaExtractor(
						getRootObject(), getOWLModelManager());
				annotationAxiom.accept(visitor);
				if (visitor.getExtractedFormula() != null) {
					addRow(new OWLCalculationsFormulaClassFrameSectionRow(
							getOWLEditorKit(), this, ontology, getRootObject(),
							annotationAxiom));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void refillInferred() throws OWLReasonerException {
		if (this.inferredFormulas) {
			boolean isSatisfiable = getOWLModelManager().getReasoner()
					.isSatisfiable(getRootObject());
			if (isSatisfiable) {
				for (Set<OWLClass> superClassSet : getOWLModelManager()
						.getReasoner().getAncestorClasses(getRootObject())) {
					for (OWLClass superClass : superClassSet) {
						for (OWLOntology ontology : getOWLModelManager()
								.getOntologies()) {
							for (OWLDataProperty dataProperty : ontology
									.getReferencedDataProperties()) {
								Set<OWLAnnotationAxiom> annotationAxioms = dataProperty
										.getAnnotationAxioms(ontology);
								for (OWLAnnotationAxiom annotationAxiom : annotationAxioms) {
									OWLArithmeticsAxiomFormulaExtractor visitor = new OWLArithmeticsAxiomFormulaExtractor(
											superClass, getOWLModelManager());
									annotationAxiom.accept(visitor);
									if (visitor.getExtractedFormula() != null) {
										addRow(new OWLCalculationsFormulaClassFrameSectionRow(
												getOWLEditorKit(), this, null,
												getRootObject(),
												annotationAxiom));
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public Comparator<OWLFrameSectionRow<OWLClass, OWLAnnotationAxiom<OWLDataProperty>, FormulaModel>> getRowComparator() {
		return null;
	}

	@Override
	public void visit(OWLEntityAnnotationAxiom axiom) {
		OWLArithmeticsAxiomFormulaExtractor visitor = new OWLArithmeticsAxiomFormulaExtractor(
				getRootObject(), getOWLModelManager());
		axiom.accept(visitor);
		if (visitor.getExtractedFormula() != null) {
			reset();
		}
	}
}
