package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.Task;
import aeminium.compiler.datagroup.ReturnDataGroup;

public class EMethodDeclaration extends EBodyDeclaration
{
	private final MethodDeclaration origin;
	
	private final EBlock body;
	private final List<ESingleVariableDeclaration> parameters;

	private final String name;
	private final boolean aeminium;

	private IMethodBinding binding;
	private Task task;

	private final ReturnDataGroup returnDatagroup;
	
	EMethodDeclaration(EAST east, MethodDeclaration origin)
	{
		super(east);

		this.origin = origin;

		AbstractTypeDeclaration parent = (AbstractTypeDeclaration) this.origin.getParent();
		this.name = parent.getName().toString() + "_" + this.origin.getName().toString();

		if (this.getModifier("@AEminium") != null)
		{
			this.origin.modifiers().remove(this.getModifier("@AEminium"));
			this.aeminium = true;
		} else
			this.aeminium = false;

		Block block = origin.getBody();
		assert(block != null);

		this.body = this.east.extend(block);

		this.parameters = new ArrayList<ESingleVariableDeclaration>();
		for (Object param : this.origin.parameters())
			this.parameters.add(this.east.extend((SingleVariableDeclaration) param));

		this.returnDatagroup = new ReturnDataGroup(this);
	}

	@Override
	public void analyse()
	{
		this.binding = this.origin.resolveBinding();
		this.east.putNode(this.east.resolveName(origin.resolveBinding()), this);

		for (ESingleVariableDeclaration param : this.parameters)
			param.analyse();

		this.body.analyse();

		this.signature.addFrom(this.body.getSignature());
		// TODO: add from parameters?
		
		System.err.println("Signature for " + this.origin.getName());
		System.err.println(this.signature);
	}

	@Override
	public int optimize()
	{
		int sum = this.body.optimize();

		for (ESingleVariableDeclaration param : this.parameters)
			sum += param.optimize();
		
		return sum;
	}

	public void preTranslate()
	{
		if (this.aeminium)
		{
			this.task = new Task(this.east, this.name, (CompilationUnit) this.origin.getRoot());
			this.body.preTranslate(this.task);
		
			for (ESingleVariableDeclaration param : this.parameters)
				param.preTranslate(this);	
		}
	}
	
	public MethodDeclaration translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		if (this.aeminium)
		{
			this.buildClass(cus);

			if (this.isMain())
				return this.buildMain();
		}

		return (MethodDeclaration) ASTNode.copySubtree(ast, this.origin);
	}

	public boolean isMain()
	{
		return (this.getModifier("static") != null) && this.origin.getName().toString().equals("main");
	}

	@SuppressWarnings("unchecked")
	public void buildClass(List<CompilationUnit> cus)
	{
		Type this_type = null;
		if (!this.isStatic())
			this_type = this.east.buildTypeFromBinding(this.binding.getDeclaringClass());

		Type ret_type = this.origin.getReturnType2();
		if (ret_type instanceof PrimitiveType)
		{
			PrimitiveType ret_primitive = (PrimitiveType)ret_type;

			if (ret_primitive.getPrimitiveTypeCode() != PrimitiveType.VOID)
				ret_type = this.east.boxPrimitiveType(ret_primitive);
		}

		this.task.setMethodTask(ret_type, this_type, this.origin.parameters());

		for (ESingleVariableDeclaration param : this.parameters)
			param.translate(cus);

		task.setExecute(this.body.build(this.task, cus));

		/* Create the constructor */
		MethodDeclaration constructor = this.task.createConstructor();
		this.task.addConstructor(constructor);
		
		cus.add(this.task.getCompilationUnit());
	}

	@SuppressWarnings("unchecked")
	public MethodDeclaration buildMain()
	{
		AST ast = this.east.getAST();
		MethodDeclaration method = ast.newMethodDeclaration();
		
		method.setName((SimpleName) ASTNode.copySubtree(ast, this.origin.getName()));
		method.parameters().addAll(ASTNode.copySubtrees(ast, this.origin.parameters()));

		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));

		Block body = ast.newBlock();

		// AeminiumHelper.init();
		MethodInvocation init = ast.newMethodInvocation();
		init.setExpression(ast.newSimpleName("AeminiumHelper"));
		init.setName(ast.newSimpleName("init"));

		body.statements().add(ast.newExpressionStatement(init));

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.task.getType())));
		creation.arguments().add(ast.newNullLiteral());

		for (Object arg : this.origin.parameters())
			creation.arguments().add((Expression) ASTNode.copySubtree(ast, ((SingleVariableDeclaration) arg).getName()));

		body.statements().add(ast.newExpressionStatement(creation));

		// AeminiumHelper.shutdown();
		MethodInvocation shutdown = ast.newMethodInvocation();
		shutdown.setExpression(ast.newSimpleName("AeminiumHelper"));
		shutdown.setName(ast.newSimpleName("shutdown"));

		body.statements().add(ast.newExpressionStatement(shutdown));

		method.setBody(body);

		return method;
	}

	/**
	 * Gets a modifier from a list by its common name
	 * @param name The common name of the modifier (e.g.: "public", "static", "@AEminium")
	 */
	public IExtendedModifier getModifier(String name)
	{
		for (Object modifier : this.origin.modifiers())
			if (modifier.toString().equals(name))
				return (IExtendedModifier) modifier;

		return null;
	}

	public boolean isAEminium()
	{
		return this.aeminium;
	}

	public boolean isStatic()
	{
		return this.getModifier("static") != null;
	}

	public Task getTask()
	{
		assert (this.task != null);
		return this.task;
	}
	
	public ReturnDataGroup getReturnDataGroup()
	{
		return this.returnDatagroup;
	}
}
