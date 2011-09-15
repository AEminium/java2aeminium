package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EVariableDeclarationStatement extends EStatement
{
	EAST east;
	VariableDeclarationStatement origin;
	List<EVariableDeclarationFragment> frags;

	EVariableDeclarationStatement(EAST east, VariableDeclarationStatement origin)
	{
		this.east = east;
		this.origin = origin;
		this.frags = new ArrayList<EVariableDeclarationFragment>();

		for (Object frag : origin.fragments())
		{
			this.frags.add(this.east.extend((VariableDeclarationFragment) frag));

			// TODO: link them?			
		}
	}

	@Override
	public void translate(TypeDeclaration decl, List<Statement> stmts)
	{
		for (EVariableDeclarationFragment frag : this.frags)
			frag.translate(decl, stmts, this.origin.getType());
	}
}
