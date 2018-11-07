package utils;

public class SamplePerson {
	private String name;
	private String tel;
	private SampleAddress address;
	private Boolean verified;
	private Boolean known;
	public SamplePerson() {
	}
	public SamplePerson(String name, String tel, SampleAddress address, Boolean verified, Boolean known) {
		super();
		this.name = name;
		this.tel = tel;
		this.address = address;
		this.verified = verified;
		this.known = known;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTel() {
		return tel;
	}
	public void setTel(String tel) {
		this.tel = tel;
	}
	public SampleAddress getAddress() {
		return address;
	}
	public void setAddress(SampleAddress address) {
		this.address = address;
	}
	public Boolean getVerified() {
		return verified;
	}
	public void setVerified(Boolean verified) {
		this.verified = verified;
	}
	public Boolean getKnown() {
		return known;
	}
	public void setKnown(Boolean known) {
		this.known = known;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((known == null) ? 0 : known.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((tel == null) ? 0 : tel.hashCode());
		result = prime * result + ((verified == null) ? 0 : verified.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SamplePerson other = (SamplePerson) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (known == null) {
			if (other.known != null)
				return false;
		} else if (!known.equals(other.known))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (tel == null) {
			if (other.tel != null)
				return false;
		} else if (!tel.equals(other.tel))
			return false;
		if (verified == null) {
			if (other.verified != null)
				return false;
		} else if (!verified.equals(other.verified))
			return false;
		return true;
	}

}
