package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EMethodDeclaration extends EBodyDeclaration
{
	MethodDeclaration origin;

	EBlock body;
	Task task;
	String name;
	boolean aeminium;

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

		// do something about parameters?
		// see optimize(), on a future version the read version is necessary for optimizing loops

		Block block = origin.getBody();
		assert(block != null);

		this.body = this.east.extend(block);
		this.east.putNode(this.east.resolveName(origin.resolveBinding()), this);	
	}

	public void optimize()
	{
		// TODO: calculate if any parameter is read-only:
		// a variable is read-only if there isn't any write operations in the closure of body operations

		this.body.optimize();
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

	public void buildClass(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();
		this.task = new Task(this.east, this.name, (CompilationUnit) this.origin.getRoot(), cus);

		TypeDeclaration parent = (TypeDeclaration) this.origin.getParent();

		/* Create the constructor */
		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.setName(ast.newSimpleName(this.name));
		constructor.setConstructor(true);

		Block constructor_body = ast.newBlock();

		if (this.getModifier("static") == null)
		{
			// add _this to parameter list
			SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
			param.setType(ast.newSimpleType(ast.newSimpleName(parent.getName().toString())));
			param.setName(ast.newSimpleName("_this"));

			constructor.parameters().add(param);
		}

		/* Create the return placeholder */
		if (!this.origin.isConstructor() && !this.origin.getReturnType2().toString().equals("void"))
			this.task.addField(this.origin.getReturnType2(), "_ret");

		/* add parameters */
		constructor.parameters().addAll(ASTNode.copySubtrees(ast, this.origin.parameters()));

		for (Object param : constructor.parameters())
		{
			SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
			
			// add field
			this.task.addField(parameter.getType(), parameter.getName().toString());

			// add assigment "this.field = field"
			Assignment asgn = ast.newAssignment();
			
			FieldAccess left = ast.newFieldAccess();
			left.setExpression(ast.newThisExpression());
			left.setName((SimpleName) ASTNode.copySubtree(ast, parameter.getName()));

			asgn.setLeftHandSide(left);
			asgn.setRightHandSide((SimpleName) ASTNode.copySubtree(ast, parameter.getName()));
			
			ExpressionStatement stmt = ast.newExpressionStatement(asgn);

			constructor_body.statements().add(stmt);
		}

		constructor.setBody(constructor_body);
		task.addConstructor(constructor);
		task.setExecute(this.body.build(this.task, cus));
	}

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

		// AeminiumHelper.schedule( ... );
		MethodInvocation schedule = ast.newMethodInvocation();
		schedule.setExpression(ast.newSimpleName("AeminiumHelper"));
		schedule.setName(ast.newSimpleName("schedule"));

			// AeminiumHelper.createNonBlockingTask(new AE_HelloWorld_main_body(args), AeminiumHelper.NO_HINTS),
			MethodInvocation create = ast.newMethodInvocation();
			create.setExpression(ast.newSimpleName("AeminiumHelper"));
			create.setName(ast.newSimpleName("createNonBlockingTask"));

			ClassInstanceCreation main_body = ast.newClassInstanceCreation();
			main_body.setType(ast.newSimpleType(ast.newSimpleName(this.name)));

			for (Object param : this.origin.parameters())
			{
				SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
				main_body.arguments().add(ASTNode.copySubtree(ast, parameter.getName()));
			}

			create.arguments().add(main_body);

			FieldAccess no_hints = ast.newFieldAccess();
			no_hints.setExpression(ast.newSimpleName("AeminiumHelper"));
			no_hints.setName(ast.newSimpleName("NO_HINTS"));

			create.arguments().add(no_hints);

		schedule.arguments().add(create);

		FieldAccess no_parent = ast.newFieldAccess();
		no_parent.setExpression(ast.newSimpleName("AeminiumHelper"));
		no_parent.setName(ast.newSimpleName("NO_PARENT"));
		schedule.arguments().add(no_parent);

		FieldAccess no_deps = ast.newFieldAccess();
		no_deps.setExpression(ast.newSimpleName("AeminiumHelper"));
		no_deps.setName(ast.newSimpleName("NO_DEPS"));
		schedule.arguments().add(no_deps);
		
		body.statements().add(ast.newExpressionStatement(schedule));

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
}
