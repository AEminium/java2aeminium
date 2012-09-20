package aeminium.compiler.task;

import org.eclipse.jdt.core.dom.CompilationUnit;

import aeminium.compiler.east.EWhileStatement;

public class WhileSubTask extends StatementSubTask
{
	protected WhileSubTask(EWhileStatement node, String name, Task parent, Task base)
	{
		super(node, name, parent, base);
	}

	public static WhileSubTask create(EWhileStatement node, String name, Task parent, Task base)
	{
		return new WhileSubTask(node, name, parent, base);
	}
	
	@Override
	public EWhileStatement getNode()
	{
		return (EWhileStatement) this.node;
	}
	
	@Override
	public CompilationUnit translate()
	{
		this.fillConstructor(this.constructors.get(0), this.node.getAST().newBlock(), true);
		this.fillExecute();
		
		return this.cu;
	}
}
