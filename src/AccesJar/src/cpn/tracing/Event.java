package cpn.tracing;

public class Event{
    private String stream;
    private int time;
    private Object value;
    
    public Event(){
        setStream(null);
        setTime(-1);
        setValue(null);
    }

    public String toString() {
    	return "{" + getStream() + ", " + Integer.valueOf(getTime()).toString() + ", " + getValue().toString() + "}";
    }
    
    public boolean equals(Event te2) {
		return getStream().equals(te2) && te2.getTime() == getTime() && te2.getValue().equals(getValue());
    }

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public String getStream() {
		return stream;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
}