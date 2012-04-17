package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EClassInstanceCreation extends EDeferredExpression
{
	protected final DataGroup datagroup;
	
	protected final Type type;
	
	protected final ArrayList<EExpression> arguments;
	
	public EClassInstanceCreation(EAST east, ClassInstanceCreation original, EASTDataNode scope)
	{
		super(east, original, scope, original.resolveConstructorBinding());
		
		this.type = original.getType();
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("new " + this.type.toString()));
		
		this.arguments = new ArrayList<EExpression>();
		for (Object arg : original.arguments())
			this.arguments.add(EExpression.create(east, (Expression) arg, this));
	}

	/* factory */
	public static EClassInstanceCreation create(EAST east, ClassInstanceCreation original, EASTDataNode scope)
	{
		return new EClassInstanceCreation(east, original, scope);
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
			/* TODO/FIXME: allow for deferred class creation
			 * this.deferred = new SignatureItemDeferred(this.deferredDependency, this.getMethod(), this.getDataGroup(), null, dgsArgs); */

			this.deferred = new SignatureItemDeferred(this.dependency, this.getMethod(), this.getDataGroup(), null, dgsArgs);
			this.signature.addItem(this.deferred);

			this.signature.addItem(new SignatureItemWrite(this.dependency, this.getDataGroup()));
			for (int i = 0; i < method.parameters.size(); i++)
				this.signature.addItem(new SignatureItemRead(this.dependency, dgsArgs.get(i)));
		} else
		{
			Signature def = this.getDefaultSignature(this.getDataGroup(), dgsArgs);
			this.signature.addAll(def);
		}
	
	}
	
	protected Signature getDefaultSignature(DataGroup dgRet, ArrayList<DataGroup> dgsArgs)
	{
		/* Conservative approach, without considering parallelism */
		Signature sig = new Signature();
		
		sig.addItem(new SignatureItemRead(this.dependency, this.getEAST().getExternalDataGroup()));
		sig.addItem(new SignatureItemWrite(this.dependency, this.getEAST().getExternalDataGroup()));

		for (DataGroup arg : dgsArgs)
			sig.addItem(new SignatureItemRead(this.dependency, arg));

		if (dgRet != null)
			sig.addItem(new SignatureItemWrite(this.dependency, dgRet));

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
			this.dependency.addStrong(arg.dependency);
		}
		
		Signature sig;
		if (this.deferred != null)
		{
			sig = this.deferred.closure();
	
//			this.dependency.addChild(this.deferredDependency);
	
			for (SignatureItem item : this.signature.getItems())
				if (!item.equals(this.deferred))
					sig.addItem(item);
		} else
			sig = this.signature;
		
		Set<Dependency> deps = stack.getDependencies(sig);
		this.dependency.addWeak(deps);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;
		
		// TODO: always inliing the deferredDependency
		for (EExpression arg : this.arguments)
			sum += arg.optimize();
		
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public int inlineTo(EASTExecutableNode inlineTo)
	{
		// TODO inline ClassInstanceCreation
		System.out.println("TODO: EClassInstanceCreation.inline()");
		return 0;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "new");
		
		for (EExpression arg : this.arguments)
			arg.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		System.err.println("TODO: Paralell ClassInstanceCreation");
		
		/*
		if (this.isAeminium())
		{
			ClassInstanceCreation create = ast.newClassInstanceCreation();
			create.setType(ast.newSimpleType(ast.newSimpleName(this.getMethod().getTask().getName())));	
			
			create.arguments().add(ast.newThisExpression());
	
			if (!this.isStatic())
				create.arguments().add(this.expr.translate(out));
	
			for (EExpression arg : this.arguments)
				create.arguments().add(arg.translate(out));

			return create;
		}
		*/
		
		ClassInstanceCreation create = ast.newClassInstanceCreation();
		create.setType(ast.newSimpleType(ast.newSimpleName(this.binding.getName())));

		for (EExpression arg : this.arguments)
			create.arguments().add(arg.translate(out));

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_ret"));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(access);
		assign.setRightHandSide(create);
		 
		return assign;
	}
	
	@Override
	public boolean isAeminium()
	{
		System.err.println("WARNING: ClassInstanceCreation.isAeminium() not supported yet");
		return false;
	}
}
