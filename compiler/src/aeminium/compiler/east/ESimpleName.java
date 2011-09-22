package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;


public class ESimpleName extends EExpression
{
	SimpleName origin;

	ESimpleName(EAST east, SimpleName origin)
	{
		super(east);

		this.origin = origin;
	}

	@Override
	public Expression translate(Task parent, List<CompilationUnit> cus, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		// TODO: get this, not from this._parent, but from this._parent._parent.... 
		// cannot be root node
		assert(!this.isRoot());

		FieldAccess root = ast.newFieldAccess();
		root.setExpression(ast.newThisExpression());
		root.setName(ast.newSimpleName("_parent"));

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(root);
		access.setName((SimpleName) ASTNode.copySubtree(ast, this.origin));
		
		return access;
	}

	@Override
	protected List<Task> getTasks(Task parent, List<CompilationUnit> cus, List<Statement> stmts)
	{
		System.err.println("TODO: SimpleName getTasks()");

		return super.getTasks(task, cus, stmts);
	}	

}
