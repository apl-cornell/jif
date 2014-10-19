
public class Main {

	final public static void main(final String[] args) {

		//for the api's we use id's instead of events because it is
		//also a way to make sure that events are found/transfered to correct lists.

		System.out.println("Welcome to rif-Calendar!");

		Server srv = new Server();

		srv.createCalendar("Alice");
		srv.createCalendar("Bob");

		Event e = new Event(new Date(23, 8, 2014), new TimeOfDay(10, 30), 30, "Meeting with Chris", "Alice");
		srv.addEvent("Alice", e);
		srv.publicizeSlot("Alice", e.getId());
		e = new Event(new Date(16, 9, 2014), new TimeOfDay(16, 15), 60, "Doctor appointment", "Alice");
		e.setAutoReject(true);
		srv.addEvent("Alice", e);
		srv.publicizeSlot("Alice", e.getId());
		e = new Event(new Date(5, 10, 2014), new TimeOfDay(9, 30), 15, "Meeting with Chris", "Alice");
		srv.addEvent("Alice", e);

		e = new Event(new Date(15, 10, 2014), new TimeOfDay(14, 30), 30, "Go to barbershop", "Bob");
		srv.addEvent("Bob", e);
		e = new Event(new Date(18, 10, 2014), new TimeOfDay(15, 45), 15, "Buy present for John", "Bob");
		srv.addEvent("Bob", e);

		System.out.println("Calendar of Alice");
		srv.printCalendar("Alice");
		System.out.println("Calendar of Bob");
		srv.printCalendar("Bob");

		e = new Event(new Date(10, 11, 2014), new TimeOfDay(18, 10), 60, "Visit parents", "Bob");
		boolean b = srv.requestSharedEvent("Bob", "Alice", e);
		if (b) srv.acceptSharedEvent("Alice", e.getId());
		else System.out.println("Automatic rejection: "+e.toString()+"\n");
		Event e1=e;

		e = new Event(new Date(16, 9, 2014), new TimeOfDay(16, 45), 20, "Visit aunts", "Bob");
		b = srv.requestSharedEvent("Bob", "Alice", e);
		if (b) srv.acceptSharedEvent("Alice", e.getId());
		else System.out.println("Automatic rejection: "+e.toString()+"\n");

		e = new Event(new Date(20, 2, 2010), new TimeOfDay(8, 15), 120, "Go for cinema", "Bob");
		b = srv.requestSharedEvent("Bob", "Alice", e);
		if (b) srv.acceptSharedEvent("Alice", e.getId());
		else System.out.println("Automatic rejection: "+e.toString()+"\n");

		Event e2 = new Event(new Date(30, 1, 2015), new TimeOfDay(21, 30), 120, "Go to concert", "Bob");
		b = srv.requestSharedEvent("Bob", "Alice", e2);
		if (b) srv.acceptSharedEvent("Alice", e2.getId());
		else System.out.println("Automatic rejection: "+e.toString()+"\n");

		System.out.println("Calendar of Alice");
		srv.printCalendar("Alice");
		System.out.println("Calendar of Bob");
		srv.printCalendar("Bob");

		System.out.println("Canceling of "+e1.toString()+"\n");
		srv.cancelEvent("Alice", e1.getId());

		/*		System.out.println("Deleting past Alice event from Bob.\n");
		srv.deletePastSharedEvents("Alice", "Bob", new Date(20, 2, 2014));*/

		System.out.println("Calendar of Alice");
		srv.printCalendar("Alice");
		System.out.println("Calendar of Bob");
		srv.printCalendar("Bob");

		System.out.println("Public view of Alice");
		rifList<Event> l = srv.takePubView("Alice");
		System.out.println(l.toString());

		/*		System.out.println("Removing Alice from Bob's shared event.\n");
		srv.removeParticipantFromSharedEvent("Bob", "Alice", e2.getId());

		System.out.println("Calendar of Alice");
		srv.printCalendar("Alice");
		System.out.println("Calendar of Bob");
		srv.printCalendar("Bob");*/

	}

}
