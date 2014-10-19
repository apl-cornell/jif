


public class Server {

	private rifList<Calendar> calList;

	public Server() {
		this.calList = new rifList<Calendar>();
	}

	public void createCalendar(String owner){
		Calendar cal = new Calendar(owner);
		this.calList.add(cal);
	}

	public void addEvent(String owner, Event e){
		int size=this.calList.getSize();
		int i;
		Node<Calendar> n = this.calList.getHead();
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(owner)) c.addEvent(e);
			n=n.getNext();
		}
	}

	/*	public void deleteEvent(String owner, int id){
		int size=this.calList.getSize();
		int i;
		Node<Calendar> n = this.calList.getHead();
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(owner)) c.deleteEvent(id);
			n=n.getNext();
		}
	}*/

	public void printCalendar(String owner){
		int size=this.calList.getSize();
		int i;
		Node<Calendar> n = this.calList.getHead();
		Calendar cal=null;
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(owner)) cal=c;
			n=n.getNext();
		}
		System.out.println(cal.toString());
	}

	//used for confidentiality downgrade
	public boolean requestSharedEvent(String user1, String user2, Event e){
		Calendar cal1=null;
		Calendar cal2=null;
		rifList<String> l = new rifList<String>();
		l.add(user1);
		l.add(user2);
		e.setSharedBetween(l);
		int size=this.calList.getSize();
		int i;
		Node<Calendar> n = this.calList.getHead();
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(user1)) cal1=c;
			else if (c.getOwner().equals(user2)) cal2=c;
			n=n.getNext();
		}
		if (cal2.rejectOnConflict(e)) return false;
		cal1.addRequestEvent(e);
		cal2.addPendingEvent(e);
		return true;
	}

	//used for integrity downgrade
	public void acceptSharedEvent(String user2, int id){
		Calendar cal1=null;
		Calendar cal2=null;
		String user1=null;
		int size=this.calList.getSize();
		int i;
		Node<Calendar> n = this.calList.getHead();
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(user2)) cal2=c;
			n=n.getNext();
		}
		user1 = cal2.acceptPendingEvent(id);
		n = this.calList.getHead();
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(user1)) cal1=c;
			n=n.getNext();
		}
		cal1.requestedEventAccepted(id);
	}

	//used for integrity upgrade
	public void cancelEvent(String user1, int id){
		Calendar cal=null;
		int size=this.calList.getSize();
		int i;
		Node<Calendar> n = this.calList.getHead();
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(user1)) cal=c;
			n=n.getNext();
		}
		rifList<String> l = cal.cancelEvent(id);
		if (l != null){
			int lsize=l.getSize();
			int li;
			Node<String> ln = l.getHead();
			for (li=0;li<lsize;li++) {
				String s=ln.getData();
				if (!s.equals(user1)){
					n = this.calList.getHead();
					for (i=0;i<size;i++) {
						Calendar c=n.getData();
						if (c.getOwner().equals(s)) cal=c;
						n=n.getNext();
					}
					cal.cancelEvent(id);
				}
				ln=ln.getNext();
			}
		}

	}

	//used for confidentiality downgrade
	public void publicizeSlot(String user1, int id){
		Calendar cal=null;
		int size=this.calList.getSize();
		int i;
		Node<Calendar> n = this.calList.getHead();
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(user1)) cal=c;
			n=n.getNext();
		}
		cal.publicizeSlot(id);
	}

	//used for confidentiality upgrade
	public void hideSlot(String user1, int id){
		Calendar cal=null;
		int size=this.calList.getSize();
		int i;
		Node<Calendar> n = this.calList.getHead();
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(user1)) cal=c;
			n=n.getNext();
		}
		cal.hideSlot(id);
	}

	public rifList<Event> takePubView(String user1){
		Calendar cal=null;
		int size=this.calList.getSize();
		int i;
		Node<Calendar> n = this.calList.getHead();
		for (i=0;i<size;i++) {
			Calendar c=n.getData();
			if (c.getOwner().equals(user1)) cal=c;
			n=n.getNext();
		}
		return cal.takePubView();
	}

	/*	//used for confidentiality upgrade
	public void deletePastSharedEvents(String user1, String user2, Date d){
		Calendar cal=null;
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(user2)) cal=c;
		}
		cal.deletePastSharedEvents(user1, d);
	}*/

	/*	//used for confidentiality upgrade
	public void removeParticipantFromSharedEvent(String user1, String user2, int id){
		Calendar cal=null;
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(user1)) cal=c;
		}
		cal.removeParticipantFromSharedEvent(user2, id);
		for (Calendar c : this.calList) {
			if (c.getOwner().equals(user2)) cal=c;
		}
		cal.deleteEvent(id);
	}*/

}
