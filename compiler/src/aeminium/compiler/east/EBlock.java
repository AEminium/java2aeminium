package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

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
	protected List<Expression> getChildDependencies(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		List<Expression> exprs = new ArrayList<Expression>();
		exprs.addAll(super.getDependencies(method, cus, stmts));
		
		for (EStatement stmt : this.stmts)
			exprs.addAll(stmt.getDependencies(method, cus, stmts));
		return exprs;
	}
	@Override
	public void translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		// unsuported yet 
		assert( this.isRoot() == false);
			
		if (this.isRoot())
		{
			// TODO
			System.err.println("TODO: EBlock");
		} else
			stmts.add(this.build(method, cus));
	}

	public Block build(EMethodDeclaration method, List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();
		Block block = ast.newBlock();

		for (EStatement stmt : this.stmts)
			if (stmt.isRoot())
				stmt.translate(method, cus, (List<Statement>) block.statements());

		return block;
	}
}	
