package aeminium.compiler.task;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;

import aeminium.compiler.east.EExpression;

public class RegularExpressionSubTask extends ExpressionSubTask
{

	protected RegularExpressionSubTask(EExpression node, String name, Task parent)
	{
		super(node, name, parent);
	}

	public static RegularExpressionSubTask create(EExpression node, String name, Task parent)
	{
		return new RegularExpressionSubTask(node, name, parent);
	}
	
	@Override
	public void fillConstructor(Block body)
	{
		AST ast = this.node.getAST();
		
		if (!this.getNode().isVoid())
			this.addField(this.getNode().getType(), "ae_ret", true);
		this.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), "ae_task", false);
		
		super.fillConstructor(body);
	}
}
