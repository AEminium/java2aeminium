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
	EAST east;
	Block origin;
	List<EStatement> stmts;

	EBlock(EAST east, Block origin)
	{
		this.east = east;
		this.origin = origin;
		this.stmts = new ArrayList<EStatement>();

		for (Object stmt : origin.statements())
			this.stmts.add(this.east.extend((Statement) stmt));
	}

	@Override
	public void translate(List<Statement> stmts)
	{
		// TODO
	}
}
