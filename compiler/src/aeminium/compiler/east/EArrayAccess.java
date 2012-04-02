package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.task.Task;

public class EArrayAccess extends EExpression
{
	protected final EExpression array;
	protected final EExpression index;
	
	public EArrayAccess(EAST east, ArrayAccess original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.array = EExpression.create(east, original.getArray(), scope);
		this.index = EExpression.create(east, original.getIndex(), scope);
	}

	public static EArrayAccess create(EAST east, ArrayAccess original, EASTDataNode scope)
	{
		return new EArrayAccess(east, original, scope);
	}
	
	@Override
	public ArrayAccess getOriginal()
	{
		return (ArrayAccess) this.original;
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.array.getDataGroup();
	}

	@Override
	public void checkSignatures()
	{
		this.array.checkSignatures();
		this.index.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.dependency, this.array.getDataGroup()));
		this.signature.addItem(new SignatureItemRead(this.dependency, this.index.getDataGroup()));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		sig.addAll(this.index.getFullSignature());
		sig.addAll(this.array.getFullSignature());
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.array.checkDependencies(stack);
		this.dependency.addStrong(this.array.dependency);
		
		this.index.checkDependencies(stack);
		this.dependency.addStrong(this.index.dependency);
		
		Set<Dependency> deps = stack.getDependencies(this.signature);
		this.dependency.addWeak(deps);
	}

	@Override
	public int optimize()
	{
		int sum = 0;
		
		sum += this.array.optimize();
		sum += this.index.optimize();
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "arrayidx");
		
		this.array.preTranslate(this.task);
		this.index.preTranslate(this.task);
	}
	
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();
		
		ArrayAccess access = ast.newArrayAccess();
		access.setArray(this.array.translate(out));
		access.setIndex(this.index.translate(out));

		return access;
	}
}
