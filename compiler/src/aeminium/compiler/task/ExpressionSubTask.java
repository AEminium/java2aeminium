package aeminium.compiler.task;

import aeminium.compiler.east.EClassInstanceCreation;
import aeminium.compiler.east.EExpression;
import aeminium.compiler.east.EMethodInvocation;

public abstract class ExpressionSubTask extends SubTask
{
	protected ExpressionSubTask(EExpression node, String name, Task parent, Task base)
	{
		super(node, name, parent, base);
	}

	public static ExpressionSubTask create(EExpression node, String name, Task parent, Task base)
	{
		if ((node instanceof EMethodInvocation && ((EMethodInvocation) node).isAeminium()) ||
			(node instanceof EClassInstanceCreation && ((EClassInstanceCreation) node).isAeminium()))
			return CallerExpressionSubTask.create(node, name, parent, base);
		
		return RegularExpressionSubTask.create(node, name, parent, base);
	}

	@Override
	public EExpression getNode()
	{
		return (EExpression) this.node;
	}
}