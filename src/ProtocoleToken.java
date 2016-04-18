public enum ProtocoleToken{
	TCP("TCP"),
	UDP("UDP"),
	RECEIVED("RECEIVED"),
	SENT("SENT");

	private String name = "";

	ProtocoleToken(String name){
		this.name = name;	
	}

	public String getName(){
		return name;
	}

	public static ProtocoleToken getToken(String str){

		for(ProtocoleToken pref : ProtocoleToken.values()){

      if(ProtocoleToken.TCP.getName().equals(str))
        return ProtocoleToken.TCP;

      else if(ProtocoleToken.UDP.getName().equals(str))
        return ProtocoleToken.UDP;

      else if(ProtocoleToken.RECEIVED.getName().equals(str))
        return ProtocoleToken.RECEIVED;

      else if(ProtocoleToken.SENT.getName().equals(str))
        return ProtocoleToken.SENT;
    }
    return null;
	}
}