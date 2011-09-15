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
	
		System.err.println("Invalid AbstractTypeDeclaration");
		return null;
	}

	public EExpression extend(Expression expr)
	{
		/* TODO */
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

		System.err.println("Invalid Statement");
		return null;
	}

	public EBlock extend(Block block)
	{
		return new EBlock(this, block);
	}
}
