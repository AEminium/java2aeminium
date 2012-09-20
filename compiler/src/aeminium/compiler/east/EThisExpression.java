package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class EThisExpression extends EExpression
{
	public EThisExpression(EAST east, ThisExpression original, EASTDataNode scope, EThisExpression base)
	{
		super(east, original, scope, base);
	}
	
	/* factory */
	public static EThisExpression create(EAST east, ThisExpression original, EASTDataNode scope, EThisExpression base)
	{
		return new EThisExpression(east, original, scope, base);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.getTypeDeclaration().thisDataGroup;
	}

	@Override
	public ThisExpression getOriginal()
	{
		return (ThisExpression) this.original;
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

		FieldAccess field = ast.newFieldAccess();

		field.setExpression(this.task.getPathToRoot());
		field.setName(ast.newSimpleName("ae_this"));

		return field;
	}
}
