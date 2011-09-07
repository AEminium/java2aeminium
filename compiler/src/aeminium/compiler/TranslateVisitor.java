package aeminium.compiler;

import aeminium.compiler.Compiler;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

class TranslateVisitor extends org.eclipse.jdt.core.dom.ASTVisitor
{
	Compiler compiler;

	TranslateVisitor(Compiler compiler)
	{
		this.compiler = compiler;
	}

	@Override
	public boolean visit(MethodInvocation method)
	{
		/*
			TODO:
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

		if (method.getName().toString().equals("test"))
			System.out.println(method.toString() + method.getExpression().resolveTypeBinding().getQualifiedName());
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

	public static void replace(ASTNode what, ASTNode with)
	{
		what.getParent().setStructuralProperty(what.getLocationInParent(), with);
	}
}

