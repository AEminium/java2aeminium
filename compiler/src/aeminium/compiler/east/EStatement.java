package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public abstract class EStatement extends EASTDependentNode
{
	/* TODO: Anything else? */
	public abstract void translate(EMethodDeclaration decl, List<CompilationUnit> cus, List<Statement> stmts);
}
