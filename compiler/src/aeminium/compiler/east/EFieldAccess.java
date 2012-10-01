package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.task.Task;

public class EFieldAccess extends EExpression
{
	protected final EExpression expr;
	protected final ESimpleNameExpression name;
	
	public EFieldAccess(EAST east, FieldAccess original, EASTDataNode scope, EFieldAccess base)
	{
		super(east, original, scope, base);
		
		this.expr = EExpression.create(east, original.getExpression(), scope, base == null ? null : base.expr);
		this.name = ESimpleNameExpression.create(east, original.getName(), scope, base == null ? null : base.name);
	}

	public static EFieldAccess create(EAST east, FieldAccess original, EASTDataNode scope, EFieldAccess base)
	{
		return new EFieldAccess(east, original, scope, base);
	}
	
	@Override
	public FieldAccess getOriginal()
	{
		return (FieldAccess) this.original;
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.name.getDataGroup();
	}

	@Override
	public void checkSignatures()
	{
		this.expr.checkSignatures();
		this.name.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());
		sig.addAll(this.name.getFullSignature());
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.expr.checkDependencies(stack);
		this.addStrongDependency(this.expr);
		
		this.name.checkDependencies(stack);
		this.addStrongDependency(this.name);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
	}

	@Override
	public int optimize()
	{
		int sum = 0;
		
		sum += this.expr.optimize();
		sum += this.name.optimize();
		
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "field", this.base == null ? null : this.base.task);
		
		this.expr.preTranslate(this.task);
		this.name.preTranslate(this.task);
	}
	
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();
		
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(this.expr.translate(out));
		access.setName((SimpleName) ASTNode.copySubtree(ast, this.name.getOriginal()));

		return access;
	}
}
