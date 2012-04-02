package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemMerge;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SignatureItemWrite;
import aeminium.compiler.task.Task;

public class EReturnStatement extends EStatement
{
	protected final EExpression expr;
	
	public EReturnStatement(EAST east, ReturnStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original, scope, method);

		if (original.getExpression() == null)
			this.expr = null;
		else
			this.expr = EExpression.create(this.east, original.getExpression(), scope);
	}

	/* factory */
	public static EReturnStatement create(EAST east, ReturnStatement stmt, EASTDataNode scope, EMethodDeclaration method)
	{
		return new EReturnStatement(east, stmt, scope, method);
	}
	
	@Override
	public ReturnStatement getOriginal()
	{
		return (ReturnStatement) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		if (this.expr != null)
		{
			this.expr.checkSignatures();
			
			this.signature.addItem(new SignatureItemRead(this.dependency, this.expr.getDataGroup()));
			this.signature.addItem(new SignatureItemWrite(this.dependency, this.method.returnDataGroup));
			this.signature.addItem(new SignatureItemMerge(this.method.returnDataGroup, this.expr.getDataGroup()));
		}
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		
		if (this.expr != null)
			sig.addAll(this.expr.getFullSignature());

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		if (this.expr != null)
		{
			this.expr.checkDependencies(stack);
			this.dependency.addStrong(this.expr.dependency);
		}

		Set<Dependency> deps = stack.getDependencies(this.signature);
		this.dependency.addWeak(deps);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;

		if (this.expr != null)
			sum += this.expr.optimize();

		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "ret");
		
		if (this.expr != null)
			this.expr.preTranslate(this.task);
	}

	@Override
	public List<Statement> build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(this.task.getPathToRoot());
		ret.setName(ast.newSimpleName("ae_ret"));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(ret);
		assign.setRightHandSide(this.expr.translate(out)); 

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}
}
