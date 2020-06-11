// Copyright 2012 (C) Matthew Brejza
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.


package com.brejza.matt.habmodem;


import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;

import net.sf.json.*;

public class Habitat_interface {

	private String _habitat_url = "habitat.habhub.org";
	private String _habitat_db = "habitat";
	private Listener _listener_info;
	private String _listener_telem_UUID="";
	private String _listener_info_UUID="";
	
	protected ArrayList<HabitatRxEvent> _listeners = new ArrayList<HabitatRxEvent>();
	
	public ConcurrentHashMap<String,String> payload_configs = new ConcurrentHashMap<String,String>();
	public ConcurrentHashMap<String,String> flight_configs = new ConcurrentHashMap<String,String>();
	private ConcurrentHashMap<String,TelemetryConfig> telem_configs = new ConcurrentHashMap<String,TelemetryConfig>();
	
	private boolean _newTelemConfigs = false;
	
	private Session s;
	private Database db;
	private Thread sdThread;
	
	public String device = "device";//"XOOM";
	public String device_software = "";//"4.0.x";
	public String application = "HAB Modem";
	public String application_version = "pre-alpha";
	
	private int _prev_query_time = 5 * 60 * 60;
	
	private boolean _queried_current_flights = false;
    
	//TODO: if failed due to connection error, identify error and dont clear the list.
	
	private Queue<Telemetry_string> out_buff = new LinkedBlockingQueue<Telemetry_string>();
	private Queue<QueueItem> _operations = new LinkedBlockingQueue<QueueItem>();
	
	
	
	public Habitat_interface(String callsign) {		
		_listener_info = new Listener(callsign,false);
	}
	
	public Habitat_interface(String habitat_url, String habitat_db, Listener listener_info) {
		_habitat_url = habitat_url;
		_habitat_db = habitat_db;
		_listener_info = listener_info;
		
	}
	
	public Habitat_interface(String habitat_url, String habitat_db) {
		_habitat_url = habitat_url;
		_habitat_db = habitat_db;
		
	}
	
	public void Test()
	{		
		 s = new Session(_habitat_url,80);
		 db = s.getDatabase(_habitat_db);
		 List<Document> foodoc;
		 View v = new View("payload_configuration/callsign_time_created_index");
		 //View v = new View("payload_configuration/callsign_time_created_index&startkey%3D[%22APEX%22]");
		// v.setKey("startkey=APEX");
		 v.setStartKey("[%22APEX%22]");
		 v.setLimit(1);
		//foodoc = db.view("flight/end_start_including_payloads").getResults();
		 foodoc = db.view(v).getResults();
		foodoc.toString();		
	}
	
	public void addHabitatRecievedListener(HabitatRxEvent listener)
	{	
		_listeners.add(listener);
	}
	
	public void addDataFetchTask(String callsign, long startTime, long stopTime, int limit)
	{
		_operations.offer(new QueueItem(1,callsign, startTime, stopTime, limit));
		StartThread();
	}
	
	public void updateChaseCar(Listener newlocation)
	{
		_listener_info = newlocation;
		_operations.offer(new QueueItem(3,0));
		StartThread();
	}
	
	public void updateListener(Listener newlistener)
	{
		_listener_info = newlistener;
	}
	
	public void addGetActiveFlightsTask()
	{
		_operations.offer(new QueueItem(2,0));
		StartThread();
	}
	
	private String resolvePayloadID(String callsign)
	{
		String c = callsign.toUpperCase();
		if (payload_configs.containsKey(c))
			return payload_configs.get(c);
		else if (!_queried_current_flights)
			_queried_current_flights = queryActiveFlights();
			
		if (payload_configs.containsKey(c))
			return payload_configs.get(c);
		else
		{
			queryAllPayloadDocs(c);
			if (payload_configs.containsKey(c))
				return payload_configs.get(c);
			else
				return null;   //TODO: add list of non payloads
		}
	}
	
	public boolean queryAllPayloadDocs(String callsign)
	{
		String docid = null;
		try
		{
			//open DB connection
			if (s == null)
			{
				 s = new Session(_habitat_url,80);
				 db = s.getDatabase(_habitat_db);// + "/_design/payload_telemetry/_update/add_listener");
			}
			else if (db == null)
				db = s.getDatabase(_habitat_db);
		
			List<Document> docsout;
			View v = new View("payload_configuration/callsign_time_created_index");
			//startkey=["CRAAG1",1357396479]&endkey=["CRAAG1",0]&descending=true
			v.setStartKey("[%22" + callsign.toUpperCase() + "%22," + Long.toString((System.currentTimeMillis() / 1000L)) + "]");
			v.setEndKey("[%22" + callsign.toLowerCase() +  "%22,0]");
			//v.setWithDocs(true);
			v.setLimit(1);
			v.setDescending(true);
			ViewResults r = db.view(v);
			docsout = r.getResults();
			
			//docsout.toString();
 

			//docsout.get(0).getJSONObject().getJSONObject("doc").getString("type")
			//docsout.get(1).getJSONObject().getJSONObject("doc").getJSONArray("sentences").getJSONObject(0).getString("callsign")
			
			if (docsout.size() > 0)
			{
				JSONArray ja = docsout.get(0).getJSONArray("key");
				String ss = docsout.get(0).getId();
				if (ja.getString(0).equals(callsign))
					docid = ss;
			}
			
		}
		catch (Exception e)
		{
			System.out.println("ERROR: "+ e.toString());
			return false;
		}
			
		if (docid != null)
			payload_configs.put(callsign.toUpperCase(),docid);
		
		
		return true;
	
	}
	
	private boolean queryPayloadConfig(String docID)
	{
		try
		{
			//open DB connection
			if (s == null)
			{
				 s = new Session(_habitat_url,80);
				 db = s.getDatabase(_habitat_db);// + "/_design/payload_telemetry/_update/add_listener");
			}
			else if (db == null)
				db = s.getDatabase(_habitat_db);
		
			
			Document doc = db.getDocument(docID);

			if (doc == null)
				return false;			
			CouchResponse cr = s.getLastResponse();		
			if (!cr.isOk())
				return false;		
			
			JSONObject obj;
			JSONArray sentences;
			JSONArray fields;
			JSONArray filters;
			
			TelemetryConfig tc = new TelemetryConfig();
			
			obj = doc.getJSONObject();
			if (obj.containsKey("sentences")){
				sentences = obj.getJSONArray("sentences");
				for (int i = 0; i < sentences.size(); i++){
					String call = "";
					if (sentences.getJSONObject(i).containsKey("callsign")){
						call = sentences.getJSONObject(i).getString("callsign");
					}
					else
						return false;
					
					if (sentences.getJSONObject(i).containsKey("fields")){
						fields = sentences.getJSONObject(i).getJSONArray("fields");
						for (int j = 0; j < fields.size(); j++)
						{
							String name = fields.getJSONObject(j).getString("name");
							String sensor = "";
							String format = "";
							TelemetryConfig.DataType dt = TelemetryConfig.DataType.IGNORE;
							if (fields.getJSONObject(j).containsKey("sensor"))
								sensor = fields.getJSONObject(j).getString("sensor");
							if (fields.getJSONObject(j).containsKey("format"))
								format = fields.getJSONObject(j).getString("format");
									
							
							if (name.equals("sentence_id")){
							}
							else if  (name.equals("time")){
							}
							else if (name.equals("latitude" )){
								if (sensor.equals("base.ascii_int"))
								{
									//TODO
									tc.gpsFormat = TelemetryConfig.GPSFormat.INT;
								}
							}
							else if (name.equals("longitude" )){
								if (sensor.equals("base.ascii_int"))
								{
									//TODO
									tc.gpsFormat = TelemetryConfig.GPSFormat.INT;
								}
							}
							else if (name.equals("altitude" )){
							}
							else {
								if (sensor.equals("base.ascii_int"))
								{
									dt = TelemetryConfig.DataType.INT;
								}
								else if (sensor.equals("base.string"))
								{
									dt = TelemetryConfig.DataType.STRING;
								}
								else if (sensor.equals("base.ascii_float"))
								{
									dt = TelemetryConfig.DataType.FLOAT;
								}
							}	
							
							
							tc.addField(name, dt);							
						}
						
						
						if (sentences.getJSONObject(i).containsKey("filters")){
							if (sentences.getJSONObject(i).getJSONObject("filters").containsKey("post")){
								filters = sentences.getJSONObject(i).getJSONObject("filters").getJSONArray("post");
								for (int j = 0; j < filters.size(); j++)
								{
									String filtertype = "";
									String source = "";
									String factor = "";
									String offset = "";
									String round = "";
									String type = "";
									
									if (filters.getJSONObject(j).containsKey("filter"))
										filtertype = filters.getJSONObject(j).getString("filter");
									if (filters.getJSONObject(j).containsKey("source"))
										source = filters.getJSONObject(j).getString("source");
									if (filters.getJSONObject(j).containsKey("factor"))
										factor = filters.getJSONObject(j).getString("factor");
									if (filters.getJSONObject(j).containsKey("type"))
										type = filters.getJSONObject(j).getString("type");
									if (filters.getJSONObject(j).containsKey("offset"))
										offset = filters.getJSONObject(j).getString("offset");
									if (filters.getJSONObject(j).containsKey("round"))
										round = filters.getJSONObject(j).getString("round");
									
									
									if (filtertype.equals("common.numeric_scale")){
										tc.addFilter(source,factor,offset,round);
									}
									else{
										
									}
																	
								}
							}
						}				
					
					}
					//save tc to some sort of data structure
					telem_configs.put(call.toUpperCase(), tc);
					_newTelemConfigs = true;
				}				
			}			
		}
		catch (Exception e)
		{
			System.out.println("ERROR: "+ e.toString());
			return false;
		}
			
			
		return true;
	}
	
	private boolean queryActiveFlights()
	{
		try
		{
			//open DB connection
			if (s == null)
			{
				 s = new Session(_habitat_url,80);
				 db = s.getDatabase(_habitat_db);// + "/_design/payload_telemetry/_update/add_listener");
			}
			else if (db == null)
				db = s.getDatabase(_habitat_db);
		
			List<Document> docsout;
			View v = new View("flight/end_start_including_payloads");
			
			v.setStartKey("[" + Long.toString((System.currentTimeMillis() / 1000L)-(2*60*60)) + "]");
			
			v.setWithDocs(true);
			v.setLimit(30);
			
			ViewResults r = db.view(v);
			docsout = r.getResults();
			
			//docsout.toString();
 

			//docsout.get(0).getJSONObject().getJSONObject("doc").getString("type")
			//docsout.get(1).getJSONObject().getJSONObject("doc").getJSONArray("sentences").getJSONObject(0).getString("callsign")
			 
			for (int i = 0; i < docsout.size(); i++)
			{
				
				
				JSONObject obj;
				
				obj = docsout.get(i).getJSONObject().getJSONObject("doc");
				if (obj != null) {
					if (obj.containsKey("type")) {
						if (obj.getString("type").equals("payload_configuration")) {
							if (obj.containsKey("sentences")){	
								JSONArray jar = obj.getJSONArray("sentences");
								for (int j = 0; j < jar.size(); j++){
									if (jar.getJSONObject(j).containsKey("callsign")){
										String call = jar.getJSONObject(j).getString("callsign");
										if (!flight_configs.containsKey(call.toUpperCase()))
											flight_configs.put(call.toUpperCase(), docsout.get(i).getId());
										if (obj.containsKey("_id"))										
											payload_configs.put(call.toUpperCase(),obj.getString("_id"));
									}									
								}
							}							
						}
					}
				}				
			}
			
			
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	//TODO: if flight doc exists, query that view instead
	private boolean getPayloadDataSince(long timestampStart, long timestampStop, int limit, String payloadID, String callsign)
	{
		double prevAltitude = -99999999;
		double maxAltitude =  -99999999;
		 long prevtime = -1;
		try
		{

			//open DB connection
			if (s == null)
			{
				 s = new Session(_habitat_url,80);
				 db = s.getDatabase(_habitat_db);// + "/_design/payload_telemetry/_update/add_listener");
			}
			else if (db == null)
				db = s.getDatabase(_habitat_db);

			 View v = new View("payload_telemetry/payload_time");

			 long starttime;
			 if (timestampStart > 0)
				 starttime = timestampStart;
			 else
				 starttime = (System.currentTimeMillis() / 1000L)-_prev_query_time;
			 
			 v.setStartKey("[%22" + payloadID + "%22," +Long.toString(starttime) + "]");
			 v.setEndKey("[%22" + payloadID + "%22," +Long.toString(timestampStop)+ "]");

			 v.setWithDocs(true);
			 v.setLimit(limit);


			 System.out.println("DEBUG: DATABASE GOT QUERY");
			 
			 TreeMap<Long,Telemetry_string> out = new TreeMap<Long,Telemetry_string>();
			 
			 JsonFactory fac = new JsonFactory();
			 InputStream is = db.view_dont_parse(v);
			 long lasttime = 0;
			 
			 if (is != null)
			 {
				 
				 JsonParser jp = fac.createJsonParser(is);

				 String str,str1;
				 boolean gotkey = false;
				 boolean keypt1 = false;
				 
				 
				 TelemetryConfig tc = null;
				 if (telem_configs.containsKey(callsign))
				 {
					 tc = telem_configs.get(callsign);
				 }
				 
				while(jp.nextToken() != null )// || (jp.getCurrentLocation().getCharOffset() < body.length()-50)) && nullcount < 20) //100000 > out.size())
				 {
					//jp.nextToken();
	
					str  = jp.getCurrentName();
					str1 = jp.getText();
					if (str == "key" && str1 == "[")
						gotkey = true;
					else if (gotkey){
						keypt1 = true;
						gotkey = false;
					} else if (keypt1) {
						keypt1 = false;
						try {
							lasttime = Long.parseLong(str1); }					
						catch (NumberFormatException e) {
							System.out.println("ERROR PARSING - NUMBER FORMAT EXCEPTION!!!"); }
													
					}
					if (str != null && str1 != null)
					{
					 if (str.equals("_sentence") && !str1.equals("_sentence")){
						 Telemetry_string ts = new Telemetry_string(str1,lasttime, tc);
						 if (!ts.isZeroGPS() && ts.time != null) {
							 if (out.size() > 0){
								 if (out.lastEntry().getValue().coords.alt_valid) {
									 prevAltitude = out.lastEntry().getValue().coords.altitude;									 
									 prevtime = out.lastEntry().getValue().time.getTime();
								 }
							 }
							 out.put(new Long(ts.time.getTime()),ts);  		
							 if (ts.coords.alt_valid)
								 maxAltitude = Math.max(ts.coords.altitude, maxAltitude);
						 }
					 }
					 
					 	//nullcount = 0;
					}
					//else
					//	nullcount++;
				 }
				jp.close();
				is.close();
				
			 }
			s.clearCouchResponse();
			
			
			System.out.println("DEBUG: DATABASE PROCESSING DONE");
			
			AscentRate as = new AscentRate();
			
			if (out.size() >= 2 && prevAltitude > -9999){
				as = new AscentRate();
				as.addData(prevtime, prevAltitude);
				 if (out.lastEntry().getValue().coords.alt_valid) {
					 as.addData(out.lastEntry().getValue().time.getTime(),
							 out.lastEntry().getValue().coords.altitude);

				 }
				 else
					 as = new AscentRate();
			}
			lasttime += 1000;
			if (lasttime >= timestampStart && lasttime <= timestampStop)
				fireDataReceived(out,true,callsign,timestampStart, lasttime,as,maxAltitude); 
			else
				fireDataReceived(out,true,callsign,timestampStart, timestampStop,as,maxAltitude); 
			
			return true;
		}
		
		catch (Exception e)
		{
			fireDataReceived(null,false,e.toString(),timestampStart, timestampStop,null,-99999999);
			return false;
		}
	}
	
	public void getActivePayloads()
	{
		
		try
		{
			//open DB connection
			if (s == null)
			{
				 s = new Session(_habitat_url,80);
				 db = s.getDatabase(_habitat_db);// + "/_design/payload_telemetry/_update/add_listener");
			}
			else if (db == null)
				db = s.getDatabase(_habitat_db);
			
			 List<Document> foodoc;
			 View v = new View("payload_telemetry/time");
	
			 v.setStartKey(Long.toString((System.currentTimeMillis() / 1000L)-_prev_query_time));
			 v.setWithDocs(true);
			 v.setLimit(40);

			 ViewResults r = db.view(v);
			 foodoc = r.getResults();
			// foodoc = db.view(v).getResults();
			foodoc.toString();
			//((JSONObject)((JSONObject)foodoc.get(1).getJSONObject().get("doc")).get("data")).get("payload")
			
		}
		catch (Exception e)
		{
			
		}
	}
	
	public void upload_payload_telem(Telemetry_string input)
	{
		boolean added=false;		
		added=out_buff.offer(input);	

		if (added)
		{
			_operations.offer(new QueueItem(0,1));
			StartThread();
		}
	}
	
	private synchronized void StartThread()
	{
		if (sdThread == null)
		{
			sdThread = new SendThread();
			sdThread.start();
		}
		else if (!sdThread.isAlive())			
		{
			sdThread = new SendThread();
			sdThread.start();
		}
	}

	
	private boolean _update_chasecar()
	{
		try
		{
			//open DB connection
			if (s == null)
			{
				 s = new Session(_habitat_url,80);
				 db = s.getDatabase(_habitat_db);// + "/_design/payload_telemetry/_update/add_listener");
			}
			else if (db == null)
				db = s.getDatabase(_habitat_db);
			
			if (_listener_info != null)
			{
				update_listener_telem();
				/*
				Document doc = new Document ();
				Document doc_info = new Document ();
				
				//date uploaded
				Date time = new Date();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
				String t = dateFormat.format(time);
				t = t.substring(0, t.length()-2) + ":" + t.substring(t.length()-2, t.length());
				
				
				doc.put("type", "listener_telemetry");
				doc.put("time_created", t);
				doc.put("time_uploaded", t);
				doc_info.put("type", "listener_information");
				doc_info.put("time_created", t);
				doc_info.put("time_uploaded", t);
				
				JSONObject client = new JSONObject();
				JSONObject l_info = new JSONObject();
				client.put("device", device);
				client.put("device_software", device_software);
				client.put("application", application);
				client.put("application_version", application_version);
				l_info.put("callsign", _listener_info.CallSign());
				
				JSONObject data = _listener_info.getJSONDataField();
				data.put("client", client);
						
				doc.put("data", data);
				
				doc_info.put("data",l_info);
				
				String sha = _listener_info.toSha256();
				String sha_info = _listener_info.toSha256();
				
				db.saveDocument(doc,sha);	
				CouchResponse cr = s.getLastResponse();
				System.out.println(cr);				
				if (cr.isOk())
					_listener_telem_UUID = sha;		
				
				db.saveDocument(doc_info,sha_info);	
				cr = s.getLastResponse();
				System.out.println(cr);				
				if (cr.isOk())
					_listener_info_UUID = sha_info;	
				*/
			}			
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	private boolean _upload(Telemetry_string input)
	{
		try
		{
			//open DB connection
			if (s == null)
			{
				 s = new Session(_habitat_url,80);
				 db = s.getDatabase(_habitat_db);// + "/_design/payload_telemetry/_update/add_listener");
			}
			else if (db == null)
				db = s.getDatabase(_habitat_db);
			
			if (_listener_info != null)
			{
				if (_listener_info.data_changed_info())
					update_listener_info();
				
				if (_listener_info.data_changed_telem())   //upload listeners location
				{
					update_listener_telem();
					/*
					Document doc = new Document ();
					
					//date uploaded
					Date time = new Date();
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
					String t = dateFormat.format(time);
					t = t.substring(0, t.length()-2) + ":" + t.substring(t.length()-2, t.length());
					
					
					
					
					doc.put("type", "listener_telemetry");
					doc.put("time_created", t);
					doc.put("time_uploaded", t);				
					
					JSONObject client = new JSONObject();
					client.put("device", device);
					client.put("device_software", device_software);
					client.put("application", application);
					client.put("application_version", application_version);
					
					JSONObject data = _listener_info.getJSONDataField();
					data.put("client", client);
							
					doc.put("data", data);
					
					
					String sha = _listener_info.toSha256(); 
					
					db.saveDocument(doc,sha);
					CouchResponse cr = s.getLastResponse();
					System.out.println(cr);
					if (cr.isOk())
						_listener_telem_UUID = sha;
					*/
				}
			}
		
		
		
		
			Document doc = new Document();
			JSONObject data = new JSONObject();
			JSONObject receivers = new JSONObject();
			JSONObject receiver = new JSONObject();
			
			//date uploaded
			Date time = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			String t = dateFormat.format(time);
			t = t.substring(0, t.length()-2) + ":" + t.substring(t.length()-2, t.length());
			
			String str64 = input.raw_64_str();
			
			data.put("_raw", str64);
			receiver.put("time_created", input.doc_time_created);
			receiver.put("time_uploaded",t);
			if (input.habitat_metadata != null)
				receiver.putAll(input.habitat_metadata);
			if (_listener_telem_UUID != "")
				receiver.put("latest_listener_telemtry",_listener_telem_UUID);
			if (_listener_info_UUID != "")
				receiver.put("latest_listener_information",_listener_info_UUID);
			receivers.put(_listener_info.CallSign(),receiver);
			
		
			doc.put("type","payload_telemetry");
			doc.put("data",data.toString());
			doc.put("receivers",receivers.toString());
			
			
			String sha = input.toSha256();
		
			//System.out.println(doc.toString());
			
			db.saveDocument(doc,sha);  				//try to upload as only listener
			CouchResponse cr = s.getLastResponse(); //see if successful
			
			if (cr.isOk())
			{		//addition went well, but get the payload config ID if not already
				if (payload_configs.containsKey(input.callsign))
					return true;
				
				//if not already existing, query the now parsed document
				Document result = db.getDocument(sha);
				
				if (result != null)
				{
					if (result.getJSONObject().containsKey("data"))
					{
						JSONObject objdata = result.getJSONObject().getJSONObject("data");
						if (objdata.containsKey("_parsed"))
						{
							objdata = objdata.getJSONObject("_parsed");
							if (objdata.containsKey("payload_configuration"))
								payload_configs.put(input.callsign, objdata.getString("payload_configuration"));
							if (objdata.containsKey("flight"))
								flight_configs.put(input.callsign, objdata.getString("flight"));
						}
					}					
				}
				return true;				
			}
			
			if (!cr.getErrorId().equals("conflict"))
			{
				//throw error but continue
			}
			
			Document result = db.getDocument(sha);
			
			//if payload configs does not contain the payload config ID, get it
			if (!payload_configs.containsKey(input.callsign))
			{				
				
				if (result != null)
				{
					if (result.getJSONObject().containsKey("data"))
					{
						JSONObject objdata = (JSONObject)(result.getJSONObject().get("data"));
						if (objdata.containsKey("_parsed"))
						{
							JSONObject obparse = (JSONObject)objdata.get("_parsed");
							if (obparse.containsKey("payload_configuration"))
								payload_configs.put(input.callsign, obparse.get("payload_configuration").toString());
						}
					}					
				}
			}
			
			int its = 0;
			//start of main loop
			while(its < 30)
			{
				its++;

				if (result == null) 
					return false;           //somethings gone wrong
				
				//instead try to append document
	
				JSONObject existing_receivers = new JSONObject();
				
				JSONObject _dat_a = (JSONObject) result.get("data");
				if (_dat_a == null)
				{
					System.out.println("DID NOT PARSE DATA SECTION");
				}
				/*
				double gf = 4.3600000000000003197;
				double ss = 0.0000000000000003197;
				double p = gf - 4.36;
				System.out.println(_dat_a.get("battery") + "   " + gf + "   " + ss + "   " + p); */
				if (!result.containsKey("receivers"))
					return false;           //somethings gone wrong
				
				existing_receivers = (JSONObject) result.remove("receivers");
				existing_receivers.put(_listener_info.CallSign(),receiver);
				result.put("receivers", existing_receivers);
				
				db.saveDocument(result,sha);
				cr = s.getLastResponse(); 
				//System.out.println(cr.statusCode);
				if (cr.isOk())
					return true;
				if (cr.getErrorId() == null)
				{
					//throw error but continue
					its += 9;
				}
				else if (!cr.getErrorId().equals("conflict"))
				{
					//throw error but continue
					its += 9;
				}
				//get document for next run through
				result = db.getDocument(sha);
			}
			//System.out.println(cr.isOk() + "  " + cr.getErrorId() + "  " + cr.getErrorReason());
			//System.out.println(s.getLastResponse());
			
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
	
	protected void fireDataReceived(TreeMap<Long, Telemetry_string> out, boolean success, String callsign, long startTime, long endTime, AscentRate as, double maxAltitude)
	{
		for (int i = 0; i < _listeners.size(); i++)
		{
			_listeners.get(i).HabitatRx(out, success, callsign, startTime, endTime,as,maxAltitude);
		}
	}
	
	public void addStringRecievedListener(HabitatRxEvent listener)
	{	
		_listeners.add(listener);
	}
	
	class SendThread extends Thread
	{
		  
		  public void run()
		  {
			  boolean buff_empty = false;
			  while(!buff_empty)
			  {
				
				  
				  if (!_operations.isEmpty())
				  {
					  QueueItem qi = _operations.poll();
					  if (qi != null)
					  {
						  if (qi.type == 0)
						  {			//upload telem
							  
							  Telemetry_string tosend = out_buff.peek();
							  boolean res = true;
							  if (tosend != null)								
								  res = _upload(tosend);  //now we have some telem, lets send it							  
							  if (!res)
								  System.out.println("UPLOAD FAILED :(");
							  else //success, remove data from queue
								  out_buff.poll();
						  }
						  else if (qi.type == 1)
						  {			//get data
							  String id = resolvePayloadID(qi.callsign);
							  if (!telem_configs.containsKey(qi.callsign.toUpperCase()))
								  queryPayloadConfig(id);
							  if (id != null)
								  getPayloadDataSince(qi.startTime, qi.stopTime,qi.count, id, qi.callsign);
						  }
						  else if (qi.type == 2)    //update list of active flights
						  {
							  _queried_current_flights = queryActiveFlights();
						  }
						  else               //upload chasecar
						  {
							  _update_chasecar();
						  }
					  }
					  else
						  buff_empty = true;
				  }
				  else
					  buff_empty = true;
				  
			  }
			
			  //deal with any items in the send queue which are left over
			  buff_empty = false;
			  while(!buff_empty)
			  {
				  
				  if (out_buff.isEmpty())
				  {
					  buff_empty = true;
				  }
				  else
				  {
					  Telemetry_string tosend = out_buff.poll();

					  boolean res = true;
					  if (tosend == null)
						  buff_empty = true;
					  else
						  res = _upload(tosend);  //now we have some telem, lets send it
					  
					  if (!res)
						  System.out.println("UPLOAD FAILED :(");
				  }
			  }
		  }
	}
	
	private boolean update_listener_telem()
	{
		try
		{
			Document doc = new Document ();
			Document doc_info = new Document ();
			
			//date uploaded
			Date time = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			String t = dateFormat.format(time);
			t = t.substring(0, t.length()-2) + ":" + t.substring(t.length()-2, t.length());
			
			
			doc.put("type", "listener_telemetry");
			doc.put("time_created", t);
			doc.put("time_uploaded", t);
			
			
			JSONObject client = new JSONObject();
			JSONObject l_info = new JSONObject();
			client.put("device", device);
			client.put("device_software", device_software);
			client.put("application", application);
			client.put("application_version", application_version);

			
			JSONObject data = _listener_info.getJSONDataFieldTelem();
			data.put("client", client);
					
			doc.put("data", data);
			
			doc_info.put("data",l_info);
			
			String sha = _listener_info.toSha256();

			
			db.saveDocument(doc,sha);	
			CouchResponse cr = s.getLastResponse();
			System.out.println(cr);				
			if (cr.isOk())
				_listener_telem_UUID = sha;		
		
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
	
	private boolean update_listener_info()
	{
		try
		{

			Document doc_info = new Document ();
			
			//date uploaded
			Date time = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			String t = dateFormat.format(time);
			t = t.substring(0, t.length()-2) + ":" + t.substring(t.length()-2, t.length());
			

			doc_info.put("type", "listener_information");
			doc_info.put("time_created", t);
			doc_info.put("time_uploaded", t);	
			
			doc_info.put("data",_listener_info.getJSONDataFieldInfo());
			
			String sha_info = toSha256(doc_info.toString());
									
			db.saveDocument(doc_info,sha_info);	
			CouchResponse cr = s.getLastResponse();
			System.out.println(cr);				
			if (cr.isOk())
				_listener_info_UUID = sha_info;	
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
	
	public boolean newTelemConfigs()
	{
		return _newTelemConfigs;
	}
	
	public ConcurrentHashMap<String,TelemetryConfig> getTelemConfigs()
	{
		_newTelemConfigs = false;
		return telem_configs;
	}
	
	class QueueItem
	{
		public int type;				//0: upload payload, 1: get payload telem, 2: update active flight list, 3: update chase car
		public String callsign = "";
		public long startTime = 0;
		public long stopTime = 0;
		public int count = 0;
		public QueueItem(int _type,int _count)
		{
			type = _type;
			count = _count;
		}
		public QueueItem(int _type, String _callsign, long _startTime, long _stopTime, int _count)
		{
			type = _type;
			callsign = _callsign;
			startTime = _startTime;
			stopTime = _stopTime;
			count = _count;
		}
	}
	
	public static String toSha256(String str)
	{

		byte [] enc = Base64.encodeBase64(str.getBytes());
		byte[] sha = null;
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
			md.update(enc); 
			sha = md.digest();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return bytesToHexStr(sha);
	}
	
	//ref: http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	public static String bytesToHexStr(byte[] bytes) {
	    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
