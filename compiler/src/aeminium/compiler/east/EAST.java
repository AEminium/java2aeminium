package aeminium.compiler.east;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EAST
{
	AST ast;
	HashMap<String, ECompilationUnit> units;

	public EAST()
	{
		this.ast = AST.newAST(AST.JLS3);
		this.units = new HashMap<String, ECompilationUnit>();	
	}

	public void optimize()
	{
		for (ECompilationUnit unit : this.units.values())
			unit.optimize();
	}

	public List<CompilationUnit> translate()
	{
		List<CompilationUnit> units = new ArrayList<CompilationUnit>();
	
		for (ECompilationUnit unit : this.units.values())
			unit.translate(units);

		return units;
	}

	public AST getAST()
	{
		return this.ast;
	}

	public ECompilationUnit extend(CompilationUnit unit)
	{
		ECompilationUnit ecu = new ECompilationUnit(this, unit);

		this.units.put(ecu.getQualifiedName(), ecu);

		return ecu;
	}

	public EAbstractTypeDeclaration extend(AbstractTypeDeclaration decl)
	{
		if (decl instanceof TypeDeclaration)
			return new ETypeDeclaration(this, (TypeDeclaration) decl); 
	
		System.err.println("Invalid AbstractTypeDeclaration: " + decl.getClass().toString());
		return null;
	}

	public EExpression extend(Expression expr)
	{
		if (expr instanceof MethodInvocation)
			return new EMethodInvocation(this, (MethodInvocation) expr);

		if (expr instanceof InfixExpression)
			return new EInfixExpression(this, (InfixExpression) expr);

		if (expr instanceof SimpleName)
			return new ESimpleName(this, (SimpleName) expr);

		if (expr instanceof NumberLiteral)
			return new ENumberLiteral(this, (NumberLiteral) expr);

		if (expr instanceof ClassInstanceCreation)
			return new EClassInstanceCreation(this, (ClassInstanceCreation) expr);

		System.err.println("Invalid expr: " + expr.getClass().toString());
		return null;
	}

	public EReturnStatement extend(ReturnStatement ret)
	{
		return new EReturnStatement(this, ret);
	}

	public EFieldDeclaration extend(FieldDeclaration field)
	{
		return new EFieldDeclaration(this, field);
	}

	public EMethodDeclaration extend(MethodDeclaration method)
	{
		return new EMethodDeclaration(this, method);
	}

	public EStatement extend(Statement stmt)
	{
		if (stmt instanceof Block)
			return new EBlock(this, (Block) stmt);

		if (stmt instanceof ReturnStatement)
			return new EReturnStatement(this, (ReturnStatement) stmt);

		if (stmt instanceof VariableDeclarationStatement)
			return new EVariableDeclarationStatement(this, (VariableDeclarationStatement) stmt);

		System.err.println("Invalid Statement: " + stmt.getClass().toString());
		return null;
	}

	public EBlock extend(Block block)
	{
		return new EBlock(this, block);
	}

	public EVariableDeclarationFragment extend(VariableDeclarationFragment frag)
	{
		return new EVariableDeclarationFragment(this, frag);
	}

	public ESimpleName extend(SimpleName name)
	{
		return new ESimpleName(this, name);
	}
}
