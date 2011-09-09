package aeminium.compiler;

import aeminium.compiler.Compiler;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

class TranslateVisitor extends org.eclipse.jdt.core.dom.ASTVisitor
{
	Compiler compiler;

	TypeDeclaration decl;
	CompilationUnit cu;
	int counter;

	TranslateVisitor(Compiler compiler, TypeDeclaration decl)
	{
		this.compiler = compiler;

		this.decl = decl;
		this.cu = (CompilationUnit) this.decl.getRoot();

		this.counter = 0;
	}

	/**
		translates:
			this.a = x.method(...);
		
		to:
			body_1 = new AE_????_method_body(x, ...);
			task_1 = AeminiumHelper.createNonBlockingTask(body_1, this, AeminiumHelper.NO_DEPS);

			class AE_???_main_body_1
			{
				AE_**_main_body _parent;

				...
				execute()
				{
					this._parent.a = b_1._ret;
				}
			}

			body_2 = new AE_??_main_body_1(body_1);

			deps_2 = new ArrayList<Task>();
			deps_2.add(task_1);

			body_2 = AeminiumHelper.createNonBlockingTask(body_2, this, deps_2);
			int a = body_1._ret;
	*/
	@Override
	public boolean visit(MethodInvocation method)
	{
		ITypeBinding type = method.getExpression().resolveTypeBinding();
		if (type == null || !type.isFromSource() || !type.isClass())
			return true;

		IMethodBinding binding = method.resolveMethodBinding();
		if (getAnnotation(binding, "AEminium") == null)
			return true;

		AST ast = method.getAST();
		ASTNode parent = method.getParent();
		Statement stmt;

		while (!(parent instanceof Statement))
			parent = parent.getParent();

		stmt = (Statement) parent;

		this.counter++;

		addImport(this.cu, ast.newName(type.getPackage().getName().toString() + "." + taskBodyName(type, binding)));

		// add body to fields
		VariableDeclarationFragment body_frag = ast.newVariableDeclarationFragment();
		body_frag.setName(ast.newSimpleName("body_" + this.counter));

		FieldDeclaration body = ast.newFieldDeclaration(body_frag);
		body.setType(ast.newSimpleType(ast.newName(taskBodyName(type, binding))));

		this.decl.bodyDeclarations().add(body);

		// add task to fields
		VariableDeclarationFragment task_frag = ast.newVariableDeclarationFragment();
		task_frag.setName(ast.newSimpleName("task_" + this.counter));

		FieldDeclaration task = ast.newFieldDeclaration(task_frag);
		task.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));
		
		this.decl.bodyDeclarations().add(task);

		// set up constructor
		Assignment asgn = ast.newAssignment();

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("body_" + this.counter));

		asgn.setLeftHandSide(access);

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newName(taskBodyName(type, binding))));

		if (!Modifier.isStatic(binding.getModifiers()))
			creation.arguments().add(ASTNode.copySubtree(ast, method.getExpression()));
	
		creation.arguments().addAll(ASTNode.copySubtrees(ast, method.arguments()));

		asgn.setRightHandSide(creation);
		ExpressionStatement asgn_stmt = ast.newExpressionStatement(asgn);

		insertBefore(stmt, asgn_stmt);

		// TODO: schedule task

		// TODO: add dependencies???



		// TODO: split the rest???????????'

		// replace method
		if (!(method.getParent() instanceof ExpressionStatement))
		{
			FieldAccess ret = ast.newFieldAccess();
			ret.setExpression((Expression) ASTNode.copySubtree(ast, access));
			ret.setName(ast.newSimpleName("_ret"));

			replace(method, ret);
		} else
			replace(method.getParent(), ast.newEmptyStatement());

		return true;
	}

	@Override
	public boolean visit(ThisExpression expr)
	{
		AST ast = expr.getAST();
		ASTNode parent = expr.getRoot();

		FieldAccess _this = ast.newFieldAccess();
		_this.setExpression(ast.newThisExpression());
		_this.setName(ast.newSimpleName("_this"));

		TranslateVisitor.replace(expr, _this);

		return true;
	}

	@Override
	public boolean visit(ReturnStatement ret)
	{
		if (ret.getExpression() != null)
		{
			AST ast = ret.getAST();
			ASTNode parent = ret.getRoot();

			Assignment assgn = ast.newAssignment();
	
			FieldAccess _ret = ast.newFieldAccess();
			_ret.setExpression(ast.newThisExpression());
			_ret.setName(ast.newSimpleName("_ret"));
	
			assgn.setLeftHandSide(_ret);
			assgn.setRightHandSide((Expression) ASTNode.copySubtree(ast, ret.getExpression()));

			ExpressionStatement stmt = ast.newExpressionStatement(assgn);
			TranslateVisitor.replace(ret, stmt);
		}

		return true;
	}

	/**
	 Inserts in the AST a Statement node before another Statement
	
	 If the parent of the existing Statement is a Block insertion is done before.
	 Otherwise, a new Block is created with both statements inside, and it replaces the existing node.  
	 @param 
	*/
	public static void insertBefore(Statement existing, Statement stmt)
	{
		AST ast = existing.getAST();
		ASTNode parent = existing.getParent();
	
		if (parent instanceof Block)
		{
			Block block = (Block) parent;
			block.statements().add(block.statements().indexOf(existing), stmt);
		} else
		{
			Block block = ast.newBlock();

			block.statements().add(stmt);
			block.statements().add(ASTNode.copySubtree(ast, existing));

			replace(existing, block);
		}
	}

	/**
	 Replaces in the AST node "what" for node "with"
	 @param what The node to be replaced
	 @param with The node to be inserted
	*/
	public static void replace(ASTNode what, ASTNode with)
	{
		StructuralPropertyDescriptor location = what.getLocationInParent();

		if (location instanceof ChildListPropertyDescriptor)
		{
			ASTNode parent = what.getParent();
			if (parent instanceof Block)
			{
				Block block = (Block) parent;
				int index = block.statements().indexOf(what);

				block.statements().add(index, with);
				block.statements().remove(index+1);
			} else
				System.err.println("Invalid replace inside " + parent.getClass().toString());
		} else
			what.getParent().setStructuralProperty(location, with);
	}

	/**
	 Adds an import for a class if it doesn't exist in the Compilation unit already
	 @param cu The compilation unit
	 @param name The qualified name of the class to load
	 */
	public static void addImport(CompilationUnit cu, Name name)
	{
		for (int i = 0; i < cu.imports().size(); i++)
		{
			ImportDeclaration imp = (ImportDeclaration) cu.imports().get(i);
			
			if (imp.getName().equals(name))
				return;
		}

		ImportDeclaration imp = cu.getAST().newImportDeclaration();
		imp.setName((Name) ASTNode.copySubtree(cu.getAST(), name));
		cu.imports().add(imp);
	}

	public static String taskBodyName(ITypeBinding type, IMethodBinding method)
	{
		return "AE_" + type.getName().toString() + "_" + method.getName().toString() + "_body";
	}

	/**
	 * Gets an annotation from by its name
	 * @param method The method owning the modifiers
	 * @param name The common name of the modifier (e.g.: "public", "static", "@AEminium")
	 */
	public static IAnnotationBinding getAnnotation(IMethodBinding method, String name)
	{
		for (IAnnotationBinding annotation : method.getAnnotations())
			if (annotation.getName().equals(name))
				return annotation;

		return null;
	}
}

