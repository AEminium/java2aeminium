package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EExpressionStatement extends EStatement
{
	ExpressionStatement origin;
	EExpression expr;

	EExpressionStatement(EAST east, ExpressionStatement origin)
	{
		super(east);
		this.origin = origin;

		this.expr = this.east.extend((Expression) origin.getExpression());
		this.link(this.expr);
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
			this.expr.translate(this.task, cus, body.statements());

			List<Expression> dependencies = new ArrayList<Expression>();
			List<Expression> arguments = new ArrayList<Expression>();
			arguments.add(ast.newThisExpression());

			List<Task> children = this.getChildTasks();
			for (Task child : children)
			{
				arguments.add(child.getBodyAccess());
				dependencies.add(child.getTaskAccess());
			}
			System.out.println(this.childs);
			this.task.addConstructor(this.task.createDefaultConstructor(children));
			this.task.setExecute(body);

			prestmts.addAll(this.task.schedule(parent, arguments, dependencies));
		} 

		return stmts;
	}
}
