package utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nuccore.NuccoreThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ui.AppLabels;
import ui.MainWindow;
import app.App;

/**
 * @author toussah
 *	Public class fetching the URLs and info of every genomes. 
 */
public class FetchURLs 
{
	
	/**
	 * 
	 */
	private String url; //The url of the database.
	private String genomeVar; //The string value of the original JSON from the database.
	private JSONObject genomeJson; //The original JSON from the database.
	private JSONArray organizedJson; //A sorted representation of the original JSON, with the chromosomes' urls and with a tree with (kingdom/group/subgroup).
	
	private MainWindow w = MainWindow.getInstance();
	
	private int nb_proc;
	
	private static float val = 0;
	
	public JSONArray getOrganizedJson() {
		return organizedJson;
	}
	
	public void setOrganizedJson(JSONArray org) {
		this.organizedJson = org;
	}
	
	public void setOrganizedJson(String filepath) throws FileNotFoundException
	{
		File genomes = new File(filepath);
        Scanner s = new Scanner(genomes);
        String content = s.useDelimiter("\\Z").next();
        s.close();
        this.organizedJson = new JSONArray(content);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public JSONObject getGenomeJson() {
		return genomeJson;
	}

	public void setGenomeJson(JSONObject genomeJson) {
		this.genomeJson = genomeJson;
	}
	
	public void setGenomeJson(String input) throws IOException 
	{
		File genomes = new File(input);
        Scanner s = new Scanner(genomes);
        String content = s.useDelimiter("\\Z").next();
        s.close();
        this.genomeVar = "{\"genomes\":" + content + "}";
        this.genomeJson = new JSONObject(this.genomeVar);
	}

	public FetchURLs(String url) throws IOException
	{
		this.url = url;
		this.organizedJson = new JSONArray();
		this.genomeJson = new JSONObject();
		this.genomeVar = new String("");
	}
	
	public void init(String urlComplete) throws Exception 
	{
	    BufferedReader reader = null;
	    try 
	    {
	        URL genomeBrowse = new URL(url + urlComplete);
	        reader = new BufferedReader(new InputStreamReader(genomeBrowse.openStream()));
	        StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1){
	            buffer.append(chars, 0, read); 
	        }
	        Pattern p = Pattern.compile(".*?projects = (.*?\\Q]\\E);.*", Pattern.DOTALL);
	        Matcher m = p.matcher(buffer.toString());
	        if(m.matches())
	        {
	        	genomeVar = "{\"genomes\":" + m.group(1) + "}";
	        }
	        genomeJson = new JSONObject(this.genomeVar);
	    } 
	    finally 
	    {
	        if (reader != null)
	            reader.close();
	    }
	}

	
	//Fetches the genome JSON from the database.
	public void init2(String urlComplete) throws IOException
	{
		URL genomeBrowse = new URL(url + urlComplete);
        BufferedReader in = new BufferedReader(new InputStreamReader(genomeBrowse.openStream()));
        Pattern p = Pattern.compile("genomes = (.*);");
        String inputLine;
        while ((inputLine = in.readLine()) != null){
        	
        	Matcher m = p.matcher(inputLine);
        	
        	if(m.matches())
        	{
        		genomeVar = "{\"genomes\":" + m.group(1) + "}";
        		break;
        	}
        }
        in.close();
        genomeJson = new JSONObject(this.genomeVar);
	}
	
	//Checks if a value exists in the different JSONObject (dictionnary) of a JSONArray, given its supposed key.
	//Returns the object if it exists, null otherwise.
	public JSONObject exists(JSONArray arr, String key, String value)
	{
		JSONObject res = null;
		int i;
		int len = arr.length();
		for(i = 0; i < len; i++)
		{
			JSONObject genome = arr.getJSONObject(i);
			if(value.equals(genome.getString(key)))
			{
				res = genome;
				break;
			}
		}
		return res;
	}
	
	//Sorts the JSON original object of the database, nesting the data to give it a correct tree view.
	public void sortJson(String output) throws IOException
	{
		int i;
		JSONArray genomes = genomeJson.getJSONArray("genomes");
		int len = genomes.length();
		int tccount = 0;
		int tocount = 0;
		int tpcount = 0;
		JSONObject genome;
		String kingdom;
		String group;
		String subgroup;
		String bioproj;
		String name;
		String status;
		int genid;
		int taxid;
		int assemblyID;
		int ccount;
		int ocount;
		int pcount;
		List<String> chromosomes;
		List<String> plasmids;
		List<String> chloroplasts;
		List<String> mitochondrions;
		
		w.add_to_log("Number of genomes fetched : " + len);
		for (i = 0; i < len; i++)
		{
			genome = genomes.getJSONObject(i);
			kingdom = genome.getString("kingdom");
			group = genome.getString("group");
			subgroup = genome.getString("subgroup");
			bioproj = genome.getString("project_acc");
			genid = genome.getInt("genome_id");
			taxid = genome.getInt("taxid");
			assemblyID = 0;
			ccount = genome.getInt("chromosome_count");
			ocount = genome.getInt("organelle_count");
			pcount = genome.getInt("plasmid_count");
			name = genome.getString("name");
			status = genome.getString("status");
			
			JSONArray assemblyIDs = genome.getJSONArray("assembly_ids");
			if(assemblyIDs.length() > 0)
			{
				assemblyID = assemblyIDs.getInt(0);	
			}
			chromosomes = new ArrayList<String>();
			plasmids = new ArrayList<String>();
			chloroplasts = new ArrayList<String>();
			mitochondrions = new ArrayList<String>();
			
			if(kingdom.equals("Bacteria") || kingdom.equals("Archaea"))
			{
				JSONArray Jchromosomes = genome.optJSONArray("chromosome_accs");
				JSONArray Jplasmids = genome.optJSONArray("plasmid_accs");
				if(Jchromosomes != null)
				{
					int j;
					int Clen = Jchromosomes.length();
					for(j = 0; j < Clen; j++)
					{
						String acc = Jchromosomes.getJSONObject(j).getString("acc");
						if(acc.startsWith("NC"))
							chromosomes.add(acc);
					}
				}
				if(Jplasmids != null)
				{
					int j;
					int Plen = Jplasmids.length();
					for(j = 0; j < Plen; j++)
					{
						String acc = Jplasmids.getJSONObject(j).getString("acc");
						if(acc.startsWith("NC"))
							plasmids.add(acc);
					}
				}
			}
			
			
			String kingdomPath = "Genomes" + App.FILE_SEP + kingdom.replaceAll("[\\/\\<\\>\\?|\\*\"\\:]", " ");
			String groupPath = kingdomPath + App.FILE_SEP + group.replaceAll("[\\/\\<\\>\\?|\\*\"\\:]", " ");
			String subgroupPath = groupPath + App.FILE_SEP + subgroup.replaceAll("[\\/\\<\\>\\?|\\*\"\\:]", " ");
			String genomePath = subgroupPath + App.FILE_SEP +  name.replaceAll("[\\/\\<\\>\\?|\\*\"\\:]", " ");
			
			
			if(status.equals("Complete") || (kingdom.equals("Eukaryotes") && status.equals("Chromosomes")))
			{
				JSONObject king_obj = exists(organizedJson, "kingdom", kingdom);
				if(king_obj == null)
				{
					king_obj = new JSONObject();
					king_obj.put("kingdom", kingdom);
					king_obj.put("groups", new JSONArray());
					organizedJson.put(king_obj);
				}
				
				JSONObject group_obj = exists(king_obj.getJSONArray("groups"), "group", group);
				if(group_obj == null)
				{
					group_obj = new JSONObject();
					group_obj.put("group", group);
					group_obj.put("subgroups", new JSONArray());
					king_obj.accumulate("groups", group_obj);
				}
				
				JSONObject subgroup_obj = exists(group_obj.getJSONArray("subgroups"), "subgroup", subgroup);
				if(subgroup_obj == null)
				{
					subgroup_obj = new JSONObject();
					subgroup_obj.put("subgroup", subgroup);
					subgroup_obj.put("genomes", new JSONArray());
					group_obj.accumulate("subgroups", subgroup_obj);
				}
				
				JSONObject genome_obj = exists(subgroup_obj.getJSONArray("genomes"), "name", name);
				if(genome_obj == null)
				{
					genome_obj = new JSONObject();
					genome_obj.put("name", name);
					genome_obj.put("genome_id", genid);
					genome_obj.put("bioproj", bioproj);
					genome_obj.put("taxid", taxid);
					genome_obj.put("assembly_id", assemblyID);
					genome_obj.put("status", status);
					genome_obj.put("chromosome_count", ccount);
					genome_obj.put("organelle_count", ocount);
					genome_obj.put("plasmid_count", pcount);
					genome_obj.put("chromosomes", chromosomes);
					genome_obj.put("plasmids", plasmids);
					genome_obj.put("mitochondrions", mitochondrions);
					genome_obj.put("chloroplasts", chloroplasts);
					genome_obj.put("path", genomePath.replace("\\", "/"));
					subgroup_obj.accumulate("genomes", genome_obj);
				}
			}
			tccount += ccount;
			tocount += ocount;
			tpcount += pcount;
		}
		w.add_to_log("Count of chromosomes : " + tccount);
		w.add_to_log("Count of organelle : " + tocount);
		w.add_to_log("Count of plasmids : " + tpcount);
		w.add_to_log(AppLabels.APP_DONE);
		Writer writer = null;
		try 
		{
		    writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream(output), "utf-8"));
		    writer.write(organizedJson.toString(4));
		} 
		catch (IOException ex) 
		{
		  // report
		}
		finally 
		{
		   try {writer.close();} catch (Exception ex) {}
		}
	}
	
	//Works.
	public int length()
	{
		int i,j,k;
		int len = organizedJson.length();
		int size = 0;
		for(i = 0; i < len; i++)
		{
			JSONArray groups = organizedJson.getJSONObject(i).getJSONArray("groups");
			int leng = groups.length();
			for(j = 0; j < leng; j++)
			{
				JSONArray subgroups = groups.getJSONObject(j).getJSONArray("subgroups");
				int lens = subgroups.length();
				for(k = 0; k < lens; k++)
				{
					JSONArray genomes = subgroups.getJSONObject(k).getJSONArray("genomes");
					int lenG = genomes.length();
					size += lenG;
				}
			}
		}
		return size;
	}
	
	public void completeUrls() throws JSONException, IOException, InterruptedException
	{
		int i,j,k,l;
		int len = organizedJson.length();
		for(i = 0; i < len; i++)
		{
			String kingName = organizedJson.getJSONObject(i).getString("kingdom");
			JSONArray groups = organizedJson.getJSONObject(i).getJSONArray("groups");
			int leng = groups.length();
			for(j = 0; j < leng; j++)
			{
				JSONArray subgroups = groups.getJSONObject(j).getJSONArray("subgroups");
				int lens = subgroups.length();
				for(k = 0; k < lens; k++)
				{
					JSONArray genomes = subgroups.getJSONObject(k).getJSONArray("genomes");
					int lenc = genomes.length();
					for(l = 0; l < lenc; l++)
					{
						JSONObject genome = genomes.getJSONObject(l);
						System.out.println(genome.get("name"));
						int clen = genome.getJSONArray("chromosomes").length();
						int plen = genome.getJSONArray("plasmids").length();
						int olen = genome.getJSONArray("mitochondrions").length() + genome.getJSONArray("chloroplasts").length();
						if(clen == 0 && plen == 0 && olen == 0)
						{
							if(kingName == "Viruses" || kingName == "Viroids")
							{
								List<String> urlList = getVirusURL(genome.getString("bioproj"));
								genome.put("chromosomes", urlList);
							}
							else
							{
								Map<String, List<String>> urlMap = getURLs(genome.getInt("assembly_id"));
								genome.put("chromosomes", urlMap.get("chromosomes"));
								genome.put("plasmids", urlMap.get("plasmids"));
								genome.put("chloroplasts", urlMap.get("chloroplasts"));
								genome.put("mitochondrions", urlMap.get("mitochondrions"));
							}
							
							Writer writer = null;
							try 
							{
							    writer = new BufferedWriter(new OutputStreamWriter(
							          new FileOutputStream("genomes5.json"), "utf-8"));
							    writer.write(organizedJson.toString(4));
							} 
							catch (IOException ex) 
							{
							  // report
							}
							finally 
							{
							   try {writer.close();} catch (Exception ex) {}
							}
						}
					}
				}
			}
		}
	}
	
	public void initThread(String output) throws JSONException, IOException, InterruptedException
	{
		int i;
		int len = organizedJson.length();
		for(i = 0; i < len; i++)
		{
			completeUrls(organizedJson.getJSONObject(i), output);
		}
	}
	
	public void completeUrls(JSONObject kingdom, String output) throws JSONException, IOException, InterruptedException
	{
		int i;
		int j;
		int k;
		JSONArray groups = kingdom.getJSONArray("groups");
		String kingName = kingdom.getString("kingdom");
		int leng = groups.length();
		for(i = 0; i < leng; i++)
		{
			JSONArray subgroups = groups.getJSONObject(i).getJSONArray("subgroups");
			int lens = subgroups.length();
			for(j = 0; j < lens; j++)
			{
				JSONArray genomes = subgroups.getJSONObject(j).getJSONArray("genomes");
				int lenc = genomes.length();
				for(k = 0; k < lenc; k++)
				{
					JSONObject genome = genomes.getJSONObject(k);
					int clen = genome.getJSONArray("chromosomes").length();
					int plen = genome.getJSONArray("plasmids").length();
					int olen = genome.getJSONArray("mitochondrions").length() + genome.getJSONArray("chloroplasts").length();
					System.out.println(genome.getString("name"));
					if(clen == 0 && plen == 0 && olen == 0)
					{
						if(kingName.equals("Viruses") || kingName.equals("Viroids"))
						{
							List<String> urlList = getVirusURL(genome.getString("bioproj"));
							genome.put("chromosomes", urlList);
						}
						else
						{
							int assemblyID = genome.getInt("assembly_id");
							if(assemblyID != 0)
							{
								Map<String, List<String>> urlMap = getURLs(assemblyID);
								genome.put("chromosomes", urlMap.get("chromosomes"));
								genome.put("plasmids", urlMap.get("plasmids"));
								genome.put("mitochondrions", urlMap.get("mitochondrions"));
								genome.put("chloroplasts", urlMap.get("chloroplasts"));
							}
						}
						Writer writer = null;
						try 
						{
						    writer = new BufferedWriter(new OutputStreamWriter(
						          new FileOutputStream(output), "utf-8"));
						    writer.write(organizedJson.toString(4));
						} 
						catch (IOException ex) 
						{
						  // report
						}
						finally 
						{
						   try {writer.close();} catch (Exception ex) {}
						}
					}
				}
			}
		}
	}
	
	public void completeUrl(String kingdom, String group, String subgroup, String name) throws JSONException, IOException
	{
		JSONObject genome = getGenome(kingdom, group, subgroup, name);
		List<String> urlList = getVirusURL(genome.getString("bioproj"));
		genome.put("chromosomes", urlList);
	}
	
	//Fetches the url list of the chromosomes of a genome on the database, given its taxid.
	public List<String> getVirusURL(String bioproj) throws IOException
	{
		boolean caught = false;
		URL genomeBrowse = new URL(url + "/bioproject/" + bioproj);
		BufferedReader in = null; 
		do
		{
			try
			{
				in = new BufferedReader(new InputStreamReader(genomeBrowse.openStream()));
				caught = false;
			}
			catch(UnknownHostException | FileNotFoundException | ConnectException ce)
			{
				caught = true;
			}
		}while(caught);
        Pattern p = Pattern.compile("<a href=\"/nuccore/(NC_.*?)\"");
        String inputLine;
        List<String> urlRes = new ArrayList<String>();
        while ((inputLine = in.readLine()) != null)
        {	
        	Matcher m = p.matcher(inputLine);
        	while(m.find())
        	{
        		urlRes.add(m.group(1));
        	}
        }
        in.close();
		return urlRes;
	}
	
	public Map<String, List<String>> getURLs(int assemblyID) throws IOException, InterruptedException
	{
		int timeout = 3;
		boolean caught = false;
		String assemblyURL = url + "/assembly/" + assemblyID;
		URL genomeBrowse = new URL(assemblyURL);
		BufferedReader in = null;
		int i = 0;
		do
		{
			try
			{
				in = new BufferedReader(new InputStreamReader(genomeBrowse.openStream()));
				caught = false;
			}
			catch(UnknownHostException | ConnectException ce)
			{
				caught = true;
				Thread.sleep(1000);
				i++;
			}
		}while(caught && i < timeout);
		if(i == timeout)
		{
			List<String> urlC = new ArrayList<String>();
		    List<String> urlP = new ArrayList<String>();
		    List<String> urlM = new ArrayList<String>();
		    List<String> urlCl = new ArrayList<String>();
		    
		    Map<String, List<String>> res = new HashMap<String, List<String>>();
		    res.put("chromosomes", urlC);
		    res.put("plasmids", urlP);
		    res.put("mitochondrions", urlM);
		    res.put("chloroplasts", urlCl);
		    System.out.println("Timeout : " + assemblyURL);
			return res;
		}
		Pattern patC = Pattern.compile("<td>Chromosome.*?(NC_[0-9\\.]*?)</a>");
		Pattern patP = Pattern.compile("<td>Plasmid.*?(NC_[0-9\\.]*?)</a>");
		Pattern patM = Pattern.compile("<td>Mitochondrion.*?(NC_[0-9\\.]*?)</a>");
		Pattern patCl = Pattern.compile("<td>Chloroplast.*?(NC_[0-9\\.]*?)</a>");
		String inputLine;
	    List<String> urlC = new ArrayList<String>();
	    List<String> urlP = new ArrayList<String>();
	    List<String> urlM = new ArrayList<String>();
	    List<String> urlCl = new ArrayList<String>();
	    while ((inputLine = in.readLine()) != null)
	    {
		    Matcher matC = patC.matcher(inputLine);
		    Matcher matP = patP.matcher(inputLine);
		    Matcher matM = patM.matcher(inputLine);
		    Matcher matCl = patCl.matcher(inputLine);
	    	while(matC.find())
	    	{
	    		urlC.add(matC.group(1));
	    	}
	    	while(matP.find())
	    	{
	    		urlP.add(matP.group(1));
	    	}
	    	while(matM.find())
	    	{
	    		urlM.add(matM.group(1));
	    	}
	    	while(matCl.find())
	    	{
	    		urlCl.add(matCl.group(1));
	    	}
	    }
	    in.close();
	    Map<String, List<String>> res = new HashMap<String, List<String>>();
	    res.put("chromosomes", urlC);
	    res.put("plasmids", urlP);
	    res.put("mitochondrions", urlM);
	    res.put("chloroplasts", urlCl);
		return res;
	}
	
	//Get the JSON representation of a genome, safe (sends an error if doesn't exist).
	public JSONObject getGenome(String kingdom, String group, String subgroup, String genome)
	{
		JSONObject k = exists(organizedJson, "kingdom", kingdom);
		if(k == null)
		{
			System.err.println("Kingdom doesn't exist.");
			System.exit(0);
		}
		
		JSONObject g = exists(k.getJSONArray("groups"), "group", group);
		if(g == null)
		{
			System.err.println("Group doesn't exist.");
			System.exit(0);
		}
		
		JSONObject s = exists(g.getJSONArray("subgroups"), "subgroup", subgroup);
		if(s == null)
		{
			System.err.println("SubGroup doesn't exist.");
			System.exit(0);
		}
		JSONObject gen = exists(s.getJSONArray("genomes"), "name", genome);
		if(gen == null)
		{
			System.err.println("Genome doesn't exist.");
			System.exit(0);
		}
		return gen;
	}
	
	

	public JSONObject getGenome(int id)
	{
		int i;
		int j;
		int k;
		int l;
		
		int len = organizedJson.length();
		for(i = 0; i < len; i++)
		{
			JSONArray groups = organizedJson.getJSONObject(i).getJSONArray("groups");
			int leng = groups.length();
			for(j = 0; j < leng; j++)
			{
				JSONArray subgroups = groups.getJSONObject(j).getJSONArray("subgroups");
				int lens = subgroups.length();
				for(k = 0; k < lens; k++)
				{
					JSONArray genomes = subgroups.getJSONObject(k).getJSONArray("genomes");
					int lenc = genomes.length();
					for(l = 0; l < lenc; l++)
					{						
						JSONObject genome = genomes.getJSONObject(l);
						if (genome.getInt("genome_id") == id)
						{
							String kingdomName = organizedJson.getJSONObject(i).getString("kingdom");
							String kingdomPath = "Genomes" + App.FILE_SEP + kingdomName.replaceAll("[\\/\\<\\>\\?|\\*\"\\:]", " ");
							
							String groupName = groups.getJSONObject(j).getString("group");
							String groupPath = kingdomPath + App.FILE_SEP + groupName.replaceAll("[\\/\\<\\>\\?|\\*\"\\:]", " ");
							
							String subGroupName = subgroups.getJSONObject(k).getString("subgroup");
							String subGroupPath = groupPath + App.FILE_SEP + subGroupName.replaceAll("[\\/\\<\\>\\?|\\*\"\\:]", " ");
							
							String name = genome.getString("name");
							String genomePath = subGroupPath + App.FILE_SEP +  name.replaceAll("[\\/\\<\\>\\?|\\*\"\\:]", " ");
							
							//Initial directory creation :
							File theDir = new File(genomePath);
							theDir.mkdirs();
							
							return genome;
						}
					}
				}
			}
		}
		return null;
	}
	
	//Get a genome attribute according to its key, safe (returns null if doesn't exist or writes error message).
	public Object optObject(String kingdom, String group, String subgroup, String genome, String key)
	{
		JSONObject gen = getGenome(kingdom, group, subgroup, genome);
		Object o = gen.opt(key);
		return o;
	}
	
	//Get genome id of a genome, not safe.
	public int getGenId(String kingdom, String group, String subgroup, String genome)
	{
		JSONObject gen = getGenome(kingdom, group, subgroup, genome);
		int genid = (int)gen.getLong("genome_id");
		return genid;
	}
	
	//Get the url list of the chromoses of a genome, safe (returns null if doesn't exist or writes error message).
	public Map<String, List<String>> getUrlByGenome(String kingdom, String group, String subgroup, String genome)
	{
		JSONObject gen = getGenome(kingdom, group, subgroup, genome);
		JSONArray chr = gen.optJSONArray("chromosomes");
		JSONArray pl = gen.optJSONArray("plasmids");
		JSONArray mit = gen.optJSONArray("mitochondrions");
		JSONArray chl = gen.optJSONArray("chloroplasts");
		
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		List<String> listChr = new ArrayList<String>();
		List<String> listPl = new ArrayList<String>();
		List<String> listMit = new ArrayList<String>();
		List<String> listChl = new ArrayList<String>();
		for (int  i = 0; i < chr.length(); i++)
		{ 
		    listChr.add(chr.getString(i));
		}
		for (int  i = 0; i < pl.length(); i++)
		{ 
		    listPl.add(pl.getString(i));
		}
		for (int  i = 0; i < mit.length(); i++)
		{ 
		    listMit.add(mit.getString(i));
		}
		for (int  i = 0; i < chl.length(); i++)
		{ 
		    listChl.add(chl.getString(i));
		}
		map.put("chromosomes", listChr);
		map.put("plasmids", listPl);
		map.put("mitochondrions", listMit);
		map.put("chloroplasts", listChl);
		return map;
	}
	
	public JSONArray getUrlFromList(int[] ids)
	{
		JSONArray res = new JSONArray();
		for(int id : ids)
		{
			JSONObject gen = getGenome(id);
			
			JSONArray chr = gen.optJSONArray("chromosomes");
			JSONArray pl = gen.optJSONArray("plasmids");
			JSONArray mit = gen.optJSONArray("mitochondrions");
			JSONArray chl = gen.optJSONArray("chloroplasts");
			
			JSONObject jo = new JSONObject();
			
			List<String> listChr = new ArrayList<String>();
			List<String> listPl = new ArrayList<String>();
			List<String> listMit = new ArrayList<String>();
			List<String> listChl = new ArrayList<String>();
			for (int  i = 0; i < chr.length(); i++)
			{ 
			    listChr.add(chr.getString(i));
			}
			for (int  i = 0; i < pl.length(); i++)
			{ 
			    listPl.add(pl.getString(i));
			}
			for (int  i = 0; i < mit.length(); i++)
			{ 
			    listMit.add(mit.getString(i));
			}
			for (int  i = 0; i < chl.length(); i++)
			{ 
			    listChl.add(chl.getString(i));
			}
			jo.put("name", gen.getString("name"));
			jo.put("genome_id", gen.getInt("genome_id"));
			jo.put("chromosomes", listChr);
			jo.put("plasmids", listPl);
			jo.put("mitochondrions", listMit);
			jo.put("chloroplasts", listChl);
			
			res.put(jo);
		}
		return res;
	}
	public JSONArray getNuccoreFromList(int[] ids) throws IOException, InterruptedException
	{
		JSONArray res = new JSONArray();
		float s = (nb_proc * 100.0f) / ids.length;
		float n = 0;
		for(int id : ids)
		{
			JSONObject gen = getGenome(id);
			int assemblyID = gen.getInt("assembly_id");
			JSONObject jo = new JSONObject();
			if(assemblyID != 0)
			{
				Map<String, List<String>> map = getURLs(assemblyID);
				List<String> listChr = map.get("chromosomes");
				List<String> listPl = map.get("plasmids");
				List<String> listChl = map.get("chloroplasts");
				List<String> listMit = map.get("mitochondrions");
				
				jo.put("name", gen.getString("name"));
				jo.put("genome_id", gen.getInt("genome_id"));
				jo.put("chromosomes", listChr);
				jo.put("plasmids", listPl);
				jo.put("mitochondrions", listMit);
				jo.put("chloroplasts", listChl);
			}
			else
			{
				List<String> l = getVirusURL(gen.getString("bioproj"));
				jo.put("chromosomes", l);
			}
			res.put(jo);
			n += s;
			w.set_progressBar_value((int)n);
		}
		return res;
	}

	public JSONArray getNuccoreFromList(int[] ids, int inf, int sup) throws IOException, InterruptedException
	{
		JSONArray res = new JSONArray();
		float s = 100.0f / ids.length;
		float n = 0;
		int i;
		for(i = inf; i < sup; i++)
		{
			int id = ids[i];
			JSONObject gen = getGenome(id);
			int assemblyID = gen.getInt("assembly_id");
			JSONObject jo = new JSONObject();
			if(assemblyID != 0)
			{
				Map<String, List<String>> map = getURLs(assemblyID);
				List<String> listChr = map.get("chromosomes");
				List<String> listPl = map.get("plasmids");
				List<String> listChl = map.get("chloroplasts");
				List<String> listMit = map.get("mitochondrions");
				
				jo.put("chromosomes", listChr);
				jo.put("plasmids", listPl);
				jo.put("mitochondrions", listMit);
				jo.put("chloroplasts", listChl);
			}
			else
			{
				List<String> l = getVirusURL(gen.getString("bioproj"));
				List<String> tmp = new ArrayList<String>();
				jo.put("chromosomes", l);
				jo.put("plasmids", tmp);
				jo.put("mitochondrions", tmp);
				jo.put("chloroplasts", tmp);
			}
			jo.put("name", gen.getString("name"));
			jo.put("genome_id", gen.getInt("genome_id"));
			jo.put("path", gen.getString("path"));
			res.put(jo);
			val += s;
			w.set_progressBar_value((int)val);
		}
		return res;
	}
	
	public JSONArray getNuccoreFromIds(int[] ids)
	{
		int i;
		nb_proc = Runtime.getRuntime().availableProcessors();
		JSONArray nuccores = new JSONArray();
        NuccoreThread[] nts = new NuccoreThread[nb_proc];
        int len = ids.length / nb_proc;
        for(i = 0; i < nb_proc; i++)
        {
        	int inf = i * (len + 1);
        	int sup = Math.min(ids.length, (i+1) * (len + 1));
        	nts[i] = new NuccoreThread(this, ids, inf, sup);
            nts[i].start();
        }
        for(i = 0; i < nb_proc; i++)
        {
        	try
            {
            	nts[i].join();
            }catch(InterruptedException ie){}
            
            JSONArray ja = nts[i].getNuccores();
            for(int j = 0; j < ja.length(); j++)
            {
            	nuccores.put(ja.getJSONObject(j));
            }
        }
        return nuccores;
	}
}