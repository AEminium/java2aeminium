package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Task;

public class EBlock extends EStatement
{
	private final Block origin;
	private final List<EStatement> stmts;

	EBlock(EAST east, Block origin)
	{
		super(east);

		this.origin = origin;
		this.stmts = new ArrayList<EStatement>();

		for (Object stmt : origin.statements())
			this.stmts.add(this.east.extend((Statement) stmt));
	}

	@Override
	public void analyse()
	{
		super.analyse();

		for (EStatement stmt : this.stmts)
			stmt.analyse();

		for (EStatement stmt : this.stmts)
			this.signature.addFrom(stmt.getSignature());
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		for (EStatement stmt : this.stmts)
			sum += stmt.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		// TODO: this.task
		for (EStatement stmt : this.stmts)
			stmt.preTranslate(parent);
	}
	
	@Override
	public List<Statement> translate(List<CompilationUnit> cus)
	{
		System.err.println("TODO: EBlock translate");
		return null;
	}

	@SuppressWarnings("unchecked")
	public Block build(Task parent, List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();
		Block block = ast.newBlock();

		for (EStatement stmt : this.stmts)
		{
			if (stmt.isRoot())
				block.statements().addAll(stmt.translate(cus));
		}

		return block;
	}
}
