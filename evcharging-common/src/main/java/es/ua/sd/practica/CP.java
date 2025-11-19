package es.ua.sd.practica;
import com.google.gson.annotations.SerializedName;

public class CP {
	@SerializedName("id")
	public String UID;
	@SerializedName("price")
	public String Price;
	@SerializedName("location")
	public String Location;
	@SerializedName("status")
	public String State;
	@SerializedName("temp")
	public double temperature = 0.0;
	public boolean alert = false;
	public float KWHRequested = 0.f;
	public String driver = null;
	
	public CP(String uid, String price, String location, String state)
	{
		UID = uid; Price = price; Location = location; State = state;
	}
	
	public String toString()
	{
		String cp;
		
		cp = UID + ";" + Price + ";" + Location;
		
		return cp;
	}
	
	
}
