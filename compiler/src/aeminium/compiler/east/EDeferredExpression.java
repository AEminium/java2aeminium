package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.signature.*;

public abstract class EDeferredExpression extends EExpression
{
	protected final IMethodBinding binding;
	
	/* checkSignature */
	protected SignatureItemDeferred deferred;
	
	public EDeferredExpression(EAST east, Expression original, EASTDataNode scope, IMethodBinding binding, EASTExecutableNode parent, EDeferredExpression base)
	{
		super(east, original, scope, parent, base);

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
	
	@Override
	public int inline(EASTExecutableNode inlineTo)
	{
		if (!this.isSequential())
			return 0;
		
		return super.inline(inlineTo);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression translate(List<CompilationUnit> out)
	{
		if (this.inlineTask)
		{
			Expression expr = this.build(out);
			this.postTranslate(this.task);
			return expr;
		}
		
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
			this.task.getExecute().getBody().statements().add(this.buildStmt(out));

		/* parent task */
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_" + this.task.getFieldName()));

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(access);
		ret.setName(ast.newSimpleName("ae_ret"));

		this.postTranslate(this.task);
		
		return ret;
	}
	
	public boolean isAeminium()
	{
		return this.getMethod() != null;
	}
	

	public abstract Statement buildStmt(List<CompilationUnit> out);
}
