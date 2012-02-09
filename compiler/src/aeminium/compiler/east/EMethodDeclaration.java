package aeminium.compiler.east;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItem;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SimpleDataGroup;

public class EMethodDeclaration extends EBodyDeclaration
{
	protected final IMethodBinding binding;

	protected final ArrayList<ESingleVariableDeclaration> parameters;
	protected final EBlock body;
	
	protected final DataGroup returnDataGroup;
	
	public EMethodDeclaration(EAST east, MethodDeclaration original, ETypeDeclaration type)
	{
		super(east, original, type);
		
		this.returnDataGroup = this.getDataGroup().append(new SimpleDataGroup("ret " + original.getName().toString()));

		this.binding = original.resolveBinding();
		
		this.east.addNode(this.binding, this);
		
		this.parameters = new ArrayList<ESingleVariableDeclaration>();
		for (Object param : original.parameters())
			this.parameters.add(ESingleVariableDeclaration.create(this.east, (SingleVariableDeclaration) param, this));
		
		this.body = EBlock.create(this.east, (Block) original.getBody(), this, this);
	}

	/* factory */
	public static EMethodDeclaration create(EAST east, MethodDeclaration method, ETypeDeclaration type)
	{
		return new EMethodDeclaration(east, method, type);
	}

	@Override
	public MethodDeclaration getOriginal()
	{
		return (MethodDeclaration) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		for (ESingleVariableDeclaration param : this.parameters)
			param.checkSignatures();
		
		this.body.checkSignatures();
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		
		for (ESingleVariableDeclaration param : this.parameters)
			sig.addAll(param.getFullSignature());

		sig.addAll(this.body.getFullSignature());
		
		return sig;
	}
	
	public Signature undefer(DataGroup dgRet, DataGroup dgThis, ArrayList<DataGroup> dgsArgs)
	{
		Signature sig = new Signature();
		
		for (int i = 0; i < this.parameters.size(); i++)
			if (this.parameters.get(i).getOriginal().getType().isPrimitiveType())
				sig.addItem(new SignatureItemRead(dgsArgs.get(i)));

		outerLoop: for (SignatureItem item : this.getFullSignature().getItems())
		{
			// don't propagate local variable changes
			if (item.isLocalTo(this.body.getDataGroup()))
				continue;

			SignatureItem _item = item;

			for (int i = 0; i < this.parameters.size(); i++)
			{
				// This item refers a read/write/merge to a parameter passed in by copy (native)
				// it has no implications on outer dependencies and can be cut out
				if (this.parameters.get(i).getOriginal().getType().isPrimitiveType()
					&& item.isLocalTo(this.parameters.get(i).name.getDataGroup()))
					continue outerLoop;

				_item = _item.replace(this.parameters.get(i).name.getDataGroup(), dgsArgs.get(i));
			}
			
			if (!this.isStatic())
				_item = _item.replace(this.type.thisDataGroup, dgThis);

			if (!this.isVoid())
				_item = _item.replace(this.returnDataGroup, dgRet);
			
			sig.addItem(_item);
		}
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		// is this needed?
		for (ESingleVariableDeclaration param : this.parameters)
			param.checkDependencies(stack);
		
		this.body.checkDependencies(stack);
	}
	
	public boolean isVoid()
	{
		return this.getOriginal().getReturnType2().toString().equals("void");
	}
}
