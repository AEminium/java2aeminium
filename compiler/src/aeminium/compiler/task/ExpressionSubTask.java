package aeminium.compiler.task;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import aeminium.compiler.east.EClassInstanceCreation;
import aeminium.compiler.east.EExpression;
import aeminium.compiler.east.EMethodInvocation;

public class ExpressionSubTask extends SubTask
{
	protected ExpressionSubTask(EExpression node, String name, Task parent)
	{
		super(node, name, parent);
	}

	public static ExpressionSubTask create(EExpression node, String name, Task parent)
	{
		if ((node instanceof EMethodInvocation && ((EMethodInvocation) node).isAeminium()) ||
			(node instanceof EClassInstanceCreation && ((EClassInstanceCreation) node).isAeminium()))
			return CallerExpressionSubTask.create(node, name, parent);
		
		return new ExpressionSubTask(node, name, parent);
	}

	@Override
	public EExpression getNode()
	{
		return (EExpression) this.node;
	}
	
	@Override
	public void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive, ArrayList<Task> overrideTasks)
	{
		AST ast = this.node.getAST();

		if (!this.getNode().isVoid())
			this.addField(this.getNode().getType(), "ae_ret", true);

		this.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), "ae_task", false);
		
		super.fillConstructor(constructor, body, recursive, overrideTasks);
	}
}