/* Generated By:JJTree: Do not edit this line. MAEStart.java */
package uk.ac.manchester.mae.parser;

public class MAEStart extends SimpleNode {
	public MAEStart(int id) {
		super(id);
	}

	public MAEStart(ArithmeticsParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. */
	@Override
	public Object jjtAccept(ArithmeticsParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	@Override
	public String toString() {
		StringBuilder toReturn = new StringBuilder();
		for (int i = 0; i < this.children.length; i++) {
			boolean isFirstBinding = this.children[i] instanceof MAEBinding
					&& (i == 0 || !(this.children[i - 1] instanceof MAEBinding));
			if (isFirstBinding) {
				toReturn.append("{");
			}
			boolean bindingfinished = i > 0
					&& this.children[i - 1] instanceof MAEBinding
					&& !(this.children[i] instanceof MAEBinding);
			boolean isLastBinding = i < this.children.length - 1
					&& this.children[i] instanceof MAEBinding
					&& !(this.children[i + 1] instanceof MAEBinding);
			if (bindingfinished) {
				toReturn.append("}->");
			}
			if (this.children[i] instanceof MAEStoreTo) {
				toReturn.append(" STORETO <");
				toReturn.append(this.children[i].toString());
				toReturn.append(">");
			} else if (this.children[i] instanceof MAEmanSyntaxClassExpression) {
				toReturn.append(" APPLIESTO <");
				toReturn.append(this.children[i].toString());
				toReturn.append(">");
			} else if (this.children[i] instanceof MAEBinding) {
				toReturn.append(this.children[i].toString());
				if (!isLastBinding) {
					toReturn.append(",");
				}
			} else {
				toReturn.append(" ");
				toReturn.append(this.children[i].toString());
			}
		}
		toReturn.append(";");
		return toReturn.toString();
	}
}
