package aeminium.compiler.east;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
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

	public int optimize()
	{
		int sum = 0;
		
		for (ETypeDeclaration type : this.types)
			sum += type.optimize();
		
		return sum;
	}

	public void preTranslate()
	{
		for (ETypeDeclaration type : this.types)
			type.preTranslate();
	}

	@SuppressWarnings("unchecked")
	public void translate(ArrayList<CompilationUnit> out)
	{
		AST ast = this.original.getAST();
		
		CompilationUnit unit = ast.newCompilationUnit();
		
		unit.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, this.getOriginal().getPackage()));
		unit.imports().addAll(ASTNode.copySubtrees(ast, this.getOriginal().imports()));

		for (ETypeDeclaration type : this.types)
			unit.types().add(type.translate(out));

		out.add(unit);
	}
}
