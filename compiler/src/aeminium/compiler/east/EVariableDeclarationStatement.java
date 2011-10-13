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
			this.frags.add(this.east.extend((VariableDeclarationFragment) frag));
	}

	@Override
	public Statement translate(Task parent)
	{
		List<Statement> stmts = new ArrayList<Statement>();
		System.err.println("TODO: VariableDeclarationStatement");
		return null;
	}
}
