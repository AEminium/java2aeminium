package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EVariableDeclarationFragment extends EASTDependentNode
{
	EAST east;
	VariableDeclarationFragment origin;
	EExpression expr;
	ESimpleName var;

	EVariableDeclarationFragment(EAST east, VariableDeclarationFragment origin)
	{
		this.east = east;
		this.origin = origin;
	
		this.var = this.east.extend(origin.getName());
		this.link(this.var);

		if (origin.getInitializer() != null)
		{
			this.expr = this.east.extend(origin.getInitializer());
			this.link(this.expr);
		}
	}

	public void translate(TypeDeclaration decl, List<Statement> stmts, Type type)
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
			
			} else
			{
				// TODO
				System.err.println("Fragment error");
			}
		}
	}

	public Statement build(TypeDeclaration decl, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		Assignment assign = ast.newAssignment();
		
		// FIXME: change to field here or in simplename?
		assign.setLeftHandSide(this.var.translate(decl, stmts));// FIXME: translate or build ???
		assign.setRightHandSide(this.expr.translate(decl, stmts));

		return ast.newExpressionStatement(assign);
	}
}
