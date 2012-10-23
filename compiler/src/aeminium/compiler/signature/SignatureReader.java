package aeminium.compiler.signature;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import aeminium.compiler.east.EAST;

public class SignatureReader
{
	public static final String FILE_NAME = "signatures/signatures";
	public static final int MAX_PARAMS = 256;

	public HashMap<String, Signature> signatures;

	public final DataGroup thisDataGroup;
	public final DataGroup returnDataGroup;
	public final DataGroup externalDataGroup;
	public final DataGroup parameterDataGroups[];
	
	public SignatureReader(EAST east) throws FileNotFoundException
	{
		this.signatures = new HashMap<String, Signature>();
		
		this.externalDataGroup = east.getExternalDataGroup();

		this.thisDataGroup = new SimpleDataGroup("this");
		this.returnDataGroup = new SimpleDataGroup("return");
		this.parameterDataGroups = new SimpleDataGroup[SignatureReader.MAX_PARAMS];
		
		for (int i = 0; i < SignatureReader.MAX_PARAMS; i++)
			this.parameterDataGroups[i] = new SimpleDataGroup("param " + i);
		
		Scanner s = null;
		
        try
        {
            s = new Scanner(new BufferedReader(new FileReader(SignatureReader.FILE_NAME)));
            int n = s.nextInt();
            s.nextLine();
            
            for (int i = 0; i < n; i++)
            {
            	String key = s.nextLine();

            	int items = s.nextInt();
            	s.nextLine();
            	
            	Signature sig = new Signature();

            	for (int j = 0; j < items; j++)
            	{
            		char type = s.next().charAt(0);
            		
            		switch (type)
            		{
	            		case 'R':
	            			sig.addItem(new SignatureItemRead(this.readDataGroup(s)));
	            			s.nextLine();
            			break;

	            		case 'W':
	            			sig.addItem(new SignatureItemWrite(this.readDataGroup(s)));	            		
	            			s.nextLine();
	            		break;
	            		
	            		case 'M':
	            			DataGroup to = this.readDataGroup(s);
	            			DataGroup from = this.readDataGroup(s);
	            			s.nextLine();
	            			
	            			sig.addItem(new SignatureItemMerge(to, from));
            			break;
            			
	            		default:
	            			System.err.println("ERROR: invalid signature file");
            		}
            	}
            	
            	s.nextLine();
            	this.signatures.put(key, sig);
            }
        } finally
        {
            if (s != null)
            	s.close();
        }
	}

	private DataGroup readDataGroup(Scanner s)
	{
		switch (s.next().charAt(0))
		{
			case 'T':
				return this.thisDataGroup;
			
			case 'R':
				return this.returnDataGroup;
			
			case 'E':
				return this.externalDataGroup;
			
			case 'P':
				return this.parameterDataGroups[s.nextInt()];
		
			default:
				System.err.println("ERROR: invalid datagroup");
				return null;
		}
	}
	
	public Signature getSignature(String key, DataGroup dgRet, DataGroup dgExpr, ArrayList<DataGroup> dgsArgs)
	{
		if (!this.signatures.containsKey(key))
		{
			System.err.println("WARNING: using default signature for: " + key);
			return getDefaultSignature(dgRet, dgExpr, dgsArgs);
		}
		
		Signature sig = this.signatures.get(key);
		Signature real = new Signature();
		
		for (SignatureItem item : sig.getItems())
		{
			SignatureItem _item = item;
			
			_item = _item.replace(this.returnDataGroup, dgRet);
			_item = _item.replace(this.thisDataGroup, dgExpr);

			for (int i = 0; i < dgsArgs.size(); i++)
				_item = _item.replace(this.parameterDataGroups[i], dgsArgs.get(i)); 
			
			real.addItem(_item);
		}
		
		return real;
	}
	
	public Signature getDefaultSignature(DataGroup dgRet, DataGroup dgExpr, ArrayList<DataGroup> dgsArgs)
	{		
		/* Conservative approach */
		Signature sig = new Signature();
		
		sig.addItem(new SignatureItemRead(this.externalDataGroup));
		sig.addItem(new SignatureItemWrite(this.externalDataGroup));
		
		if (dgExpr != null)
			sig.addItem(new SignatureItemRead(dgExpr));

		for (DataGroup arg : dgsArgs)
			sig.addItem(new SignatureItemRead(arg));
		
		if (dgRet != null)
			sig.addItem(new SignatureItemWrite(dgRet));
		
		return sig;
	}
}
