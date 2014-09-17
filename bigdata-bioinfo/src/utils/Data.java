package utils;
public class Data {
    private String name;
    private int genome_id;
    private int taxid;
    private String status;
    private String kingdom;
    private String group;
    private String subgroup;

    

    public String getName() {
		return name;
	}



	public int getGenome_id() {
		return genome_id;
	}



	public int getTaxid() {
		return taxid;
	}



	public String getStatus() {
		return status;
	}



	public String getKingdom() {
		return kingdom;
	}



	public String getGroup() {
		return group;
	}



	public String getSubgroup() {
		return subgroup;
	}



	public void setName(String name) {
		this.name = name;
	}



	public void setGenome_id(int genome_id) {
		this.genome_id = genome_id;
	}



	public void setTaxid(int taxid) {
		this.taxid = taxid;
	}



	public void setStatus(String status) {
		this.status = status;
	}



	public void setKingdom(String kingdom) {
		this.kingdom = kingdom;
	}



	public void setGroup(String group) {
		this.group = group;
	}



	public void setSubgroup(String subgroup) {
		this.subgroup = subgroup;
	}



	public String toString() {
        return String.format("name:%s", name);
    }
}