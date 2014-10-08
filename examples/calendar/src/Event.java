import java.util.List;


public class Event {

	private static int counter=0;
	private int id;
	private Date date;
	private TimeOfDay time;
	private int duration;
	private String description;
	private String creator;
	private boolean autoRejectOnConflict;
	private List<String> sharedBetween;

	public Event(Date date, TimeOfDay time, int duration, String description, String creator) {
		this.counter++;
		this.id=this.counter;
		this.date = date;
		this.time=time;
		this.duration=duration;
		this.description = description;
		this.creator = creator;
		this.autoRejectOnConflict=false;
		this.sharedBetween=null;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreator() {
		return creator;
	}

	public int getId(){
		return this.id;
	}

	public void setAutoReject(boolean b){
		this.autoRejectOnConflict=b;
	}

	public boolean getAutoReject(){
		return this.autoRejectOnConflict;
	}

	public List<String> getSharedBetween() {
		return sharedBetween;
	}

	public void setSharedBetween(List<String> sharedBetween) {
		this.sharedBetween = sharedBetween;
	}

	@Override
	public String toString() {
		return "Event("+ id +") [date=" + date + ", time=" + time + ", duration="
				+ duration + "mins, description=" + description + ", creator="
				+ creator + "]";
	}

}
