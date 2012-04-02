package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.Dependency;
import aeminium.compiler.RuntimeDependency;
import aeminium.compiler.signature.*;

public abstract class EDeferredExpression extends EExpression
{
	protected final IMethodBinding binding;
	protected final Dependency deferredDependency;
	
	/* checkSignature */
	protected SignatureItemDeferred deferred;
	
	public EDeferredExpression(EAST east, Expression original, EASTDataNode scope, IMethodBinding binding)
	{
		super(east, original, scope);

		this.binding = binding;
		this.deferredDependency = new RuntimeDependency(this, "deferred");
	}

	public EMethodDeclaration getMethod()
	{
		return (EMethodDeclaration) this.east.getNode(this.binding);
	}
	
	public List<ModifierKeyword> getModifiers()
	{
		return EModifierKeyword.fromFlags(this.binding.getModifiers());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression translate(List<CompilationUnit> out)
	{
		if (this.inline)
			return this.build(out);
		
		out.add(this.task.translate());
		
		AST ast = this.getAST();
		
		/* in task */
		if (!this.isAeminium() && !this.isVoid())
		{
			FieldAccess this_ret = ast.newFieldAccess();
			this_ret.setExpression(ast.newThisExpression());
			this_ret.setName(ast.newSimpleName("ae_ret"));
	
			Assignment assign = ast.newAssignment();
			assign.setLeftHandSide(this_ret);
			assign.setRightHandSide(this.build(out));

			this.task.getExecute().getBody().statements().add(ast.newExpressionStatement(assign));
		} else
			this.task.getExecute().getBody().statements().add(ast.newExpressionStatement(this.build(out)));

		/* parent task */
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_" + this.task.getName()));

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(this.task.undefer(access));
		ret.setName(ast.newSimpleName("ae_ret"));

		return ret;
	}
	
	public boolean isAeminium()
	{
		return this.getMethod() != null;
	}
}
