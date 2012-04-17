package aeminium.compiler.east;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItem;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.MethodTask;
import aeminium.compiler.task.Task;

public class EMethodDeclaration extends EBodyDeclaration implements EASTDeclaringNode
{
	protected final IMethodBinding binding;

	protected final ArrayList<EMethodDeclarationParameter> parameters;
	protected final EBlock body;
	
	protected final DataGroup returnDataGroup;
	
	public EMethodDeclaration(EAST east, MethodDeclaration original, ETypeDeclaration type)
	{
		super(east, original, type);
		
		this.returnDataGroup = this.getDataGroup().append(new SimpleDataGroup("ret " + original.getName().toString()));

		this.binding = original.resolveBinding();
		
		this.east.addNode(this.binding, this);
		
		this.parameters = new ArrayList<EMethodDeclarationParameter>();
		for (Object param : original.parameters())
			this.parameters.add(EMethodDeclarationParameter.create(this.east, (SingleVariableDeclaration) param, this));
		
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
		this.body.checkSignatures();
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		sig.addAll(this.body.getFullSignature());
		
		return sig;
	}
	
	public Signature undefer(Dependency dep, DataGroup dgRet, DataGroup dgThis, ArrayList<DataGroup> dgsArgs)
	{
		Signature sig = new Signature();

		outerLoop: for (SignatureItem item : this.getFullSignature().getItems())
		{
			// don't propagate local variable changes
			if (item.isLocalTo(this.body.getDataGroup()))
				continue;

			SignatureItem _item = item;
			_item = _item.setDependency(dep);

			for (int i = 0; i < this.parameters.size(); i++)
			{
				// This item refers a read/write/merge to a parameter passed in by copy (native)
				// it has no implications on outer dependencies and can be cut out
				if (this.parameters.get(i).getOriginal().getType().isPrimitiveType()
					&& item.isLocalTo(this.parameters.get(i).name.getDataGroup()))
					continue outerLoop;

				_item = _item.replace(this.parameters.get(i).name.getDataGroup(), dgsArgs.get(i));
			}
			
			if (this.getOriginal().isConstructor())
			{
				_item = _item.replace(this.type.thisDataGroup, dgRet);
			} else
			{
				if (!this.isVoid())
					_item = _item.replace(this.returnDataGroup, dgRet);

				if (!this.isStatic())
					_item = _item.replace(this.type.thisDataGroup, dgThis);	
			}

			sig.addItem(_item);
		}
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.body.checkDependencies(stack);
		this.dependency.addStrong(this.body.dependency);
	}
	
	public boolean isVoid()
	{
		return this.getOriginal().getReturnType2().toString().equals("void");
	}

	public boolean isMain()
	{
		return this.getOriginal().getName().toString().equals("main") && this.isVoid() && this.isStatic();
	}
	
	public boolean isConstructor()
	{
		return this.getOriginal().isConstructor();
	}
	
	public EBlock getBody()
	{
		return this.body;
	}

	@Override
	public int optimize()
	{
		int sum = 0;
		
		sum += this.body.optimize();
		sum += super.optimize();
		
		return sum;
	}

	public void preTranslate()
	{
		String name = this.type.getOriginal().getName() + "_" + this.getOriginal().getName();
		
		this.preTranslate(MethodTask.create(this, name));		
	}

	@Override
	public void preTranslate(Task parent)
	{
		this.task = parent;
		this.body.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	public MethodDeclaration translate(ArrayList<CompilationUnit> out)
	{
		out.add(this.task.translate());

		MethodDeclaration execute = this.task.getExecute();
		
		execute.getBody().statements().addAll(this.body.translate(out));

		if (this.isMain())
			return this.buildMain();
		
		return (MethodDeclaration) ASTNode.copySubtree(this.getAST(), this.original);
	}

	@SuppressWarnings("unchecked")
	private MethodDeclaration buildMain()
	{
		AST ast = this.getAST();
		MethodDeclaration method = ast.newMethodDeclaration();
		
		method.setName((SimpleName) ASTNode.copySubtree(ast, this.getOriginal().getName()));
		method.parameters().addAll(ASTNode.copySubtrees(ast, this.getOriginal().parameters()));

		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));

		Block body = ast.newBlock();

		// AeminiumHelper.init();
		MethodInvocation init = ast.newMethodInvocation();
		init.setExpression(ast.newSimpleName("AeminiumHelper"));
		init.setName(ast.newSimpleName("init"));

		body.statements().add(ast.newExpressionStatement(init));

		// new Class_main().schedule(AeminiumHelper.NO_PARENT, AeminiumHelper.NO_DEPS, args);
		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.task.getName())));

		MethodInvocation schedule = ast.newMethodInvocation();
		schedule.setExpression(creation);
		schedule.setName(ast.newSimpleName("schedule"));

		FieldAccess parent = ast.newFieldAccess();
		parent.setExpression(ast.newSimpleName("AeminiumHelper"));
		parent.setName(ast.newSimpleName("NO_PARENT"));

		schedule.arguments().add(parent);

		FieldAccess deps = ast.newFieldAccess();
		deps.setExpression(ast.newSimpleName("AeminiumHelper"));
		deps.setName(ast.newSimpleName("NO_DEPS"));
		
		schedule.arguments().add(deps);
		
		for (Object arg : this.getOriginal().parameters())
			schedule.arguments().add((Expression) ASTNode.copySubtree(ast, ((SingleVariableDeclaration) arg).getName()));

		body.statements().add(ast.newExpressionStatement(schedule));

		// AeminiumHelper.shutdown();
		MethodInvocation shutdown = ast.newMethodInvocation();
		shutdown.setExpression(ast.newSimpleName("AeminiumHelper"));
		shutdown.setName(ast.newSimpleName("shutdown"));

		body.statements().add(ast.newExpressionStatement(shutdown));

		method.setBody(body);

		return method;
	}

	public ArrayList<EMethodDeclarationParameter> getParameters()
	{
		return this.parameters;
	}

	public Type getThisType()
	{
		AST ast = this.getAST();
		
		// TODO: EMethodDeclaration Type from TypeDeclaration
		return ast.newSimpleType((SimpleName) ASTNode.copySubtree(ast, this.type.getOriginal().getName()));
	}
	
	@Override
	public MethodTask getTask()
	{
		return (MethodTask) this.task;
	}
}