package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.CatchClause;

public class ECatchClause extends EASTNode {
	

	public ECatchClause(EAST east, CatchClause original) {
		super(east, original);
		
		
	}

	@Override
	public CatchClause getOriginal() {

		return (CatchClause) this.original;

	}

}
