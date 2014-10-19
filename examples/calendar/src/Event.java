
public class Event {

	private static int counter=0;
	private int id;
	private Slot slot;
	private String description;
	private String creator;
	private boolean autoRejectOnConflict;
	private rifList<String> sharedBetween;
	private boolean pubSlot;

	public Event(Date date, TimeOfDay time, int duration, String description, String creator) {
		this.counter++;
		this.id=this.counter;
		this.slot = new Slot(date,time,duration);
		this.description = description;
		this.creator = creator;
		this.autoRejectOnConflict=false;
		this.sharedBetween=null;
		this.pubSlot=false;
	}

	public Date getDate() {
		return this.slot.getDate();
	}

	public void setDate(Date date) {
		this.slot.setDate(date);
	}

	public TimeOfDay getTime() {
		return this.slot.getTime();
	}

	public void setTime(TimeOfDay time) {
		this.slot.setTime(time);
	}

	public int getDuration() {
		return this.slot.getDuration();
	}

	public void setDuration(int duration) {
		this.slot.setDuration(duration);
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

	public rifList<String> getSharedBetween() {
		return sharedBetween;
	}

	public void setSharedBetween(rifList<String> sharedBetween) {
		this.sharedBetween = sharedBetween;
	}

	public void setPubSlot(boolean b){
		this.pubSlot=b;
	}

	public boolean getPubSlot(){
		return this.pubSlot;
	}

	@Override
	public String toString() {
		return "Event("+ id +") [date=" + this.slot.getDate() + ", time=" + this.slot.getTime() + ", duration="
				+ this.slot.getDuration() + "mins, description=" + description + ", creator="
				+ creator + "]";
	}

}
