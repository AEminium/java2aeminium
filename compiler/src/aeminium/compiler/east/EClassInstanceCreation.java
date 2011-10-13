package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EClassInstanceCreation extends EExpression
{
	ClassInstanceCreation origin;
	List<EExpression> args;
	IMethodBinding binding;

	EClassInstanceCreation(EAST east, ClassInstanceCreation origin)
	{
		super(east);

		this.origin = origin;
		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
			this.args.add(this.east.extend((Expression) arg));
	}

	@Override
	public void optimize()
	{
		super.optimize();

		for (EExpression arg : this.args)
			arg.optimize();

		// TODO check if the constructor being used has @AEminium
		// if not, this call must be serialized, or at least run a serial version in a task that is paralell.
 	}

	@Override
	public Expression translate(Task parent)
	{
		System.err.println("TODO: ClassInstanceCreation translate");
		return null;
	}
}
