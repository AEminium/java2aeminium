package aeminium.compiler.east;

import java.util.HashMap;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;

public class EType
{
	public static SimpleType boxType(PrimitiveType type)
	{
		AST ast = type.getAST();

		HashMap<PrimitiveType.Code, String> primitives = new HashMap<PrimitiveType.Code, String>();

		primitives.put(PrimitiveType.BYTE, "Byte");
		primitives.put(PrimitiveType.SHORT, "Short");
		primitives.put(PrimitiveType.INT, "Integer");
		primitives.put(PrimitiveType.LONG, "Long");
		primitives.put(PrimitiveType.FLOAT, "Float");
		primitives.put(PrimitiveType.DOUBLE, "Double");
		primitives.put(PrimitiveType.CHAR, "Char");
		primitives.put(PrimitiveType.BOOLEAN, "Boolean");
		
		String boxedName = primitives.get(type.getPrimitiveTypeCode());
		return ast.newSimpleType(ast.newSimpleName(boxedName));
	}

	public static Type build(AST ast, ITypeBinding binding)
	{
		if (binding.isClass())
			return ast.newSimpleType(ast.newName(binding.getQualifiedName()));

		if (binding.isPrimitive())
			return ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));

		// TODO EType.build(AST ast, ITypeBinding binding)
		System.err.println("EType.build(AST ast, ITypeBinding binding)");
		return null;
	}
}
