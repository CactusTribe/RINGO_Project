/**
 * Enumeration ProtocoleToken
 * - Contient les tokens du protocole
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */

public enum ProtocoleToken{
	WELC("WELC"),
	NEWC("NEWC"),
	ACKC("ACKC"),
	APPL("APPL"),
	WHOS("WHOS"),
	MEMB("MEMB"),
	GBYE("GBYE"),
	EYBG("EYBG"),
	TEST("TEST"),
	DOWN("DOWN"),
	DUPL("DUPL"),
	ACKD("ACKD"),

	TCP("TCP"),
	UDP("UDP"),
	RECEIVED("RECEIVED"),
	SENT("SENT");

	private String name = "";

	ProtocoleToken(String name){
		this.name = name;	
	}
}