package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EVariableDeclarationFragment extends EASTDependentNode
{
	VariableDeclarationFragment origin;
	EExpression expr;
	ESimpleName var;

	EVariableDeclarationFragment(EAST east, VariableDeclarationFragment origin)
	{
		super(east);
		this.origin = origin;
	
		this.var = this.east.extend(origin.getName());

		if (origin.getInitializer() != null)
			this.expr = this.east.extend(origin.getInitializer());
	}

	public Statement translate(Task parent)
	{
		System.err.println("TODO: Variable DeclarationStatement");
		return null;		
	}

	@Override
	public void optimize()
	{
		super.optimize();
		this.root = false;
	}
}
