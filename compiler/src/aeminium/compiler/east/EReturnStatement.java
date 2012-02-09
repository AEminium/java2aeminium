package aeminium.compiler.east;

import java.util.Set;

import org.eclipse.jdt.core.dom.ReturnStatement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemMerge;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SignatureItemWrite;

public class EReturnStatement extends EStatement
{
	protected final EExpression expr;
	
	public EReturnStatement(EAST east, ReturnStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original, scope, method);

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
		this.expr.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
		this.signature.addItem(new SignatureItemWrite(this.method.returnDataGroup));
		this.signature.addItem(new SignatureItemMerge(this.method.returnDataGroup, this.expr.getDataGroup()));
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
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		for (EASTExecutableNode node : deps)
		{
			if (node.equals(this.expr))
				this.strongDependencies.add(node);
			else
				this.weakDependencies.add(node);
		}
	}
}
