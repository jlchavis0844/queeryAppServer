
public enum ServCmd {addUser("addUser"), kill("kill"), 
	login("login"), setLocation("setsLocation"), updateLocation("updateLocation"),
	updateSlider("updateSlider");
	
	private final String s;
	
	ServCmd(String s){
		this.s = s;
	}

	@Override
	public String toString(){
		return s;
	}
}
