import java.util.*;

public class Message{

	private PrefixMsg prefix = null;

	private String ip = new String(new char[15]);
	private String ip_succ = new String(new char[15]);
	private String ip_diff = new String(new char[15]);
	private short port = 0;
	private short port_succ = 0;
	private short port_diff = 0;
	private int idm = 0;
	private int id_trans = 0;
	private int id_app = 0;
	private int id = 0;
	private short size_mess = 0;
	private short size_nom = 0;
	private int num_mess = 0;
	private int no_mess = 0;
	private short size_content = 0;
	
	public Message(){

	}

	public Message(String mess){
		if(mess.length() <= 512){
			ArrayList<String> argv = new ArrayList<String>(Arrays.asList(mess.split("\\s+")));

			this.prefix = PrefixMsg.getPrefix(argv.get(0));
			this.ip = argv.get(1);
			this.ip_succ = argv.get(2);
			this.ip_diff = argv.get(3);
			this.port = (short)Integer.parseInt(argv.get(4));
			this.port_succ = (short)Integer.parseInt(argv.get(5));
			this.port_diff = (short)Integer.parseInt(argv.get(6));
			this.idm = Integer.parseInt(argv.get(7));
			this.id_trans = Integer.parseInt(argv.get(8));
			this.id_app = Integer.parseInt(argv.get(9));
			this.id = Integer.parseInt(argv.get(10));
			this.size_mess = (short)Integer.parseInt(argv.get(11));
			this.size_nom = (short)Integer.parseInt(argv.get(12));
			this.num_mess = Integer.parseInt(argv.get(13));
			this.no_mess = Integer.parseInt(argv.get(14));
			this.size_content = (short)Integer.parseInt(argv.get(15));
		}
	}

	public String toString(){
		String mess = String.format("%s %s %s %s %04d %04d %04d %08d %08d %08d %08d %03d %02d %08d %08d %03d\n",
			prefix,ip,ip_succ,ip_diff,port,port_succ,port_diff,
			idm,id_trans,id_app,id,size_mess,size_nom,num_mess,
			no_mess,size_content);

		return mess;
	}

	public void print(){
		String mess = String.format("#########################################\n"
				+" - TYPE : %s\n"
				+" - IP : %s\n"
				+" - IP_SUCC : %s\n"
				+" - IP_DIFF : %s\n"
				+" - PORT : %04d\n"
				+" - PORT_SUCC : %04d\n"
				+" - PORT_DIFF : %04d\n"
				+" - IDM : %08d\n"
				+" - ID_TRANS : %08d\n"
				+" - ID_APP : %08d\n"
				+" - ID : %08d\n"
				+" - SIZE_MESS : %03d\n"
				+" - SIZE_NOM : %02d\n"
				+" - NUM_MESS : %08d\n"
				+" - NO_MESS : %08d\n"
				+" - SIZE_CONTENT : %03d\n"
				+"#########################################\n",
				prefix,ip,ip_succ,ip_diff,port,port_succ,port_diff,
				idm,id_trans,id_app,id,size_mess,size_nom,num_mess,
				no_mess,size_content);
	
		System.out.println(mess);
	}
}