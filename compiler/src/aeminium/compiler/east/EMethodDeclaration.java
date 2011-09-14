package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import aeminium.compiler.east.EAST;

import aeminium.compiler.east.ETypeDeclaration;
import aeminium.compiler.east.EMethodDeclaration;

public class EMethodDeclaration
{
	MethodDeclaration origin;

	EMethodDeclaration(MethodDeclaration origin)
	{
		this.origin = origin;

		/* TODO */
		System.out.println(origin.getName());
	}
}
