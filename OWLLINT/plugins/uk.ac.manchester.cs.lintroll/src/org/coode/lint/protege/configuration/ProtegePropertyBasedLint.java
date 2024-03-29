/**
 * 
 */
package org.coode.lint.protege.configuration;

import java.util.Collection;

import org.coode.lint.protege.LintProtegePluginInstance;
import org.semanticweb.owlapi.lint.LintException;
import org.semanticweb.owlapi.lint.LintReport;
import org.semanticweb.owlapi.lint.LintVisitor;
import org.semanticweb.owlapi.lint.LintVisitorEx;
import org.semanticweb.owlapi.lint.configuration.LintConfiguration;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Luigi Iannone
 * 
 */
public final class ProtegePropertyBasedLint<O extends OWLObject> implements
		LintProtegePluginInstance<O> {
	private final LintProtegePluginInstance<O> delegate;
	private final LintConfiguration lintConfiguration;

	/**
	 * @param delegate
	 */
	private ProtegePropertyBasedLint(LintProtegePluginInstance<O> delegate) {
		if (delegate == null) {
			throw new NullPointerException("The delegate cannot be null");
		}
		this.delegate = delegate;
		this.lintConfiguration = new ProtegePreferenceLintConfiguration(this.getId(),
				delegate.getLintConfiguration());
	}

	/**
	 * @param targets
	 * @return
	 * @throws LintException
	 * @see org.semanticweb.owlapi.lint.Lint#detected(java.util.Collection)
	 */
	public LintReport<O> detected(Collection<? extends OWLOntology> targets) throws LintException {
		return this.delegate.detected(targets);
	}

	/**
	 * @return
	 * @see org.semanticweb.owlapi.lint.Lint#getName()
	 */
	public String getName() {
		return this.delegate.getName();
	}

	/**
	 * @return
	 * @see org.semanticweb.owlapi.lint.Lint#getDescription()
	 */
	public String getDescription() {
		return this.delegate.getDescription();
	}

	public boolean isInferenceRequired() {
		return this.delegate.isInferenceRequired();
	}

	/**
	 * @param visitor
	 * @see org.semanticweb.owlapi.lint.Lint#accept(org.semanticweb.owlapi.lint.LintVisitor)
	 */
	public void accept(LintVisitor visitor) {
		this.delegate.accept(visitor);
	}

	/**
	 * @param <P>
	 * @param visitor
	 * @return
	 * @see org.semanticweb.owlapi.lint.Lint#accept(org.semanticweb.owlapi.lint.LintVisitorEx)
	 */
	public <P> P accept(LintVisitorEx<P> visitor) {
		return this.delegate.accept(visitor);
	}

	/**
	 * @return
	 * @see org.semanticweb.owlapi.lint.Lint#getLintConfiguration()
	 */
	public LintConfiguration getLintConfiguration() {
		return this.lintConfiguration;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return this.delegate.equals(obj);
	}

	public void dispose() throws Exception {
		this.delegate.dispose();
	}

	public void initialise() throws Exception {
		this.delegate.initialise();
	}

	public static <P extends OWLObject> ProtegePropertyBasedLint<P> buildProtegePropertyBasedLint(
			LintProtegePluginInstance<P> lint) {
		return new ProtegePropertyBasedLint<P>(lint);
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

	public String getId() {
		return this.delegate.getId();
	}
}
