/* Generated By:JJTree: Do not edit this line. OPPLClause.java */
package org.coode.oppl.syntax;

public @SuppressWarnings("all")
class OPPLClause extends SimpleNode {
	public OPPLClause(int id) {
		super(id);
	}

	public OPPLClause(OPPLParser p, int id) {
		super(p, id);
	}

	@Override
	public String toString() {
		return "";
	}

	@Override
	public String toString(String prefix) {
		return "";
	}

	/** Accept the visitor. * */
	public Object jjtAccept(OPPLParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
}