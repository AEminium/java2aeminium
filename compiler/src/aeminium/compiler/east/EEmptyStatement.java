package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class EEmptyStatement extends EStatement
{	
	public EEmptyStatement(EAST east, EmptyStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original, scope, method);
	}

	/* factory */
	public static EEmptyStatement create(EAST east, EmptyStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		return new EEmptyStatement(east, original, scope, method);
	}

	@Override
	public EmptyStatement getOriginal()
	{
		return (EmptyStatement) this.original;
	}



	@Override
	public void checkSignatures()
	{	
	}

	@Override
	public Signature getFullSignature()
	{
		return new Signature();
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{	
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "empty");
	}
	
	@Override
	public List<Statement> build(List<CompilationUnit> out)
	{		
		AST ast = this.getAST();

		EmptyStatement stmt = ast.newEmptyStatement();
		return Arrays.asList((Statement)stmt);
	}
}
