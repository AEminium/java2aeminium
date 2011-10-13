package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EInfixExpression extends EExpression
{
	InfixExpression origin;
	EExpression left;
	EExpression right;
	List<EExpression> extended;

	Object constant;
	ITypeBinding type;

	EInfixExpression(EAST east, InfixExpression origin)
	{
		super(east);

		this.origin = origin;

		this.left = this.east.extend(origin.getLeftOperand());
		this.right = this.east.extend(origin.getRightOperand());

		if (origin.hasExtendedOperands())
		{
			this.extended = new ArrayList<EExpression>();
			for (Object ext : origin.extendedOperands())
				this.extended.add(this.east.extend((Expression) ext));	
		}

		// TODO: allow constant resolving
		this.constant = this.origin.resolveConstantExpressionValue();
		this.type = this.origin.resolveTypeBinding();
	}

	@Override
	public void optimize()
	{
		super.optimize();

		this.left.optimize();
		this.right.optimize();

		if (this.extended != null)
			for (EExpression ext : this.extended)
				ext.optimize();
	}

	@Override
	public Expression translate(Task parent)
	{
		System.err.println("translate InfixExpression");
		return null;
	}
}
