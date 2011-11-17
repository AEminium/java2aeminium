package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EAssignment extends EExpression
{
	Assignment origin;
	EExpression left;
	EExpression right;

	ITypeBinding type;

	EAssignment(EAST east, Assignment origin)
	{
		super(east);

		this.origin = origin;

		this.left = this.east.extend(origin.getLeftHandSide());
		this.right = this.east.extend(origin.getRightHandSide());
		this.type = this.origin.resolveTypeBinding();
	}

	@Override
	public void optimize()
	{
		super.optimize();

		this.left.optimize();
		this.right.optimize();
	}

	@Override
	public Expression translate(Task parent, boolean write)
	{
		System.err.println("TODO: EAssignment translate");
		return null;
	}
}
