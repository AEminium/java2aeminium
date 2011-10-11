package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EBlock extends EStatement
{
	Block origin;
	List<EStatement> stmts;

	EBlock(EAST east, Block origin)
	{
		super(east);

		this.east = east;
		this.origin = origin;
		this.stmts = new ArrayList<EStatement>();

		for (Object stmt : origin.statements())
			this.stmts.add(this.east.extend((Statement) stmt));
	}

	@Override
	public void optimize()
	{
		super.optimize();
		
		for (EStatement stmt : this.stmts)
			stmt.optimize();
	}

	@Override
	protected List<Task> getChildTasks()
	{
		List<Task> tasks = new ArrayList<Task>();
		tasks.addAll(super.getTasks());
		
		for (EStatement stmt : this.stmts)
			tasks.addAll(stmt.getTasks());

		return tasks;
	}

	@Override
	public List<Statement> translate(Task parent, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		List<Statement> stmts = new ArrayList<Statement>();
			
		if (this.isRoot())
		{
			AST ast = this.east.getAST();
			this.task = parent.newSubtask(cus);

			Block body = ast.newBlock();
			body.statements().add(this.build(this.task, cus, body.statements()));
			
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

		} else
			stmts.add(this.build(parent, cus, prestmts));

		return stmts;
	}

	public Block build(Task parent, List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();
		Block block = ast.newBlock();

		List<Statement> stmts = new ArrayList<Statement>();

		for (EStatement stmt : this.stmts)
			if (stmt.isRoot())
				stmts.addAll(stmt.translate(parent, cus, block.statements()));

		block.statements().addAll(stmts);
		return block;
	}

	public Block build(Task parent, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		AST ast = this.east.getAST();
		Block block = ast.newBlock();

		for (EStatement stmt : this.stmts)
			if (stmt.parents.size() == 0) // the roots
				block.statements().addAll(stmt.translate(parent, cus, prestmts));

		return block;
	}
}	
