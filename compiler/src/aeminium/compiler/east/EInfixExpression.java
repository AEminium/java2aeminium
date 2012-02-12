package aeminium.compiler.east;

import java.util.Set;

import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;

public class EInfixExpression extends EExpression
{
	protected final EExpression left;
	protected final EExpression right;
	protected final Operator operator;
	
	protected final DataGroup datagroup;
	
	public EInfixExpression(EAST east, InfixExpression original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.operator = this.getOriginal().getOperator();
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("infix " + this.operator));

		this.left = EExpression.create(this.east, original.getLeftOperand(), scope);
		this.right = EExpression.create(this.east, original.getRightOperand(), scope);
	}

	/* factory */
	public static EInfixExpression create(EAST east, InfixExpression original, EASTDataNode scope)
	{
		return new EInfixExpression(east, original, scope);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public InfixExpression getOriginal()
	{
		return (InfixExpression) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		this.left.checkSignatures();
		this.right.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.left.getDataGroup()));
		this.signature.addItem(new SignatureItemRead(this.right.getDataGroup()));
		this.signature.addItem(new SignatureItemWrite(this.datagroup));
		
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
	
		sig.addAll(this.signature);
		sig.addAll(this.left.getFullSignature());
		sig.addAll(this.right.getFullSignature());
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.left.checkDependencies(stack);
		this.right.checkDependencies(stack);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
		{
			if (this.left.equals(node) || this.right.equals(node))
				this.strongDependencies.add(node);
			else
				this.weakDependencies.add(node);
		}
	}
	
	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		sum += this.left.optimize();
		sum += this.right.optimize();
		
		return sum;
	}
}
