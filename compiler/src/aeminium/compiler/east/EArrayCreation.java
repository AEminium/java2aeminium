package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemMerge;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SignatureItemWrite;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class EArrayCreation extends EExpression
{
	protected final DataGroup datagroup;
	protected final ArrayList<EExpression> dimensions;
	protected final EArrayInitializer initializer;
	protected final ArrayType type;
	
	public EArrayCreation(EAST east, ArrayCreation original, EASTDataNode scope, EArrayCreation base)
	{
		super(east, original, scope, base);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("array"));
		
		this.type = original.getType();
		
		this.dimensions = new ArrayList<EExpression>();
		for (int i = 0; i < original.dimensions().size(); i++)
		{
			this.dimensions.add
			(
				EExpression.create
				(
					east,
					(Expression) original.dimensions().get(i),
					scope,
					base == null ? null : base.dimensions.get(i)
				)
			);
		}
		
		if (original.getInitializer() == null)
			this.initializer = null;
		else
		{
			this.initializer = EArrayInitializer.create
			(
				east,
				(ArrayInitializer) original.getInitializer(),
				scope,
				base == null ? null : base.initializer
			);
		}
	}

	/* factory */
	public static EArrayCreation create(EAST east, ArrayCreation original, EASTDataNode scope, EArrayCreation base)
	{
		return new EArrayCreation(east, original, scope, base);
	}
	
	@Override
	public ArrayCreation getOriginal()
	{
		return (ArrayCreation) this.original;
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public void checkSignatures()
	{
		for (EExpression dim : this.dimensions)
			dim.checkSignatures();
		
		if (this.initializer != null)
		{
			this.initializer.checkSignatures();

			this.signature.addItem(new SignatureItemRead(this.initializer.getDataGroup()));
			this.signature.addItem(new SignatureItemMerge(this.datagroup, this.initializer.getDataGroup()));
		}
		
		this.signature.addItem(new SignatureItemWrite(this.datagroup));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);

		for (EExpression dim : this.dimensions)
			sig.addAll(dim.getFullSignature());
		
		if (this.initializer != null)
			sig.addAll(this.initializer.getFullSignature());
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		for (EExpression dim : this.dimensions)
		{
			dim.checkDependencies(stack);
			this.addStrongDependency(dim);
		}
		
		if (this.initializer != null)
		{
			this.initializer.checkDependencies(stack);
			this.addStrongDependency(this.initializer);
		}
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;
		
		for (EExpression dim : this.dimensions)
			sum += dim.optimize();
		
		if (this.initializer != null)
		{
			sum += this.initializer.optimize();
			sum += this.initializer.inline(this);
		}
		
		sum += super.optimize();

		return sum;
	}
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "array", this.base == null ? null : this.base.task);

		for (int i = 0; i < this.dimensions.size(); i++)
			this.dimensions.get(i).preTranslate(this.task);
		
		if (this.initializer != null)
			this.initializer.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();
		
		ArrayCreation create = ast.newArrayCreation();
		
		create.setType((ArrayType) ASTNode.copySubtree(ast, this.type));
		
		for (EExpression dim : this.dimensions)
			create.dimensions().add(dim.translate(out));
		
		if (this.initializer != null)
			create.setInitializer(this.initializer.build(out));
		
		return create;
	}

}
