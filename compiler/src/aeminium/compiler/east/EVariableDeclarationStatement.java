package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EVariableDeclarationStatement extends EStatement
{
	VariableDeclarationStatement origin;
	List<EVariableDeclarationFragment> frags;

	EVariableDeclarationStatement(EAST east, VariableDeclarationStatement origin)
	{
		super(east);

		this.origin = origin;
		this.frags = new ArrayList<EVariableDeclarationFragment>();

		for (Object frag : origin.fragments())
		{
			EVariableDeclarationFragment efrag = this.east.extend((VariableDeclarationFragment) frag);
			this.frags.add(efrag);
			this.link(efrag);
		}
	}

	@Override
	public List<Statement> translate(Task parent, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		List<Statement> stmts = new ArrayList<Statement>();

		if (this.isRoot())
		{
			this.task = parent.newSubtask(cus);

			// TODO: IMPROVE alow @AEminium on constructors? 
			AST ast = this.east.getAST();

			// task body
			Block body = ast.newBlock();

			for (EVariableDeclarationFragment frag : this.frags)
				body.statements().addAll(frag.translate(parent, cus, prestmts, this.origin.getType()));


			List<Task> children = this.getChildTasks(this.task, cus, prestmts);
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

			stmts.addAll(this.task.schedule(parent, arguments, dependencies));
		} else
			for (EVariableDeclarationFragment frag : this.frags)
				stmts.addAll(frag.translate(parent, cus, prestmts, this.origin.getType()));

		return stmts;
	}

	@Override
	public void optimize()
	{
		super.optimize();
	}
}
