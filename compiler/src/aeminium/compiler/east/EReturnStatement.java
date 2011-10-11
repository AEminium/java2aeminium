package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EReturnStatement extends EStatement
{
	ReturnStatement origin;
	EExpression expr;

	EReturnStatement(EAST east, ReturnStatement origin)
	{
		super(east);
		this.origin = origin;

		if (origin.getExpression() != null)
		{
			this.expr = this.east.extend((Expression) origin.getExpression());
			this.link(this.expr);
		}
	}

	@Override
	public List<Statement> translate(Task parent, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		AST ast = this.east.getAST();
		List <Statement> stmts = new ArrayList<Statement>();

		// TODO:
		assert(this.isRoot());

		if (this.isRoot())
		{
			this.task = parent.newSubtask(cus);

			Block body = ast.newBlock();

			if (this.expr != null)
			{
				// this._root._ret = ...;
				Assignment assign = ast.newAssignment();
	
				FieldAccess parentaccess = ast.newFieldAccess();
				parentaccess.setExpression(ast.newThisExpression());
				parentaccess.setName(ast.newSimpleName("_parent"));

				FieldAccess ret = ast.newFieldAccess();
				ret.setExpression(parentaccess);
				ret.setName(ast.newSimpleName("_ret"));

				assign.setLeftHandSide(ret);
				assign.setRightHandSide(this.expr.translate(this.task, cus, prestmts));
	
				ExpressionStatement stmt = ast.newExpressionStatement(assign);
				body.statements().add(stmt);
			}
		
			// TODO: mark task as finished?
		
			List<Expression> dependencies = new ArrayList<Expression>();
			List<Expression> arguments = new ArrayList<Expression>();
			arguments.add(ast.newThisExpression());

			List<Task> children = this.getChildTasks();
			for (Task child : children)
			{
				arguments.add(child.getBodyAccess());
				dependencies.add(child.getTaskAccess());
			}

			this.task.addConstructor(this.task.createDefaultConstructor(children));
			this.task.setExecute(body);

			prestmts.addAll(this.task.schedule(parent, arguments, dependencies));

		} 

		return stmts;
	}
}
