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
	
	public EStringLiteral(EAST east, StringLiteral original, EASTDataNode scope, EStringLiteral base)
	{
		super(east, original, scope, base);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("literal"));
	}

	/* factory */
	public static EStringLiteral create(EAST east, StringLiteral original, EASTDataNode scope, EStringLiteral base)
	{
		return new EStringLiteral(east, original, scope, base);
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
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "literal", this.base == null ? null : this.base.task);
	}
	
	@Override
	public boolean isSimpleTask()
	{
		return true;
	}

	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();
		
		return (StringLiteral) ASTNode.copySubtree(ast, this.getOriginal());
	}

}
