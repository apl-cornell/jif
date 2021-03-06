/*
 Message logging to file (echoed also to console)
 */

import java.io.*;
import java.text.*;
import java.util.*;

class BankLog extends Log {
    private LogMessage [] messages;   // Have to use an array rather than a vector because vectors are synchronized

    public BankLog(String basename) throws IOException {
	super(basename);
	messages = new LogMessage[1000];
    }

    private void appendMessage(LogMessage msg) {
	if (sequenceNumber < (messages.length - 1)) {
	    // there's room in the current array
	    messages[sequenceNumber] = msg;
	} else {
	    // need to grow the array
	    LogMessage [] newMessages = new LogMessage[sequenceNumber + 500];
	    for (int i=0; i<messages.length; i++) {
		newMessages[i] = messages[i];
	    }
	    messages = newMessages;
	}
	sequenceNumber++;
    }

    public synchronized void logMessage(LogMessage msg) {
	try {
	    System.out.println(basename + " LOG: " + msg);
	    String timestamp = DateFormat.getDateTimeInstance().format(new Date());
	    msg.setTime(timestamp);
	    msg.setSeq(sequenceNumber);
	    logfile.write("" + sequenceNumber + timestamp +": " + msg + "\n");
	    logfile.flush();
	    appendMessage(msg);
	} catch(IOException e) {
	    fail("Log file inaccessible:" + e);
	}
    }

    public boolean available(int i) {
	return (!(messages[i] == null));
    }

    public LogMessage getMessageAt(int seqNumber) {
	LogMessage msg;
	msg = (LogMessage) messages[seqNumber];
	return msg;
    }
}
