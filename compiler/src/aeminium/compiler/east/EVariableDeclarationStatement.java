package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

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
	public void translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		for (EVariableDeclarationFragment frag : this.frags)
			frag.translate(method, cus, stmts, this.origin.getType());
	}

	@Override
	public void optimize()
	{
		for (EVariableDeclarationFragment frag : this.frags)
			frag.optimize();
	}
}
