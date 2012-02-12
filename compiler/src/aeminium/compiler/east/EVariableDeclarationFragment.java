package aeminium.compiler.east;

import java.util.Set;

import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemMerge;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SignatureItemWrite;

public class EVariableDeclarationFragment extends EASTExecutableNode implements EASTDataNode
{
	protected final EASTDataNode scope;
	protected final Type dataType;
	protected final DataGroup datagroup;
	
	protected final ESimpleNameDeclaration name;
	protected final EExpression expr;
	
	public EVariableDeclarationFragment(EAST east, VariableDeclarationFragment original, EASTDataNode scope, Type dataType)
	{
		super(east, original);

		this.scope = scope;
		this.dataType = dataType;
		this.datagroup = scope.getDataGroup();
	
		this.name = ESimpleNameDeclaration.create(this.east, original.getName(), this);
		this.expr = EExpression.create(this.east, original.getInitializer(), this.scope);
	}

	@Override
	public VariableDeclarationFragment getOriginal()
	{
		return (VariableDeclarationFragment) this.original;
	}

	public static EVariableDeclarationFragment create(EAST east, VariableDeclarationFragment frag, EASTDataNode scope, Type dataType)
	{
		return new EVariableDeclarationFragment(east, frag, scope, dataType);
	}

	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public void checkSignatures()
	{
		this.name.checkSignatures();
		
		if (this.expr != null)
		{
			this.expr.checkSignatures();
			
			this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
			this.signature.addItem(new SignatureItemWrite(this.name.getDataGroup()));
			this.signature.addItem(new SignatureItemMerge(this.name.getDataGroup(), this.expr.getDataGroup()));
		}
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
		if (this.expr != null)
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
	
	@Override
	public int optimize()
	{
		int sum = super.optimize();

		if (this.expr != null)
			sum += this.expr.optimize();

		return sum;
	}
}
