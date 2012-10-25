package aeminium.compiler.east;

import java.util.Set;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class ESingleVariableDeclaration extends EASTExecutableNode implements EASTDeclaringNode
{
	protected final EASTDataNode scope;
	protected final DataGroup datagroup;
	
	protected final ESimpleNameDeclaration name;
	protected final EExpression expr;
	
	public ESingleVariableDeclaration(EAST east, SingleVariableDeclaration original, EASTDataNode scope, EASTExecutableNode parent, ESingleVariableDeclaration base)
	{
		super(east, original, parent, base);

		this.scope = scope;
		
		/* FIXME: maybe this should append something to the datagroup */
		this.datagroup = scope.getDataGroup();
		
		this.name = ESimpleNameDeclaration.create(this.east, original.getName(), this, this, base == null ? null : base.name);
		
		if (original.getInitializer() == null)
			this.expr = null;
		else
			this.expr = EExpression.create(this.east, original.getInitializer(), this, this, base == null ? null : base.expr);
	}

	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public ETypeDeclaration getTypeDeclaration()
	{
		return this.scope.getTypeDeclaration();
	}
	
	@Override
	public SingleVariableDeclaration getOriginal()
	{
		return (SingleVariableDeclaration) this.original;
	}

	public static ESingleVariableDeclaration create(EAST east, SingleVariableDeclaration param, EASTDataNode scope, EASTExecutableNode parent, ESingleVariableDeclaration base)
	{
		return new ESingleVariableDeclaration(east, param, scope, parent, base);
	}

	@Override
	public void checkSignatures()
	{
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
			this.addStrongDependency(this.expr);
		}

		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
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
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "decl", this.base == null ?  null : this.base.task);
		
		if (this.expr != null)
			this.expr.preTranslate(this.task);
	}
	
	@Override
	public EASTDataNode getScope()
	{
		return this.getScope();
	}
	
	@Override
	public boolean isSimpleTask()
	{
		return EASTExecutableNode.HARD_AGGREGATION;
	}
}