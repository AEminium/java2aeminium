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
	public Statement translate(Task parent)
	{
		System.err.println("TODO: EBlock translate");
		return null;
	}

	public Block build(Task parent)
	{
		AST ast = this.east.getAST();
		Block block = ast.newBlock();

		for (EStatement stmt : this.stmts)
			if (stmt.isRoot())
				block.statements().add(stmt.translate(parent));

		return block;
	}
}
