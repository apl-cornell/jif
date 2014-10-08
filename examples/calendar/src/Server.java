import java.util.LinkedList;
import java.util.List;


public class Server {

	private List<Calendar> calList;

	public Server() {
		this.calList = new LinkedList<Calendar>();
	}

	public void createCalendar(String owner){
		Calendar cal = new Calendar(owner);
		this.calList.add(cal);
	}

	public void addEvent(String owner, Event e){
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(owner)) c.addEvent(e);
		}
	}

	public void deleteEvent(String owner, int id){
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(owner)) c.deleteEvent(id);
		}
	}

	public void printCalendar(String owner){
		Calendar cal=null;
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(owner)) cal=c;
		}
		System.out.println(cal.toString());
	}

	public boolean requestSharedEvent(String user1, String user2, Event e){
		Calendar cal1=null;
		Calendar cal2=null;
		List<String> l = new LinkedList<String>();
		l.add(user1);
		l.add(user2);
		e.setSharedBetween(l);
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(user1)) cal1=c;
			else if (c.getOwner().equals(user2)) cal2=c;
		}
		if (cal2.rejectOnConflict(e)) return false;
		cal1.addRequestEvent(e);
		cal2.addPendingEvent(e);
		return true;
	}

	public void acceptSharedEvent(String user2, int id){
		Calendar cal1=null;
		Calendar cal2=null;
		String user1=null;
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(user2)) cal2=c;
		}
		user1 = cal2.acceptPendingEvent(id);
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(user1)) cal1=c;
		}
		cal1.requestedEventAccepted(id);
	}

	public void cancelEvent(String user1, int id){
		Calendar cal=null;
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(user1)) cal=c;
		}
		List<String> l = cal.cancelEvent(id);
		if (l != null){
			for (String s : l) {
				if (!s.equals(user1)){
					for (Calendar c : this.calList) {
						if (c.getOwner().equals(s)) cal=c;
					}
					cal.cancelEvent(id);
				}
			}
		}

	}

	public void deletePastSharedEvents(String user1, String user2, Date d){
		Calendar cal=null;
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(user2)) cal=c;
		}
		cal.deletePastSharedEvents(user1, d);
	}

}
