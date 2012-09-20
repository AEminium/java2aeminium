package aeminium.compiler.task;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import aeminium.compiler.east.EExpression;

public class RegularExpressionSubTask extends ExpressionSubTask
{

	protected RegularExpressionSubTask(EExpression node, String name, Task parent, Task base)
	{
		super(node, name, parent, base);
	}

	public static RegularExpressionSubTask create(EExpression node, String name, Task parent, Task base)
	{
		return new RegularExpressionSubTask(node, name, parent, base);
	}
	
	@Override
	public void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive)
	{
		AST ast = this.node.getAST();
		
		if (!this.getNode().isVoid())
			this.addField(this.getNode().getType(), "ae_ret", true);

		this.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), "ae_task", false);
		
		super.fillConstructor(constructor, body, recursive);
	}
}
