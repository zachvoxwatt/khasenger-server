package res;

import java.util.UUID;

public class ClientUser
{
	private UUID accessToken;
	private String name;
	
	public ClientUser() {}
	public ClientUser(String name, UUID tok) { this.name = name; this.accessToken = tok; }
	
	public void setName(String n) { this.name = n; }
	public void assignToken(UUID token) { this.accessToken = token; }
	
	public String getName() { return this.name; }
	public UUID getAccessToken() { return this.accessToken; }
}