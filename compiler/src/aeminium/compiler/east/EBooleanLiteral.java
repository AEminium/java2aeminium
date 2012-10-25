package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class EBooleanLiteral extends EExpression
{
	protected final DataGroup datagroup;
	
	public EBooleanLiteral(EAST east, BooleanLiteral original, EASTDataNode scope, EASTExecutableNode parent, EBooleanLiteral base)
	{
		super(east, original, scope, parent, base);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("literal"));
	}

	/* factory */
	public static EBooleanLiteral create(EAST east, BooleanLiteral original, EASTDataNode scope, EASTExecutableNode parent, EBooleanLiteral base)
	{
		return new EBooleanLiteral(east, original, scope, parent, base);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public BooleanLiteral getOriginal()
	{
		return (BooleanLiteral) this.original;
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
		
		return (BooleanLiteral) ASTNode.copySubtree(ast, this.getOriginal());
	}

}
