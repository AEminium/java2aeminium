package aeminium.compiler.signature;

public abstract class SignatureItem
{
	public abstract SignatureItem replace(DataGroup what, DataGroup with);

	/**
	 * True if any of the DataGroups referred start with scope
	 * @param scope
	 * @return
	 */
	public abstract boolean isLocalTo(DataGroup scope);
}
