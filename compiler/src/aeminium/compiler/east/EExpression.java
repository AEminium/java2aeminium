package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;

public abstract class EExpression extends EASTDependentNode
{
	/* TODO: Anything else? */
	public abstract Expression translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts);
}
