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
	HashMap<String, EASTNode> nodes;

	public EAST()
	{
		this.ast = AST.newAST(AST.JLS3);
		this.units = new HashMap<String, ECompilationUnit>();	
		this.nodes = new HashMap<String, EASTNode>();	
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

		if (expr instanceof Assignment)
			return new EAssignment(this, (Assignment) expr);

		if (expr instanceof ThisExpression)
			return new EThisExpression(this, (ThisExpression) expr);

		if (expr instanceof ParenthesizedExpression)
			return new EParenthesizedExpression(this, (ParenthesizedExpression) expr);

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

		if (stmt instanceof ExpressionStatement)
			return new EExpressionStatement(this, (ExpressionStatement) stmt);

		if (stmt instanceof IfStatement)
			return new EIfStatement(this, (IfStatement) stmt);

		System.err.println("Invalid Statement: " + stmt.getClass().toString());
		return null;
	}

	public EBlock extend(Block block)
	{
		return new EBlock(this, block);
	}

	public ESingleVariableDeclaration extend(SingleVariableDeclaration decl)
	{
		return new ESingleVariableDeclaration(this, decl);
	}

	public EVariableDeclarationFragment extend(VariableDeclarationFragment frag)
	{
		return new EVariableDeclarationFragment(this, frag);
	}

	public ESimpleName extend(SimpleName name)
	{
		return new ESimpleName(this, name);
	}

	public String resolveName(IBinding binding)
	{
		String id = "";
		
		if (binding instanceof IVariableBinding)
		{
			IVariableBinding var = (IVariableBinding) binding;
			ITypeBinding type = var.getDeclaringClass();
			if (type == null)
			{
				// local var
				IMethodBinding method = var.getDeclaringMethod();
				type = method.getDeclaringClass();
				id = "var_" + type.getQualifiedName() + "_" + method.getName() + "_" + var.getVariableId();
			} else
				id = "var_" + type.getQualifiedName() + "_" + var.getVariableId();
		} else if (binding instanceof IMethodBinding)
		{
			IMethodBinding method = (IMethodBinding) binding;
			ITypeBinding type = method.getDeclaringClass();
	
			// FIXME: identify overloaded methods
			id = "method_" + type.getQualifiedName() + "_" + method.getName();
		}

		return id;
	}

	public void putNode(String name, EASTNode node)
	{
		this.nodes.put(name, node);	
	}

	public EASTNode getNode(String name)
	{
		return this.nodes.get(name);
	}

	public Type buildTypeFromBinding(ITypeBinding binding)
	{
		if (binding.isClass())
			return this.ast.newSimpleType(this.ast.newName(binding.getQualifiedName()));

		if (binding.isPrimitive())
			return this.ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));

		// TODO
		System.err.println("Binding: TODO: complex types");
		return null;
	}

	public Type boxPrimitiveType(PrimitiveType primitive)
	{
		HashMap<PrimitiveType.Code, String> primitives = new HashMap<PrimitiveType.Code, String>();

		primitives.put(PrimitiveType.BYTE, "Byte");
		primitives.put(PrimitiveType.SHORT, "Short");
		primitives.put(PrimitiveType.INT, "Integer");
		primitives.put(PrimitiveType.LONG, "Long");
		primitives.put(PrimitiveType.FLOAT, "Float");
		primitives.put(PrimitiveType.DOUBLE, "Double");
		primitives.put(PrimitiveType.CHAR, "Char");
		primitives.put(PrimitiveType.BOOLEAN, "Boolean");
		
		String boxedName = primitives.get(primitive.getPrimitiveTypeCode());
		return this.ast.newSimpleType(ast.newSimpleName(boxedName));
	}
}
