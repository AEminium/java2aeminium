package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EVariableDeclarationFragment extends EASTDependentNode
{
	VariableDeclarationFragment origin;
	EExpression expr;
	ESimpleName var;

	EVariableDeclarationFragment(EAST east, VariableDeclarationFragment origin)
	{
		super(east);
		this.origin = origin;
	
		this.var = this.east.extend(origin.getName());
		this.link(this.var);

		if (origin.getInitializer() != null)
		{
			this.expr = this.east.extend(origin.getInitializer());
			this.link(this.expr);
		}
	}

	public List<Statement> translate(Task parent, List<CompilationUnit> cus, List<Statement> prestmts, Type type)
	{
		AST ast = this.east.getAST();
		List<Statement> stmts = new ArrayList<Statement>();

		parent.addField((Type) ASTNode.copySubtree(ast, type), this.origin.getName().toString());

		// initializer
		if (this.expr != null)
		{
			if (this.isRoot())
			{
				this.task = parent.newSubtask(cus);

				Block body = ast.newBlock();
				body.statements().add(this.build(parent, cus, prestmts));

				this.task.setExecute(body);

				List<Task> children = this.getChildTasks(parent, cus, prestmts);
				List<Expression> arguments = new ArrayList<Expression>();
				List<Expression> dependencies = new ArrayList<Expression>();
				arguments.add(ast.newThisExpression());

				for (Task child : children)
				{
					arguments.add(child.getBodyAccess());
					dependencies.add(child.getTaskAccess());
				}
	
				this.task.addConstructor(this.task.createDefaultConstructor(children));
				this.task.setExecute(body);

				prestmts.addAll(this.task.schedule(parent, arguments, dependencies));
			} else
				stmts.add(this.build(parent, cus, prestmts));
		}

		return stmts;
	}

	public Statement build(Task parent, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		AST ast = this.east.getAST();

		Assignment assign = ast.newAssignment();
		
		// FIXME: change to field here or in simplename?
		assign.setLeftHandSide(this.var.translate(parent, cus, prestmts)); // FIXME: translate or build ???
		assign.setRightHandSide(this.expr.translate(parent, cus, prestmts));

		return ast.newExpressionStatement(assign);
	}

	@Override
	public void optimize()
	{
		super.optimize();

		// FIXME:
		this.root = false;
	}
}
