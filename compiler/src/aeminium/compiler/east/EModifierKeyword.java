package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

public class EModifierKeyword
{
	public static List<ModifierKeyword> fromFlags(int modifiers)
	{
		List<ModifierKeyword> list = new ArrayList<ModifierKeyword>();
		
		for (int i = 0 ; i < 32; i++)
		{
			int val = modifiers & ( 1 << i);
			if (val != 0)
				list.add(ModifierKeyword.fromFlagValue(val));
		}
		
		return list;
	}
}
