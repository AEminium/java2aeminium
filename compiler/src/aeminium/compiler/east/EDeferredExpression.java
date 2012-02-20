package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.signature.*;

public abstract class EDeferredExpression extends EExpression
{
	protected final IMethodBinding binding;
	
	/* checkSignature */
	protected SignatureItemDeferred deferred;
	
	public EDeferredExpression(EAST east, Expression original, EASTDataNode scope, IMethodBinding binding)
	{
		super(east, original, scope);

		this.binding = binding;
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
		if (this.inlineTask)
			return this.build(out);
		
		out.add(this.task.translate());
		
		AST ast = this.getAST();
		
		/* in task */
		this.task.getExecute().getBody().statements().add(ast.newExpressionStatement(this.build(out)));

		/* parent task */
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_" + this.task.getName()));

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(access);
		ret.setName(ast.newSimpleName("ae_ret"));

		return ret;
	}
	
	public boolean isAeminium()
	{
		return this.getMethod() != null;
	}
}
