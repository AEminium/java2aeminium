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
	protected List<Task> getChildTasks(Task task, List<CompilationUnit> cus, List<Statement> stmts)
	{
		List<Task> tasks = new ArrayList<Task>();
		tasks.addAll(super.getTasks(task, cus, stmts));
		
		for (EStatement stmt : this.stmts)
			tasks.addAll(stmt.getTasks(task, cus, stmts));

		return tasks;
	}

	@Override
	public List<Statement> translate(Task parent, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		// unsuported yet 
		assert( this.isRoot() == false);

		List<Statement> stmts = new ArrayList<Statement>();
			
		if (this.isRoot())
		{
			// TODO
			System.err.println("TODO: EBlock");
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
			if (stmt.isRoot())
				block.statements().addAll(stmt.translate(parent, cus, prestmts));

		return block;
	}
}	
