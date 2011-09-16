package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.*;
import aeminium.compiler.east.*;

public class EClassInstanceCreation extends EExpression
{
	ClassInstanceCreation origin;
	List<EExpression> args;
	IMethodBinding binding;

	EClassInstanceCreation(EAST east, ClassInstanceCreation origin)
	{
		super(east);

		this.origin = origin;
		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
		{
			EExpression earg = this.east.extend((Expression) arg);
			this.link(earg);
			this.args.add(earg);
		}

		// TODO: add internal dependencies (System.out, and other statics here)?
	}

	@Override
	public void optimize()
	{
		this.root = true;
	}

	@Override
	public Expression translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		assert(this.isRoot());

		// TODO: IMPROVE alow @AEminium on constructors? 
		AST ast = this.east.getAST();

		// task body
		Block body = ast.newBlock();

		// _ret = X(...);
		Assignment assign = ast.newAssignment();

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("_ret"));

		assign.setLeftHandSide(access);
		assign.setRightHandSide(this.build(method, cus, (List<Statement>) body.statements()));
		
		body.statements().add(ast.newExpressionStatement(assign));

		TypeDeclaration decl = this.newSubTaskBody(method, cus, body);

		// _ret field
		VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
		frag.setName(ast.newSimpleName("_ret"));

		FieldDeclaration ret = ast.newFieldDeclaration(frag);
		ret.setType((SimpleType) ASTNode.copySubtree(ast, this.origin.getType()));
		ret.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		ret.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));

		decl.bodyDeclarations().add(ret);
		
		// scheduling
		ClassInstanceCreation creation = this.newSubTaskCreation(method, cus, stmts, decl);
		List<Expression> dependencies = this.getDependencies(method, cus, stmts);
		this.schedule(method, cus, stmts, dependencies, creation);
		
		// expression
		assert(this.task_id != -1);

		FieldAccess ret_body = ast.newFieldAccess();
		ret_body.setExpression(ast.newThisExpression());
		ret_body.setName(ast.newSimpleName("_body_" + this.task_id));

		FieldAccess ret_access = ast.newFieldAccess();
		ret_access.setExpression(ret_body);
		ret_access.setName(ast.newSimpleName("_ret"));

		return ret_access;
	}

	public Expression build(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType((SimpleType) ASTNode.copySubtree(ast, this.origin.getType()));

		for (EExpression arg : this.args)
			creation.arguments().add(arg.translate(method, cus, stmts));

		return creation;
	}
}
