import java.util.LinkedList;
import java.util.List;


public class Calendar {

	private final String owner; //this should be substituted by a class parameter of type principal
	private List<Event> eventList;
	private List<Event> requestEvents;
	private List<Event> pendingEvents;
	private List<Event> canceledEvents;
	private List<Event> deletedEvents;

	public Calendar(String owner) {
		this.owner = owner;
		this.eventList = new LinkedList<Event>();
		this.requestEvents = new LinkedList<Event>();
		this.pendingEvents = new LinkedList<Event>();
		this.canceledEvents = new LinkedList<Event>();
		this.deletedEvents = new LinkedList<Event>();
	}

	public String getOwner() {
		return owner;
	}

	public List<Event> getEventList() {
		return eventList;
	}

	public void addEvent(Event e){
		this.eventList.add(e);
	}

	public void addRequestEvent(Event e){
		this.requestEvents.add(e);
	}

	public void addPendingEvent(Event e){
		this.pendingEvents.add(e);
	}

	public void deleteEvent(int id){
		for (Event e : this.eventList) {
			if (e.getId()==id) this.eventList.remove(e);
		}
	}

	public String acceptPendingEvent(int id){
		Event se=null;
		for (Event e : this.pendingEvents) {
			if (e.getId()==id) se=e;
		}
		this.pendingEvents.remove(se);
		this.eventList.add(se);
		return se.getCreator();
	}

	public void requestedEventAccepted(int id){
		Event se=null;
		for (Event e : this.requestEvents) {
			if (e.getId()==id) se=e;
		}
		this.requestEvents.remove(se);
		this.eventList.add(se);
	}

	public boolean rejectOnConflict(Event ereq){
		for (Event e : this.eventList) {
			int reqStart = ereq.getTime().getHour()*60+ereq.getTime().getMinute();
			int eStart = e.getTime().getHour()*60+e.getTime().getMinute();
			int eFinish = eStart+ e.getDuration();
			if (e.getAutoReject() && e.getDate().getYear()==ereq.getDate().getYear() && 
					e.getDate().getMonth()==ereq.getDate().getMonth() &&
					e.getDate().getDay()==ereq.getDate().getDay() && 
					eStart<=reqStart && reqStart<=eFinish){
				return true;
			}
		}
		return false;
	}

	public List<String> cancelEvent(int id){
		for (Event e : this.eventList) {
			if (e.getId()==id){
				this.eventList.remove(e);
				this.canceledEvents.add(e);
				return e.getSharedBetween();
			}
		}
		return null;
	}

	public void deletePastSharedEvents(String user, Date d1){
		List<Event> l = new LinkedList<Event>();
		for (Event e : this.eventList) {
			if (e.getSharedBetween()!= null && e.getSharedBetween().contains(user)){
				Date d2 = e.getDate();
				if (d2.getYear()<d1.getYear() || 
						(d2.getYear()==d1.getYear() && d2.getMonth()<d1.getMonth())
						|| (d2.getYear()==d1.getYear() && d2.getMonth()==d1.getMonth()
						&& d2.getDay()<d1.getDay())){
					l.add(e);
				}
			}
		}
		this.eventList.removeAll(l);
		this.deletedEvents.addAll(l);
	}


	@Override
	public String toString() {
		String output=null;
		for (Event e : this.eventList) {
			if (output==null) output=e.toString()+ "\n";
			else{
				output = output + e.toString() + "\n";
			}
		}
		return output;
	}

}
