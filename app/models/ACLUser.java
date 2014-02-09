package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class ACLUser extends Model {
	
	public String sourcename;
	public String login;
	public String fullname;
	
	@ManyToOne()
	public ACLGroup group;
	
	public ACLUser(ACLGroup group, String sourcename, String login, String fullname) {
		this.group = group;
		this.sourcename = sourcename;
		this.login = login;
		this.fullname = fullname;
	}
	
}
