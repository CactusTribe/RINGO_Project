public class RINGO_Project{

	public static void main(String[] args){

		Machine m1 = new Machine("192.168.0.0", 5900, 6000, 7000);
		
		Message mess = new Message("WELC127.000.000.001127.000.000.002127.000.000.00360016002700000000000000000000000000000000000003040000000800000002056");
		mess.print();
		System.out.println(mess);
	}
}
