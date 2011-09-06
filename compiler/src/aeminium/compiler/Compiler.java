package aeminium.compiler;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class Compiler
{
	private String sourceDir;
	private String targetDir;

	Compiler(String sourceDir, String targetDir)
	{
		this.sourceDir = sourceDir;
		this.targetDir = targetDir;
	}

	/**
	 * Attempts to compile all files from sourceDir into targetDir
	 */
	public void compile()
	{
		this.compile(new File(this.sourceDir));
	}

	/**
	 * Attempts to compile all files in a (sub)-directory
	 * @param dir The directory path, relative to the directory where the compiler is being executed, to compile files from.
	 */
	private void compile(File dir)
	{
		File[] children = dir.listFiles();
		if (children != null)
			for (File child : children)
				this.compile(child);
		else
			this.parse(dir);
	}

	/**
	 * Parses a CompilationUnit (file) and creates the modified version in the target directory
	 * @param file The file that contains the compilation unit.
	 */
	private void parse(File file)
	{
		String source;

		try
		{
			source = readFileAsString(file);
		} catch (IOException e)
		{
			System.err.println("Failed to load file: "+ e);
			return;
		}

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(source.toCharArray());
		parser.setResolveBindings(true);

		CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		/* Modify the source */
		cu.accept(new AeminiumVisitor(this));
		
		addAeminiumImports(cu);

		this.saveCU(cu);
	}

	/**
	 * Adds the AeminiumHelper to the imports
	 */
	public void addAeminiumImports(CompilationUnit cu)
	{
		AST ast = cu.getAST();

		ImportDeclaration imp = ast.newImportDeclaration();
		imp.setName(ast.newName("aeminium.AeminiumHelper"));

		cu.imports().add(imp);
	}

	/**
	 * Saves a compilation unit to its correct path
	 * @param cu The compilation unit to be stored
	 */
	public void saveCU(CompilationUnit cu)
	{
		BufferedWriter writer = null;

		try
		{
			writer = new BufferedWriter(new FileWriter(this.getCUPath(cu)));
			writer.write(cu.toString());
		} catch (IOException e)
		{
			System.err.println("Failed to save compilation unit: " + e); 
		} finally
		{
			try
			{
				if ( writer != null)
					writer.close( );
			} catch ( IOException e)
			{
			}
		}
	}

	/**
	 * Obtains the CU's file path from it's package and types definitions
	 */ 
	private String getCUPath(CompilationUnit cu)
	{
		String path = this.targetDir;

		path += "/";
		path += cu.getPackage().getName().toString().replace('.', '/');
		path += "/";
	
		/*
			FIXME:
			if compilation unit has more than 1 defined this doesn't work
		 	need to loop over them and choose the ONE that has a "public" access
		*/
		path += ((TypeDeclaration) cu.types().get(0)).getName();
		path += ".java";
		
		return path;
	}

	/**
	 * Compiles Java code to AEminium (runtime) code.
	 * 
	 * Call as java -j Compiler.jar sourceDir targetDir
	 * @param args 
	 */
	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.out.println("java -j Compiler.jar sourceDir targetDir");
			return;
		}

		Compiler compiler = new Compiler(args[0], args[1]);
		compiler.compile();
	}

	/**
	 * Reads a file into a String
	 * @param filePath The path to the file
	 * @return the contents of the file
	 * @throws java.io.IOException
	 */
	private static String readFileAsString(File file)
		throws java.io.IOException
	{
	    byte[] buffer = new byte[(int) file.length()];
	    
	    BufferedInputStream f = null;
	    try
	    {
	        f = new BufferedInputStream(new FileInputStream(file));
	        f.read(buffer);
	    } finally
	    {
	        if (f != null)
	        {
	        	try
	        	{
	        		f.close();
	        	} catch (IOException ignored)
	        	{
	        	}
	    	}
	    }
	    
	    return new String(buffer);
	}
} 
