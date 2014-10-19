

public class Calendar {

	private final String owner; //this should be substituted by a class parameter of type principal
	private rifList<Event> eventList;
	private rifList<Event> requestEvents;
	private rifList<Event> pendingEvents;
	private rifList<Event> canceledEvents;
	/*	private rifList<Event> deletedEvents;*/

	public Calendar(String owner) {
		this.owner = owner;
		this.eventList = new rifList<Event>();
		this.requestEvents = new rifList<Event>();
		this.pendingEvents = new rifList<Event>();
		this.canceledEvents = new rifList<Event>();
		/*this.deletedEvents = new rifList<Event>();*/	
	}

	public String getOwner() {
		return owner;
	}

	public rifList<Event> getEventList() {
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

	/*	public void deleteEvent(int id){
		int size=this.eventList.getSize();
		int i;
		Node<Event> n = this.eventList.getHead();
		for(i=0;i<size;i++){
			if (n.getData().getId()==id){
				this.eventList.remove(n.getData());
				this.deletedEvents.add(n.getData());
				break;
			}
			n=n.getNext();
		}
	}*/

	public String acceptPendingEvent(int id){
		int size=this.pendingEvents.getSize();
		int i;
		Node<Event> n = this.pendingEvents.getHead();
		Event se=null;
		for (i=0;i<size;i++) {
			if (n.getData().getId()==id) se=n.getData();
			n=n.getNext();
		}
		this.pendingEvents.remove(se);
		this.eventList.add(se);
		return se.getCreator();
	}

	public void requestedEventAccepted(int id){
		int size=this.requestEvents.getSize();
		int i;
		Node<Event> n = this.requestEvents.getHead();
		Event se=null;
		for (i=0;i<size;i++) {
			if (n.getData().getId()==id) se=n.getData();
			n=n.getNext();
		}
		this.requestEvents.remove(se);
		this.eventList.add(se);
	}

	public boolean rejectOnConflict(Event ereq){
		int size=this.eventList.getSize();
		int i;
		Node<Event> n = this.eventList.getHead();
		for (i=0;i<size;i++) {
			Event e=n.getData();
			int reqStart = ereq.getTime().getHour()*60+ereq.getTime().getMinute();
			int eStart = e.getTime().getHour()*60+e.getTime().getMinute();
			int eFinish = eStart+ e.getDuration();
			if (e.getAutoReject() && e.getDate().getYear()==ereq.getDate().getYear() && 
					e.getDate().getMonth()==ereq.getDate().getMonth() &&
					e.getDate().getDay()==ereq.getDate().getDay() && 
					eStart<=reqStart && reqStart<=eFinish){
				return true;
			}
			n=n.getNext();
		}
		return false;
	}

	public rifList<String> cancelEvent(int id){
		int size=this.eventList.getSize();
		int i;
		Node<Event> n = this.eventList.getHead();
		for (i=0;i<size;i++) {
			Event e=n.getData();
			if (e.getId()==id){
				this.eventList.remove(e);
				this.canceledEvents.add(new Event(e.getDate(), e.getTime(), e.getDuration(), e.getDescription(), e.getCreator()));
				return e.getSharedBetween();
			}
			n=n.getNext();
		}
		return null;
	}

	//applied only to events created and owned by just this user!
	public void publicizeSlot(int id){
		int size=this.eventList.getSize();
		int i;
		Node<Event> n = this.eventList.getHead();
		for (i=0;i<size;i++) {
			Event e=n.getData();
			if (e.getId()==id){
				Event newe = new Event(e.getDate(), e.getTime(), e.getDuration(), e.getDescription(), e.getCreator());
				newe.setPubSlot(true);
				this.eventList.remove(e);
				this.eventList.add(newe);
			}
			n=n.getNext();
		}
	}

	//applied only to events created and owned by just this user!
	public void hideSlot(int id){
		int size=this.eventList.getSize();
		int i;
		Node<Event> n = this.eventList.getHead();
		for (i=0;i<size;i++) {
			Event e=n.getData();
			if (e.getId()==id){
				Event newe = new Event(e.getDate(), e.getTime(), e.getDuration(), e.getDescription(), e.getCreator());
				newe.setPubSlot(false);
				this.eventList.remove(e);
				this.eventList.add(newe);
			}
			n=n.getNext();
		}
	}

	public rifList<Event> takePubView(){
		int size=this.eventList.getSize();
		int i;
		Node<Event> n = this.eventList.getHead();
		rifList<Event> l = new rifList<Event>();
		for (i=0;i<size;i++) {
			Event e=n.getData();
			if (e.getPubSlot()){
				Event newe = new Event(e.getDate(), e.getTime(), e.getDuration(), null, null);
				l.add(newe);
			}
			n=n.getNext();
		}
		return l;
	}

	/*	public void deletePastSharedEvents(String user, Date d1){
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
	}*/

	/*	public void removeParticipantFromSharedEvent(String user2, int id){
		for (Event e : this.eventList) {
			if (e.getId()==id){
				e.getSharedBetween().remove(user2);
				break;
			}
		}
	}*/

	@Override
	public String toString() {
		int size=this.eventList.getSize();
		int i;
		Node<Event> n = this.eventList.getHead();
		String output=null;
		for (i=0;i<size;i++) {
			Event e=n.getData();
			if (output==null) output=e.toString()+ "\n";
			else{
				output = output + e.toString() + "\n";
			}
			n=n.getNext();
		}
		return output;
	}

}
