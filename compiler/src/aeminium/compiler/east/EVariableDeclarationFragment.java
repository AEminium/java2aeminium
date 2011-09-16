package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EVariableDeclarationFragment extends EASTDependentNode
{
	VariableDeclarationFragment origin;
	EExpression expr;
	ESimpleName var;

	EVariableDeclarationFragment(EAST east, VariableDeclarationFragment origin)
	{
		super(east);
		this.origin = origin;
	
		this.var = this.east.extend(origin.getName());
		this.link(this.var);

		if (origin.getInitializer() != null)
		{
			this.expr = this.east.extend(origin.getInitializer());
			this.link(this.expr);
		}
	}

	public void translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts, Type type)
	{
		AST ast = this.east.getAST();

		VariableDeclarationFragment frag;
		frag = (VariableDeclarationFragment) ASTNode.copySubtree(ast, this.origin);

		FieldDeclaration field = ast.newFieldDeclaration(frag);
		field.setType((Type) ASTNode.copySubtree(ast, type));

		// initializer
		if (this.expr != null)
		{
			if (this.isRoot())
			{
				Block body = ast.newBlock();
				this.build(method, cus, (List<Statement>) body.statements());

				TypeDeclaration subtask = this.newSubTaskBody(method, cus, body);

				List<Expression> dependencies = this.getDependencies(method, cus, stmts);
				ClassInstanceCreation creation = this.newSubTaskCreation(method, cus, stmts, subtask);

				this.schedule(method, cus, stmts, dependencies, creation);
			} else
				this.build(method, cus, stmts);
		}
	}

	public void build(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		Assignment assign = ast.newAssignment();
		
		// FIXME: change to field here or in simplename?
		assign.setLeftHandSide(this.var.translate(method, cus, stmts)); // FIXME: translate or build ???
		assign.setRightHandSide(this.expr.translate(method, cus, stmts));

		stmts.add(ast.newExpressionStatement(assign));
	}
}
