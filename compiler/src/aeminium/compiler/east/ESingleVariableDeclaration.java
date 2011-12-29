package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.datagroup.LocalDataGroup;
import aeminium.compiler.datagroup.SignatureItemMerge;
import aeminium.compiler.datagroup.SignatureItemRead;
import aeminium.compiler.datagroup.SignatureItemWrite;

public class ESingleVariableDeclaration extends EASTDependentNode
{
	private final SingleVariableDeclaration origin;
	private final EExpression expr;
	private final ESimpleName name;

	IBinding binding;

	ESingleVariableDeclaration(EAST east, SingleVariableDeclaration origin)
	{
		super(east);
		this.origin = origin;
	
		this.name = this.east.extend(origin.getName());

		if (origin.getInitializer() != null)
			this.expr = this.east.extend(origin.getInitializer());
		else
			this.expr = null;
	}

	@Override
	public void analyse()
	{
		this.binding = this.origin.resolveBinding();
		this.east.putNode(this.east.resolveName(this.binding), this.name);

		super.analyse();
	
		this.name.analyse();
		this.name.setDataGroup(new LocalDataGroup(this));
		
		if (this.expr != null)
		{
			this.expr.analyse();
			
			this.signature.addFrom(this.expr.getSignature());

			this.signature.add(new SignatureItemRead(this.expr.getDataGroup()));
			this.signature.add(new SignatureItemWrite(this.name.getDataGroup()));
			this.signature.add(new SignatureItemMerge(this.name.getDataGroup(), this.expr.getDataGroup()));
		}
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
	
		sum += this.name.optimize();

		if (this.expr != null)
			sum += this.expr.optimize();

		return sum;
	}

	public void preTranslate(EMethodDeclaration owner)
	{
		this.name.preTranslate(owner.getTask());		
	}
	
	public void translate(List<CompilationUnit> cus)
	{
		// TODO
		System.out.println("TODO: SingleVariableDecl translate (initializer expr)");
	}
}
