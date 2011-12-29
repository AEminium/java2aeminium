package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Task;
import aeminium.compiler.datagroup.SignatureItemMerge;
import aeminium.compiler.datagroup.SignatureItemRead;
import aeminium.compiler.datagroup.SignatureItemWrite;

public class EAssignment extends EExpression
{
	private final Assignment origin;
	private final EExpression left;
	private final EExpression right;

	ITypeBinding type;

	EAssignment(EAST east, Assignment origin)
	{
		super(east);

		this.origin = origin;

		this.left = this.east.extend(origin.getLeftHandSide());
		this.right = this.east.extend(origin.getRightHandSide());
		this.type = this.origin.resolveTypeBinding();
	}

	@Override
	public void analyse()
	{
		super.analyse();

		this.left.analyse();
		this.right.analyse();
		
		this.datagroup = this.left.getDataGroup();
		
		this.signature.addFrom(this.left.getSignature());
		this.signature.addFrom(this.right.getSignature());

		this.signature.add(new SignatureItemWrite(this.left.getDataGroup()));
		this.signature.add(new SignatureItemRead(this.right.getDataGroup()));
		this.signature.add(new SignatureItemMerge(this.left.getDataGroup(), this.right.getDataGroup()));
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		sum += this.left.optimize();
		sum += this.right.optimize();
		
		return sum;
	}
	
	public void preTranslate(Task parent)
	{
		if (this.isRoot())
			this.task = parent.newStrongDependency("assign");
		else
			this.task = parent;
		
		this.left.preTranslate(this.task);
		this.right.preTranslate(this.task);
	}
	
	@Override
	public Expression translate(List<CompilationUnit> cus)
	{
		/*
			TODO
			order of translation is important for dependencies 
			c = c+1;
			must translate c+1 before c
		*/
		System.err.println("TODO: EAssignment translate");
		return null;
	}
}
