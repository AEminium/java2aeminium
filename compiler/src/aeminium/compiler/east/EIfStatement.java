package aeminium.compiler.east;

import java.util.Set;

import org.eclipse.jdt.core.dom.IfStatement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;

public class EIfStatement extends EStatement
{
	protected final EExpression expr;
	protected final EStatement thenStmt;
	protected final EStatement elseStmt;
	
	public EIfStatement(EAST east, IfStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original, scope, method);

		this.expr = EExpression.create(this.east, original.getExpression(), scope);
		this.thenStmt = EStatement.create(this.east, original.getThenStatement(), scope, method);
		
		if (original.getElseStatement() == null)
			this.elseStmt = null;
		else
			this.elseStmt = EStatement.create(this.east, original.getElseStatement(), scope, method);
	}

	/* factory */
	public static EIfStatement create(EAST east, IfStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		return new EIfStatement(east, original, scope, method);
	}

	@Override
	public IfStatement getOriginal()
	{
		return (IfStatement) this.original;
	}

	@Override
	public void checkSignatures()
	{
		this.expr.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
		
		this.thenStmt.checkSignatures();
		
		if (this.elseStmt != null)
			this.elseStmt.checkSignatures();
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();

		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());
		sig.addAll(this.thenStmt.getFullSignature());
		
		if (this.elseStmt != null)
			sig.addAll(this.elseStmt.getFullSignature());

		return sig;
	}
	
	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.expr.checkDependencies(stack);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
		{
			if (node.equals(this.expr))
				this.strongDependencies.add(node);
			else
				this.weakDependencies.add(node);
		}
		
		this.thenStmt.checkDependencies(stack);
		
		if (this.elseStmt != null)
			this.elseStmt.checkDependencies(stack);
	}
}
