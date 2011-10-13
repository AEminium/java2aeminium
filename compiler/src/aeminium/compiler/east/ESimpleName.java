package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;


public class ESimpleName extends EExpression
{
	SimpleName origin;

	ESimpleName(EAST east, SimpleName origin)
	{
		super(east);

		this.origin = origin;
	}

	@Override
	public void optimize()
	{
		super.optimize();
		this.root = false;
	}

	@Override
	public Expression translate(Task parent)
	{
		System.err.println("TODO: SimpleName");
		return null;
	}
}
