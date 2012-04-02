package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public abstract class ENameExpression extends EExpression
{
	protected final IBinding binding;
	
	public ENameExpression(EAST east, Name original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.binding = original.resolveBinding();
	}
	
	/* factory */
	public static ENameExpression create(EAST east, Name original, EASTDataNode scope)
	{
		if (original instanceof SimpleName)
			return ESimpleNameExpression.create(east, (SimpleName) original, scope);
		
		if (original instanceof QualifiedName)
			return EQualifiedNameExpression.create(east, (QualifiedName) original, scope);
		
		System.err.println("FIXME: ENameExpression.create()");
		return null;
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		ESimpleNameDeclaration node = ((ESimpleNameDeclaration) this.east.getNode(this.binding));
		
		if (node == null)
		{
			System.out.println("WARNING: using external datagroup for: " + this.binding.getKey());
			return this.getEAST().getExternalDataGroup();
		}
		
		return node.getDataGroup();
	}

	@Override
	public Name getOriginal()
	{
		return (Name) this.original;
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

		ESimpleNameDeclaration node = (ESimpleNameDeclaration) this.east.getNode(this.binding);

		if (node == null)
			return (Expression) ASTNode.copySubtree(ast, this.original);

		FieldAccess field = ast.newFieldAccess();

		field.setExpression(this.dependency.getPathTo(node.getDeclaringTask().getNode().dependency));
		field.setName((SimpleName) ASTNode.copySubtree(ast, this.getOriginal()));

		return field;
	}
}
