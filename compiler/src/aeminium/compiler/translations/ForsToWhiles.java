package aeminium.compiler.translations;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;

import com.pavlinic.codegen.partialParser.PartialParser;

public class ForsToWhiles extends ASTVisitor {

	public CompilationUnit translate(CompilationUnit cu) {
		cu.accept(this);
		return cu;
	}

	@Override
	public boolean visit(ForStatement node) {	
		
		StringBuilder tmp = new StringBuilder();
		for (Object n : node.initializers()) {
			tmp.append(n.toString() + ";");
		}
		tmp.append(" while (" + node.getExpression() + ") {\n");
		tmp.append(node.getBody());
		for (Object n : node.updaters()) {
			tmp.append(n.toString() + ";");
		}
		tmp.append("\n}");
		Block b = (Block) new PartialParser().parseBlock( tmp.toString() , node.getAST());
		
		node.setExpression(null);
		node.initializers().clear();
		node.updaters().clear();
		
		node.setBody((Statement) b);
		return super.visit(node);
	}

	
	
}
