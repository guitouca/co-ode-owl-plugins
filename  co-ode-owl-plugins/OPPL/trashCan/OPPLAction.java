/* Generated By:JJTree: Do not edit this line. OPPLAction.java */
package org.coode.oppl.syntax;

import org.semanticweb.owl.model.OWLAxiom;

public @SuppressWarnings("all")
class OPPLAction extends SimpleNode {
	protected String action;
	protected OWLAxiom axiom;

	public OPPLAction(int id) {
		super(id);
	}

	public OPPLAction(OPPLParser p, int id) {
		super(p, id);
	}

	public void setActionType(String s) {
		this.action = s;
	}

	public void setAxiom(OWLAxiom axiom) {
		this.axiom = axiom;
	}

	@Override
	public String toString() {
		return this.action + " " + this.axiom.toString();
	}

	@Override
	public String toString(String prefix) {
		return "";
	}

	/**
	 * @return the action
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * @return the axiom
	 */
	public OWLAxiom getAxiom() {
		return this.axiom;
	}

	/** Accept the visitor. * */
	public Object jjtAccept(OPPLParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
}