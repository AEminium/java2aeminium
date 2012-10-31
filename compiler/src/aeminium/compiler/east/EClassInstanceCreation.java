package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EClassInstanceCreation extends EDeferredExpression
{
	protected final DataGroup datagroup;
	
	protected final Type type;
	
	protected final ArrayList<EExpression> arguments;
	
	public EClassInstanceCreation(EAST east, ClassInstanceCreation original, EASTDataNode scope, EASTExecutableNode parent, EClassInstanceCreation base)
	{
		super(east, original, scope, original.resolveConstructorBinding(), parent, base);
		
		this.type = original.getType();
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("new " + this.type.toString()));
		
		this.arguments = new ArrayList<EExpression>();

		for (int i = 0 ; i < original.arguments().size(); i++)
		{
			this.arguments.add
			(
				EExpression.create
				(
					east,
					(Expression) original.arguments().get(i),
					this,
					this,
					base == null ? null : base.arguments.get(i)
				)
			);
		}
	}

	/* factory */
	public static EClassInstanceCreation create(EAST east, ClassInstanceCreation original, EASTDataNode scope, EASTExecutableNode parent, EClassInstanceCreation base)
	{
		return new EClassInstanceCreation(east, original, scope, parent, base);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public ClassInstanceCreation getOriginal()
	{
		return (ClassInstanceCreation) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		for (EExpression arg : this.arguments)
			arg.checkSignatures();

		ArrayList<DataGroup> dgsArgs = new ArrayList<DataGroup>();
		for (EExpression arg : this.arguments)
			dgsArgs.add(arg.getDataGroup());
		
		EMethodDeclaration method = this.getMethod();
		
		if (method != null)
		{
			this.deferred = new SignatureItemDeferred(this.getMethod(), this.getDataGroup(), null, dgsArgs);
			this.signature.addItem(this.deferred);
			this.signature.addItem(new SignatureItemWrite(this.getDataGroup()));
		} else
		{
			Signature def = this.getDefaultSignature(this.getDataGroup(), dgsArgs);
			this.signature.addAll(def);
		}
	
	}
	
	protected Signature getDefaultSignature(DataGroup dgRet, ArrayList<DataGroup> dgsArgs)
	{
		/* Conservative approach */
		Signature sig = new Signature();
		
		sig.addItem(new SignatureItemRead(this.getEAST().getExternalDataGroup()));
		sig.addItem(new SignatureItemWrite(this.getEAST().getExternalDataGroup()));

		for (DataGroup arg : dgsArgs)
			sig.addItem(new SignatureItemRead(arg));
		
		if (dgRet != null)
			sig.addItem(new SignatureItemWrite(dgRet));
		
		return sig;
	}
	
	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		
		for (EExpression arg : this.arguments)
			sig.addAll(arg.getFullSignature());

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		for (EExpression arg : this.arguments)
		{
			arg.checkDependencies(stack);
			this.addStrongDependency(arg);
		}
		
		Signature sig;
		if (this.deferred != null)
			sig = this.deferred.closure();
		else
			sig = this.signature;
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, sig);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;
		
		for (EExpression arg : this.arguments)
			sum += arg.optimize();
		
		sum += super.optimize();
		
		return sum;
	}

	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "new", this.base == null ? null : this.base.task);
		
		for (int i = 0; i < this.arguments.size(); i++)
			this.arguments.get(i).preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		if (this.isSequential() || !this.isAeminium())
		{
			ClassInstanceCreation create = ast.newClassInstanceCreation();
			create.setType(ast.newSimpleType(ast.newSimpleName(this.binding.getName())));

			for (EExpression arg : this.arguments)
				create.arguments().add(arg.translate(out));

			return create;
		}
		
		System.err.println("TODO: Paralell ClassInstanceCreation");

		/* same thing for now */
		ClassInstanceCreation create = ast.newClassInstanceCreation();
		create.setType(ast.newSimpleType(ast.newSimpleName(this.binding.getName())));

		for (EExpression arg : this.arguments)
			create.arguments().add(arg.translate(out));

		if (this.inlineTask)
			return create;
		
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_ret"));
		
		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(access);
		assign.setRightHandSide(create);
		
		return assign;
	}

	@Override
	public Statement buildStmt(List<CompilationUnit> out)
	{
		AST ast = this.getAST();
		return ast.newExpressionStatement(this.build(out));
	}
}
