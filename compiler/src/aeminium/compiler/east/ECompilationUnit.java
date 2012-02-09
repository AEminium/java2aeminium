package aeminium.compiler.east;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ECompilationUnit extends EASTNode
{
	protected final ArrayList<ETypeDeclaration> types;
		
	public ECompilationUnit(EAST east, CompilationUnit original)
	{
		super(east, original);
		
		this.east.addOriginalCU(this);

		this.types = new ArrayList<ETypeDeclaration>();
		for (Object type : original.types())
			this.types.add(ETypeDeclaration.create(this.east, (TypeDeclaration) type, this));
	}

	/* Factory */
	public static ECompilationUnit create(EAST east, CompilationUnit original)
	{
		return new ECompilationUnit(east, original);
	}

	@Override
	public CompilationUnit getOriginal()
	{
		return (CompilationUnit) this.original;
	}

	@Override
	public void checkSignatures()
	{
		for (ETypeDeclaration type : this.types)
			type.checkSignatures();
	}

	public void checkDependencies()
	{
		for (ETypeDeclaration type : this.types)
			type.checkDependencies();
	}
}
