package aeminium.compiler.task;

import aeminium.compiler.east.EExpression;
import aeminium.compiler.east.EMethodInvocation;

public abstract class ExpressionSubTask extends SubTask
{
	protected ExpressionSubTask(EExpression node, String name, Task parent)
	{
		super(node, name, parent);
	}

	public static ExpressionSubTask create(EExpression node, String name, Task parent)
	{
		if (node instanceof EMethodInvocation)
			return CallerExpressionSubTask.create(node, name, parent);
		
		return RegularExpressionSubTask.create(node, name, parent);
	}

	@Override
	public EExpression getNode()
	{
		return (EExpression) this.node;
	}
}