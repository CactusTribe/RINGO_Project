public enum PrefixMsg{
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
	ACKD("ACKD");

	private String name = "";

	PrefixMsg(String name){
		this.name = name;	
	}

	public String getName(){
		return name;
	}

	public static PrefixMsg getPrefix(String str){

		for(PrefixMsg pref : PrefixMsg.values()){

      if(PrefixMsg.WELC.getName().equals(str))
        return PrefixMsg.WELC;

      else if(PrefixMsg.NEWC.getName().equals(str))
        return PrefixMsg.NEWC;

      else if(PrefixMsg.ACKC.getName().equals(str))
        return PrefixMsg.ACKC;

      else if(PrefixMsg.APPL.getName().equals(str))
        return PrefixMsg.APPL;

      else if(PrefixMsg.WHOS.getName().equals(str))
        return PrefixMsg.WHOS;

      else if(PrefixMsg.MEMB.getName().equals(str))
        return PrefixMsg.MEMB;

      else if(PrefixMsg.GBYE.getName().equals(str))
        return PrefixMsg.GBYE;

      else if(PrefixMsg.EYBG.getName().equals(str))
        return PrefixMsg.EYBG;

      else if(PrefixMsg.TEST.getName().equals(str))
        return PrefixMsg.TEST;

      else if(PrefixMsg.DOWN.getName().equals(str))
        return PrefixMsg.DOWN;

      else if(PrefixMsg.DUPL.getName().equals(str))
        return PrefixMsg.DUPL;

      else if(PrefixMsg.ACKD.getName().equals(str))
        return PrefixMsg.ACKD;
    }
    return null;
	}
}