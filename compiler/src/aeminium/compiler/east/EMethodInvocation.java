package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EMethodInvocation extends EExpression
{
	EAST east;
	MethodInvocation origin;

	EMethodInvocation(EAST east, MethodInvocation origin)
	{
		this.east = east;
		this.origin = origin;
	
	}

	@Override
	public Expression translate(TypeDeclaration decl, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		assert (this.isRoot());

		// TODO
		FieldAccess ret = ast.newFieldAccess();

		return ret;
	}

	@Override
	public boolean isRoot()
	{
		return true;
	}
}
