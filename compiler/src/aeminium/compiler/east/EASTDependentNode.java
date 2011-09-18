package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public abstract class EASTDependentNode extends EASTNode
{
	protected List<EASTDependentNode> childs;
	protected List<EASTDependentNode> parents;

	protected boolean root;
	protected int task_id;

	EASTDependentNode(EAST east)
	{
		super(east);

		this.childs = new ArrayList<EASTDependentNode>();
		this.parents = new ArrayList<EASTDependentNode>();

		this.root = true;
		this.task_id = -1;
	}

	protected void link(EASTDependentNode child)
	{
		this.childs.add(child);
		child.parents.add(this);
	}

	@Override
	public void optimize()
	{
		// FIXME:  if the parents change in the middle of optimize operations this might change..
		this.root = this.parents.size() == 0;

		for (EASTDependentNode child : this.childs)
			child.optimize();
	}

	protected final boolean isRoot()
	{
		return this.root;
	}

	protected List<Expression> getDependencies(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		List<Expression> exprs = new ArrayList<Expression>();

		if (this.isRoot())
		{
			assert(this.task_id != -1);

			AST ast = this.east.getAST();

			FieldAccess access = ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			
			access.setName(ast.newSimpleName("_task_"+this.task_id));

			exprs.add(access);
		} else
			exprs.addAll(this.getChildDependencies(method, cus, stmts));

		return exprs;
	}

	protected List<Expression> getChildDependencies(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		List<Expression> exprs = new ArrayList<Expression>();

		for (EASTDependentNode child : this.childs)
			exprs.addAll(child.getDependencies(method, cus, stmts));

		return exprs;
	}

	public TypeDeclaration newSubTaskBody(EMethodDeclaration method, List<CompilationUnit> cus, Block body)
	{
		AST ast = this.east.getAST();

		this.task_id = method.subtasks;

		CompilationUnit cu_original = (CompilationUnit) method.getOriginalCU();
		String taskname = method.bodyName() + "_" + this.task_id;

		CompilationUnit cu = ast.newCompilationUnit();

		cu.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, cu_original.getPackage()));
		cu.imports().addAll(ASTNode.copySubtrees(ast, cu_original.imports()));

		TypeDeclaration decl = ast.newTypeDeclaration();
		decl.setName(ast.newSimpleName(taskname));

		decl.superInterfaceTypes().add(ast.newSimpleType(ast.newName("aeminium.runtime.Body")));
	
		// constructor
		{
			MethodDeclaration constructor = ast.newMethodDeclaration();
			constructor.setName(ast.newSimpleName(taskname));
			constructor.setConstructor(true);

			// add _root parameter
			SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
			param.setType(ast.newSimpleType(ast.newSimpleName(method.bodyName())));
			param.setName(ast.newSimpleName("_root"));

			constructor.parameters().add(param);

			Block constructor_body = ast.newBlock();

				// add _root field;
				VariableDeclarationFragment root = ast.newVariableDeclarationFragment();
				root.setName(ast.newSimpleName("_root"));

				FieldDeclaration rootfield = ast.newFieldDeclaration(root);
				rootfield.setType(ast.newSimpleType(ast.newSimpleName(method.bodyName())));

				decl.bodyDeclarations().add(rootfield);

				// this._root = _root
				Assignment asgn = ast.newAssignment();

				FieldAccess access = ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName("_root"));

				asgn.setLeftHandSide(access);
				asgn.setRightHandSide(ast.newSimpleName("_root"));

				constructor_body.statements().add(ast.newExpressionStatement(asgn));
			constructor.setBody(constructor_body);

			decl.bodyDeclarations().add(constructor);
		}

		// execute
		ETypeDeclaration.addExecuteMethod(ast, decl, body);

		cu.types().add(decl);
		cus.add(cu);

		method.subtasks++;

		return decl;
	}

	public ClassInstanceCreation newSubTaskCreation(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts, TypeDeclaration type)
	{
		AST ast = this.east.getAST();

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType((SimpleName) ASTNode.copySubtree(ast, type.getName())));
		creation.arguments().add(ast.newThisExpression());
		
		return creation;
	}

	public void schedule(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts, List<Expression> dependencies, ClassInstanceCreation creation)
	{
		assert(this.task_id != -1);

		AST ast = this.east.getAST();

		// add body field
		VariableDeclarationFragment body_frag = ast.newVariableDeclarationFragment();
		body_frag.setName(ast.newSimpleName("_body_" + this.task_id));

		FieldDeclaration body_field = ast.newFieldDeclaration(body_frag);
		body_field.setType(ast.newSimpleType((SimpleName) ASTNode.copySubtree(ast, ((SimpleType) creation.getType()).getName())));

		method.getTaskBody().bodyDeclarations().add(body_field);

		// add body creation
		Assignment body_assign = ast.newAssignment();

		FieldAccess root_access = ast.newFieldAccess();
		root_access.setExpression(ast.newThisExpression());
		root_access.setName(ast.newSimpleName("_root"));

		FieldAccess body_access = ast.newFieldAccess();
		body_access.setExpression(root_access);
		body_access.setName(ast.newSimpleName("_body_"+this.task_id)); 

		body_assign.setLeftHandSide(body_access);
		body_assign.setRightHandSide(creation);

		stmts.add(ast.newExpressionStatement(body_assign));

		// add deps declaration
		VariableDeclarationFragment var_frag = ast.newVariableDeclarationFragment();
		var_frag.setName(ast.newSimpleName("_deps_"+this.task_id));
		
		ParameterizedType var_type = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("ArrayList")));
		var_type.typeArguments().add(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));

		ClassInstanceCreation var_creation = ast.newClassInstanceCreation();
		var_creation.setType(var_type);

		var_frag.setInitializer(var_creation);

		VariableDeclarationStatement var_stmt = ast.newVariableDeclarationStatement(var_frag);
		var_stmt.setType((Type) ASTNode.copySubtree(ast, var_type));

		stmts.add(var_stmt);

		// add deps
		for (Expression dep : dependencies)
		{
			MethodInvocation dep_invocation = ast.newMethodInvocation();
			dep_invocation.setExpression(ast.newSimpleName("_deps_"+this.task_id));
			dep_invocation.setName(ast.newSimpleName("add"));
			
			dep_invocation.arguments().add(dep);

			stmts.add(ast.newExpressionStatement(dep_invocation));
		}

		// add task field
		VariableDeclarationFragment task_frag = ast.newVariableDeclarationFragment();
		task_frag.setName(ast.newSimpleName("_task_"+this.task_id));

		FieldDeclaration task_field = ast.newFieldDeclaration(task_frag);
		body_field.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));

		method.getTaskBody().bodyDeclarations().add(task_field);

		// add task creation
		Assignment task_assign = ast.newAssignment();
	
		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression((Expression) ASTNode.copySubtree(ast, root_access));
		task_access.setName(ast.newSimpleName("_task_" + this.task_id));

		task_assign.setLeftHandSide(task_access);

		MethodInvocation task_creation = ast.newMethodInvocation();
		task_creation.setExpression(ast.newSimpleName("AeminiumHelper"));
		task_creation.setName(ast.newSimpleName("createNonBlockingTask"));
		
		task_creation.arguments().add((Expression) ASTNode.copySubtree(ast, body_access));

		FieldAccess no_hints = ast.newFieldAccess();
		no_hints.setExpression(ast.newSimpleName("AeminiumHelper"));
		no_hints.setName(ast.newSimpleName("NO_HINTS"));
		
		task_creation.arguments().add(no_hints);
		
		task_assign.setRightHandSide(task_creation);

		stmts.add(ast.newExpressionStatement(task_assign));

		// add schedule
	}

}
