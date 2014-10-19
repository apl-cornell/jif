
public class Slot {

	private Date date;
	private TimeOfDay time;
	private int duration;

	public Slot(Date date, TimeOfDay time, int duration) {
		this.date = date;
		this.time=time;
		this.duration=duration;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public TimeOfDay getTime() {
		return time;
	}

	public void setTime(TimeOfDay time) {
		this.time = time;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

}
