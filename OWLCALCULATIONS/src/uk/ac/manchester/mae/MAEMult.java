/* Generated By:JJTree: Do not edit this line. MAEMult.java */
package uk.ac.manchester.mae;

public class MAEMult extends SimpleNode {
	private boolean isMultiplication;
	private boolean isPercentage;

	public MAEMult(int id) {
		super(id);
	}

	public MAEMult(ArithmeticsParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. * */
	@Override
	public Object jjtAccept(ArithmeticsParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	public void setMultiplication(boolean b) {
		this.isMultiplication = b;
	}

	public boolean isMultiplication() {
		return this.isMultiplication;
	}

	public void setPercentage(boolean b) {
		this.isPercentage = b;
	}

	public boolean isPercentage() {
		return this.isPercentage;
	}
}
