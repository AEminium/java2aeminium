package aeminium.compiler;

import aeminium.compiler.*;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

class AeminiumVisitor extends org.eclipse.jdt.core.dom.ASTVisitor
{
	Compiler compiler;

	/**
	 * A class implementing the visitor pattern for ASTNodes.
	 * 
	 * The AeminiumVisitor is responsible for translating methods into the corresponding AE task bodies
	 * and creating the necessary startup code
	 */
	AeminiumVisitor(Compiler compiler)
	{
		this.compiler = compiler;
	}

	@Override
	public boolean visit(MethodDeclaration method)
	{
		IExtendedModifier modifier = getModifier(method, "@AEminium");
		if (modifier != null)
		{
			method.modifiers().remove(modifier);
			
			this.buildClassBody(method);

			if (method.getName().toString().equals("main"))
				this.replaceMain(method);
		}

		return true;
	}

	/**
	 * Builds a new class that implements the aeminium.runtime.Body interface that executes the same operations a method.
	 * @param method The method that the created class should execute
	 */
	private void buildClassBody(MethodDeclaration method)
	{
		AST ast = method.getAST();

		/* Create the class definition */
		CompilationUnit cu = ast.newCompilationUnit();
		CompilationUnit cu_original = (CompilationUnit) method.getRoot();
		TypeDeclaration parent = (TypeDeclaration) method.getParent();

		cu.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, cu_original.getPackage()));
		cu.imports().addAll(ASTNode.copySubtrees(ast, cu_original.imports()));

		TypeDeclaration decl = ast.newTypeDeclaration();		
		decl.setName(ast.newSimpleName(this.taskBodyName(method)));
		
		decl.superInterfaceTypes().add(ast.newSimpleType(ast.newName("aeminium.runtime.Body")));
		
		/* Create the constructor */
		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.setName(ast.newSimpleName(this.taskBodyName(method)));
		constructor.setConstructor(true);

		Block constructor_body = ast.newBlock();

		if (getModifier(method, "static") == null)
		{
			// add _this field
			VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName("_this"));

			FieldDeclaration field = ast.newFieldDeclaration(frag);
			field.setType(ast.newSimpleType(ast.newSimpleName(parent.getName().toString())));
			
			// add _this to parameter list
			SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
			param.setType(ast.newSimpleType(ast.newSimpleName(parent.getName().toString())));
			param.setName(ast.newSimpleName("_this"));

			constructor.parameters().add(param);
		}

		/* add parameters */
		constructor.parameters().addAll(ASTNode.copySubtrees(ast, method.parameters()));

		for (Object param : method.parameters())
		{
			SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
			
			// add field
			VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
			frag.setName((SimpleName) ASTNode.copySubtree(ast, parameter.getName()));

			FieldDeclaration field = ast.newFieldDeclaration(frag);
			field.setType((Type) ASTNode.copySubtree(ast, parameter.getType()));
			
			decl.bodyDeclarations().add(field);

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
		decl.bodyDeclarations().add(constructor);

		/* Create the execute() method */

		// public void execute(aeminium.runtime.Runtime rt, aeminium.runtime.Task task) throws Exception
		MethodDeclaration execute = ast.newMethodDeclaration();
		execute.setName(ast.newSimpleName("execute"));
		execute.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		SingleVariableDeclaration runtime = ast.newSingleVariableDeclaration();
		runtime.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Runtime")));
		runtime.setName(ast.newSimpleName("rt"));

		execute.parameters().add(runtime);

		SingleVariableDeclaration task = ast.newSingleVariableDeclaration();
		task.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));
		task.setName(ast.newSimpleName("task"));

		execute.parameters().add(task);

		execute.thrownExceptions().add(ast.newSimpleName("Exception"));

		/* FIXME: by now its only a copy */
		Block execute_body = (Block) ASTNode.copySubtree(ast, method.getBody());

		execute.setBody(execute_body);
		decl.bodyDeclarations().add(execute);

		cu.types().add(decl);

		compiler.saveCU(cu);
	}

	private void replaceMain(MethodDeclaration method)
	{
		/* 
			AeminiumHelper.init();
			AeminiumHelper.schedule(
				AeminiumHelper.createNonBlockingTask(new AE_HelloWorld_main_body(args), AeminiumHelper.NO_HINTS),
				AeminiumHelper.NO_PARENT,
				AeminiumHelper.NO_DEPS
			);
			AeminiumHelper.shutdown();
		*/
		AST ast = method.getAST();
		Block body = ast.newBlock();

		// AeminiumHelper.init();
		MethodInvocation init = ast.newMethodInvocation();
		init.setExpression(ast.newSimpleName("AeminiumHelper"));
		init.setName(ast.newSimpleName("init"));

		body.statements().add(ast.newExpressionStatement(init));

		MethodInvocation schedule = ast.newMethodInvocation();
		schedule.setExpression(ast.newSimpleName("AeminiumHelper"));
		schedule.setName(ast.newSimpleName("schedule"));

			// AeminiumHelper.createNonBlockingTask(new AE_HelloWorld_main_body(args), AeminiumHelper.NO_HINTS),
			MethodInvocation create = ast.newMethodInvocation();
			create.setExpression(ast.newSimpleName("AeminiumHelper"));
			create.setName(ast.newSimpleName("createNonBlockingTask"));

			ClassInstanceCreation main_body = ast.newClassInstanceCreation();
			main_body.setType(ast.newSimpleType(ast.newSimpleName(taskBodyName(method))));

			for (Object param : method.parameters())
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
	}

	/**
	 * Returns the name of the class that implements the aeminium.runtime.Body
	 */
	private String taskBodyName(MethodDeclaration method)
	{
		TypeDeclaration parent = (TypeDeclaration) method.getParent();

		return "AE_" + parent.getName().toString() + "_" + method.getName().toString() + "_body";
	}

	/**
	 * Gets a modifier from a list by its common name
	 * @param method The method owning the modifiers
	 * @param name The common name of the modifier (e.g.: "public", "static", "@AEminium")
	 */
	private static IExtendedModifier getModifier(MethodDeclaration method, String name)
	{
		for (Object modifier : method.modifiers())
			if (modifier.toString().equals(name))
				return (IExtendedModifier) modifier;

		return null;
	}
}

