package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.east.EAST;
import aeminium.compiler.east.EASTNode;

public abstract class EStatement extends EASTDependentNode
{
	/* TODO: Anything else? */
	public abstract void translate(boolean sequential, AST ast, List<Statement> stmts);
}
