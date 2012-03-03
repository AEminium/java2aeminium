package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class EExpressionStatement extends EStatement
{
	protected final EExpression expr;
	
	public EExpressionStatement(EAST east, ExpressionStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original, scope, method);

		this.expr = EExpression.create(this.east, original.getExpression(), scope);
	}

	/* factory */
	public static EExpressionStatement create(EAST east, ExpressionStatement stmt, EASTDataNode scope, EMethodDeclaration method)
	{
		return new EExpressionStatement(east, stmt, scope, method);
	}
	
	@Override
	public ReturnStatement getOriginal()
	{
		return (ReturnStatement) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		this.expr.checkSignatures();
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.expr.checkDependencies(stack);
		this.strongDependencies.add(this.expr);

		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		for (EASTExecutableNode node : deps)
			if (!node.equals(this.expr))
				this.weakDependencies.add(node);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;

		sum += this.expr.optimize();
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "exprstmt");
		
		this.expr.preTranslate(this.task);
	}

	@Override
	public List<Statement> build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		Expression expr = this.expr.translate(out);
		
		if (expr instanceof FieldAccess)
			return new ArrayList<Statement>();
		
		ExpressionStatement exprstmt = ast.newExpressionStatement(expr);
		return Arrays.asList((Statement) exprstmt);
	}
}
