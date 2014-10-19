
public class rifList<T> {

	private Node<T> head;
	private Node<T> last;
	private int size;

	public rifList() {
		this.head = new Node<T>(null);
		this.last = this.head;
		this.size = 0;
	}

	@Override
	public String toString() {
		Node<T> n = head;
		int i=0;
		String output = "[";
		while (i<this.size) {
			output += n.getData().toString();
			n = n.getNext();
			i++;
			if (i<this.size){
				output += ",";
			}
		}
		output += "]";
		return output;
	}

	public void add(T data){
		this.last.setData(data);
		size++;
		Node<T> n = new Node<T>(null);
		this.last.setNext(n);
		this.last=n;
	}

	public void remove(T data){
		Node<T> n = this.head;
		int i=0;
		if (this.size==0) return;
		if (this.head.getData()==data){
			this.head=n.getNext();
			n.setNext(null);
			this.size--;
			return;
		}
		while (i<this.size-1){
			if (n.getNext().getData()==data){
				Node<T> m = n.getNext();
				n.setNext(n.getNext().getNext());
				m.setNext(null);
				this.size--;
				return;
			}
			n=n.getNext();
			i++;
		}
	}

	public int getSize(){
		return this.size;
	}

	public Node<T> getHead(){
		return this.head;
	}
}