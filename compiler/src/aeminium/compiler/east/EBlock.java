package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.east.EAST;
import aeminium.compiler.east.EStatement;

public class EBlock extends EStatement
{
	Block origin;
	List<EStatement> stmts;

	EBlock(Block origin)
	{
		this.origin = origin;
		this.stmts = new ArrayList<EStatement>();

		for (Object stmt : origin.statements())
			this.stmts.add(EAST.extend((Statement) stmt));
	}

	@Override
	public void translate(boolean sequential, AST ast, List<Statement> stmts)
	{
		// TODO
		System.out.println("TODO EBlock");
	}
}
