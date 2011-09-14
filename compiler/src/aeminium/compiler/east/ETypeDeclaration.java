package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import aeminium.compiler.east.EAST;

import aeminium.compiler.east.EAbstractTypeDeclaration;
import aeminium.compiler.east.EFieldDeclaration;
import aeminium.compiler.east.EMethodDeclaration;

public class ETypeDeclaration extends EAbstractTypeDeclaration
{
	TypeDeclaration origin;
	List<EFieldDeclaration> fields;
	List<EMethodDeclaration> methods;

	ETypeDeclaration(TypeDeclaration origin)
	{
		this.origin = origin;

		if (origin.isInterface())
			System.err.println("TODO: Interfaces are not supported yet.");

		this.fields = new ArrayList<EFieldDeclaration>();
		for (Object field : origin.getFields())
			this.fields.add(EAST.extend((FieldDeclaration) field));

		this.methods = new ArrayList<EMethodDeclaration>();
		for (Object method : origin.getMethods())
			this.methods.add(EAST.extend((MethodDeclaration) method));
	}

	public void translate(List<CompilationUnit> cus)
	{
		// TODO
	}
}
