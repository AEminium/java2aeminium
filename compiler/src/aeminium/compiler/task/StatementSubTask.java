package aeminium.compiler.task;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import aeminium.compiler.east.EStatement;
import aeminium.compiler.east.EWhileStatement;

public class StatementSubTask extends SubTask
{
	protected StatementSubTask(EStatement node, String name, Task parent)
	{
		super(node, name, parent);
	}

	public static StatementSubTask create(EStatement node, String name, Task parent)
	{
		if (node instanceof EWhileStatement)
			return WhileSubTask.create((EWhileStatement) node, name, parent);
		
		return new StatementSubTask(node, name, parent);
	}

	@Override
	public EStatement getNode()
	{
		return (EStatement) this.node;
	}
	
	@Override
	public void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive, ArrayList<Task> overrideTasks)
	{
		AST ast = this.node.getAST();
		
		if (!recursive)
			this.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), "ae_task", false);

		super.fillConstructor(constructor, body, recursive, overrideTasks);
	}
}