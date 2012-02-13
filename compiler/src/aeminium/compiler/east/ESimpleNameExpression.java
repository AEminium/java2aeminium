package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class ESimpleNameExpression extends EExpression
{
	protected final IBinding binding;
	
	public ESimpleNameExpression(EAST east, SimpleName original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.binding = original.resolveBinding();
	}

	@Override
	public DataGroup getDataGroup()
	{
		return ((ESimpleNameDeclaration) this.east.getNode(this.binding)).getDataGroup();
	}

	@Override
	public SimpleName getOriginal()
	{
		return (SimpleName) this.original;
	}

	/* factory */
	public static ESimpleNameExpression create(EAST east, SimpleName original, EASTDataNode scope)
	{
		return new ESimpleNameExpression(east, original, scope);
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
			this.task = parent.newSubTask(this, "literal");
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

		ESimpleNameDeclaration node = (ESimpleNameDeclaration) this.east.getNode(this.binding);

		FieldAccess field = ast.newFieldAccess();

		field.setExpression(this.task.getPathToTask(node.getDeclaringTask()));
		field.setName((SimpleName) ASTNode.copySubtree(ast, this.getOriginal()));

		return field;
	}
}
