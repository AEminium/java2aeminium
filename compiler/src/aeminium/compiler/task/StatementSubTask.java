package aeminium.compiler.task;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import aeminium.compiler.east.EStatement;
public class StatementSubTask extends SubTask
{
	protected StatementSubTask(EStatement node, String name, Task parent, Task base)
	{
		super(node, name, parent, base);
	}

	public static StatementSubTask create(EStatement node, String name, Task parent, Task base)
	{
		return new StatementSubTask(node, name, parent, base);
	}

	@Override
	public EStatement getNode()
	{
		return (EStatement) this.node;
	}
	
	@Override
	public void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive)
	{
		AST ast = this.node.getAST();
		
		this.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), "ae_task", false);
		super.fillConstructor(constructor, body, recursive);
	}
}