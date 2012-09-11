package aeminium.compiler.task;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import aeminium.compiler.east.EWhileStatement;

public class WhileSubTask extends StatementSubTask
{
	@SuppressWarnings("unchecked")
	protected WhileSubTask(EWhileStatement node, String name, Task parent)
	{
		super(node, name, parent);
		
		AST ast = node.getAST();

		MethodDeclaration recursiveConstructor = ast.newMethodDeclaration();
		this.constructors.add(recursiveConstructor);
		this.decl.bodyDeclarations().add(recursiveConstructor);
	}

	public static WhileSubTask create(EWhileStatement node, String name, Task parent)
	{
		return new WhileSubTask(node, name, parent);
	}
	
	@Override
	public EWhileStatement getNode()
	{
		return (EWhileStatement) this.node;
	}
	
	@Override
	public CompilationUnit translate()
	{
		ArrayList<Task> deps = new ArrayList<Task>();
		deps.add(this.getNode().getBody().getTask());
		
		this.fillConstructor(this.constructors.get(0), this.node.getAST().newBlock(), false, null);
		this.fillConstructor(this.constructors.get(1), this.node.getAST().newBlock(), true, deps);
		this.fillExecute();
		
		return this.cu;
	}
}
