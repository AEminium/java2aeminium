package aeminium.compiler.task;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import aeminium.compiler.east.EStatement;

public class StatementSubTask extends SubTask
{
	protected StatementSubTask(EStatement node, String name, Task parent)
	{
		super(node, name, parent);
	}

	public static StatementSubTask create(EStatement node, String name, Task parent)
	{
		return new StatementSubTask(node, name, parent);
	}

	@Override
	public EStatement getNode()
	{
		return (EStatement) this.node;
	}
	
	@Override
	public void fillConstructor(Block body)
	{
		AST ast = this.node.getAST();
		
		this.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), "ae_task", false);

		super.fillConstructor(body);
	}
}