/**
 * Enumeration AppToken
 * - Contient les tokens des applications installées
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */

public enum AppToken{
	DIFF("DIFF"),
	TRANS("TRANS");

	private String name = "";

	AppToken(String name){
		this.name = name;

		for(int i=0; i < (8 - name.length()); i++){
			this.name += "#";
		}
	}

	public String toString(){
		return this.name;
	}
}