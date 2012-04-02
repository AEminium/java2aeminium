package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.StringLiteral;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class EStringLiteral extends EExpression
{
	protected final DataGroup datagroup;
	
	public EStringLiteral(EAST east, StringLiteral original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("literal"));
		this.simple = true;
	}

	/* factory */
	public static EStringLiteral create(EAST east, StringLiteral original, EASTDataNode scope)
	{
		return new EStringLiteral(east, original, scope);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public StringLiteral getOriginal()
	{
		return (StringLiteral) this.original;
	}

	@Override
	public void checkSignatures()
	{
		// Nothing
	}

	@Override
	public Signature getFullSignature()
	{
		// Nothing
		return new Signature();
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		// Nothing
	}

	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "literal");
	}

	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();
		
		return (StringLiteral) ASTNode.copySubtree(ast, this.getOriginal());
	}

}
