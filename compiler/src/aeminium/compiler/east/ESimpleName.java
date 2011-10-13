package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;


public class ESimpleName extends EExpression
{
	SimpleName origin;
	IBinding binding;

	ESimpleName(EAST east, SimpleName origin)
	{
		super(east);

		this.origin = origin;
	}

	@Override
	public void optimize()
	{
		super.optimize();

		this.root = false;
		this.binding = this.origin.resolveBinding();
	}

	@Override
	public Expression translate(Task parent)
	{
		AST ast = this.east.getAST();

		String variable = this.east.resolveName(this.binding);
		EASTDependentNode node = (EASTDependentNode) this.east.getNode(variable);

		FieldAccess field = ast.newFieldAccess();
		field.setExpression(parent.getPathToTask(node.task));
		field.setName((SimpleName) ASTNode.copySubtree(ast, this.origin));

		// FIXME: this is wrong, not only it depends on the creation, but also on the last task that
		// accessed it. That can be determined in compile time, if no conditional code is found
		// (no loops or ifs or ternary operators)

		parent.addWeakDependency(node.task);

		return field;
	}
}
