package aeminium.compiler;

import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;

import aeminium.compiler.Compiler;

import aeminium.compiler.east.ECompilationUnit;

public class Optimizer
{
	Compiler compiler;

	Optimizer(Compiler compiler)
	{
		this.compiler = compiler;
	}

	public ECompilationUnit optimize(ECompilationUnit unit)
	{
		return unit;
	}
}
