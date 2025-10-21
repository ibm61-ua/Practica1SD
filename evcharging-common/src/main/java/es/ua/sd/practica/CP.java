package es.ua.sd.practica;

public class CP {
	public String UID;
	public String Price;
	public String Location;
	public String State;
	public float KWHRequested = 0.f;
	public String driver = null;
	
	public CP(String uid, String price, String location, String state)
	{
		UID = uid; Price = price; Location = location; State = state;
	}
	
	
}
