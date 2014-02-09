package models;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import play.db.jpa.Model;

@Entity
public class ACLGroup extends Model {
	
	public String name;
	
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
	public List<ACLUser> users;
	
	public ACLGroup(String name) {
		this.name = name;
	}
	
}
