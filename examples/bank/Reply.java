import java.lang.*;
import java.io.*;

class Reply implements Serializable {
    public final static int BALANCE = 0x00;
    public final static int INSUFFICIENT_FUNDS = 0x01;
    public final static int DENIED_DAILY_LIMIT = 0x02;
    public final static int ATM_DEPLETED = 0x04;

    private int msg;
    private double balance;

    public Reply(int msg, double balance) {
	this.msg = msg;
	this.balance = balance;
    }

    public String toString() {
	switch (msg) {
	case BALANCE : return ("Balance = "+(Double.toString(balance)));
	case INSUFFICIENT_FUNDS : return("Insufficient Funds ");
	case DENIED_DAILY_LIMIT : return("Exceeded daily limit");
	case ATM_DEPLETED : return ("ATM Depleted");
	default : return("Bad Reply");
	}
    }
}
