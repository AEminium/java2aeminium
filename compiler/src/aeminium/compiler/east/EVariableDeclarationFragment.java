package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EVariableDeclarationFragment extends EASTDependentNode
{
	VariableDeclarationFragment origin;
	EExpression expr;
	ESimpleName var;

	IBinding binding;

	EVariableDeclarationFragment(EAST east, VariableDeclarationFragment origin)
	{
		super(east);
		this.origin = origin;
	
		this.var = this.east.extend(origin.getName());

		if (origin.getInitializer() != null)
			this.expr = this.east.extend(origin.getInitializer());
	}

	@Override
	public void optimize()
	{
		super.optimize();
	
		if (this.expr != null)
			this.expr.optimize();

		this.binding = this.origin.resolveBinding();
	}

	public List<Statement> translate(Task parent, Type type)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(parent, type);

		this.task = parent.newStrongDependency("declfrag");

		Block execute = ast.newBlock();
		execute.statements().addAll(this.build(task, type));
		task.setExecute(execute);

		MethodDeclaration constructor = task.createConstructor();
		task.addConstructor(constructor);

/*
		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName(this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());


*/
		return new ArrayList<Statement>();
	}

	public List<Statement> build(Task task, Type type)
	{
		AST ast = this.east.getAST();

		task.addField(type, this.origin.getName().toString());
		this.east.putNode(this.east.resolveName(this.binding), this);

		List<Statement> stmts = new ArrayList<Statement>();

		if (this.expr != null)
		{
			FieldAccess field = ast.newFieldAccess();
			field.setExpression(ast.newThisExpression());
			field.setName((SimpleName) ASTNode.copySubtree(ast, this.origin.getName()));

			Assignment assign = ast.newAssignment();
			assign.setLeftHandSide(field);
			assign.setRightHandSide(this.expr.translate(task));
			
			stmts.add(ast.newExpressionStatement(assign));
		}

		return stmts;
	}
}
