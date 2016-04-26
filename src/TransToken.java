/**
 * Enumeration TransToken
 * - Contient les tokens de l'application transfert
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */

public enum TransToken{
	REQ("REQ"),
	ROK("ROK"),
	SEN("SEN");

	private String name = "";

	TransToken(String name){
		this.name = name;
	}

}