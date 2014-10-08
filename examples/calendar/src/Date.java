
public class Date {

	private int day;
	private int month;
	private int year;

	public Date(int d, int m, int y) {
		this.day=d;
		this.month=m;
		this.year=y;
	}

	int getDay(){
		return this.day;
	}

	int getMonth(){
		return this.month;
	}

	int getYear(){
		return this.year;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public void setYear(int year) {
		this.year = year;
	}

	@Override
	public String toString() {
		return day + "-" + month + "-" + year;
	}

}
