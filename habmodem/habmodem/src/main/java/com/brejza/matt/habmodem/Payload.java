package com.brejza.matt.habmodem;

import java.util.List;
import java.util.TreeMap;

import org.mapsforge.core.GeoPoint;


public class Payload implements java.io.Serializable{
	
		
	private static final long serialVersionUID = 0x5901fa8c0e38abb2L;
	public String callsign;
	String _payloadID;
	String _flightID;
	boolean _isActiveFlight = false;
	boolean _useFlightView = false;
	boolean _activePayload = false;
	int _total300 = 0;
	int _total50 = 0;
	public int colour = 0;
	
	private int _extraFields;
	
	double maxAltitude = -9999999;
	
	public TelemetryConfig telemetryConfig = null;
	public List<GeoPoint> predictedPath = null;
	public long lastPredictionGetTime = 0;
	public Gps_coordinate lastPredictionLocation = null;
	
	int _maxLookBehind = 4*24*60*60;
	int _maxRecords = 3000;
	long _lastUpdated = 0;
	long _query_ongoing = 0;    //set to 0 for no query (datafetch), otherwise timestamp of query
	public TreeMap<Long,Telemetry_string> data = new TreeMap<Long,Telemetry_string>();
	
	public AscentRate ascentRate = new AscentRate();
	
	public Payload(String call, int _colour, boolean activePayload, int lookBehind)
	{
		callsign = call; 
		_activePayload = activePayload;
		_maxLookBehind = lookBehind;// * 24*60*60;
		colour = _colour;
	}
	
	public Payload(String call, int _colour, boolean activePayload)
	{
		callsign = call; 
		_activePayload = activePayload;
		colour = _colour;
	}
	
	public Payload(String call, String payloadID, String flightID)
	{
		callsign = call; 
		_payloadID = payloadID;
		_flightID = flightID;
		_isActiveFlight = true;
		_activePayload = false;
	}
	
	public Payload(String call, String payloadID)
	{
		callsign = call;
		_payloadID = payloadID;
		_activePayload = false;
	}
	
	public Payload(Telemetry_string str, int _colour){
		callsign = str.callsign;
		if (str.time != null)
			data.put(Long.valueOf(str.time.getTime()), str);
		_activePayload = true;
		colour = _colour;
	}		
					
	public void setFlightID(String id){
		_flightID = id;
	}
	public void setNewColour(int _colour)
	{
		colour = _colour;
	}
	public String getFlightID(){
		return _flightID;
	}
	public void setPayloadID(String id){
		_payloadID = id;
	}
	public String getPayloadID(){
		return _payloadID;
	}
	public void setMaxLookBehindDays(int t){
		_maxLookBehind = t * 60*60*24;
	}
	public void setMaxLookBehindSecs(int t){
		_maxLookBehind = t;
	}
	public void setMaxLookBehind(int t){
		_maxLookBehind = t;
	}
	public long getLastUpdated(){
		return _lastUpdated;
	}
	public void setMaxRecords(int max){
		_maxRecords = max;
	}
	public boolean isActivePayload(){
		return _activePayload;
	}
	public void setIsActivePayload(boolean ap){
		_activePayload = ap;
	}
	public int getMaxRecords(){
		return _maxRecords;
	}
	public long getUpdateStart(boolean flightView) {
		if (_lastUpdated == 0){
			if (flightView)
				return 0;
			else
				return (System.currentTimeMillis()/1000 - _maxLookBehind);
		}
		else
			return _lastUpdated;
	}
	public void setLastUpdated(long t){
		_lastUpdated = t;
		_activePayload = true;
	}
	public void setLastUpdatedNow(){
		_lastUpdated = System.currentTimeMillis()/1000;
		_activePayload = true;
	}
	public Telemetry_string getLastString()	{
		if (data.size() > 0)
		{
			return data.lastEntry().getValue();
		}
		else
			return null;
	}
	public long getLastTime()	{
		if (data.size() > 0)
		{
			return Long.valueOf(data.lastKey());
		}
		else
			return 0;
	}
	public double getAscentRate(){
		return ascentRate.getAscentRate();
	}

	public void putPacket(Telemetry_string str) {
		if (str.time != null){
			data.put(str.time.getTime(), str);
			if (str.coords.alt_valid && str.checksum_valid)
				ascentRate.addData(str.time.getTime(), str.coords.altitude);
		}
	}
	
	public void putPackets( TreeMap<Long,Telemetry_string> in){
		if (in == null)
			return;
		if (in.size() < 1)
			return;
		data.putAll(in);
		Gps_coordinate c = data.lastEntry().getValue().coords;
		if (c.alt_valid)
			ascentRate.addData(data.lastEntry().getValue().time.getTime(),c.altitude);
	}
	public void clearUserData(){
		data = new TreeMap<Long,Telemetry_string>();
		_activePayload = false;
		_lastUpdated = 0;
		ascentRate = new AscentRate();
	}
	public void putMaxAltitude(double altitude)
	{
		maxAltitude = Math.max(maxAltitude, altitude);
	}
	public double getMaxAltitude()
	{
		return maxAltitude;
	}
	public long getQueryOngoing()
	{
		return _query_ongoing;
	}
	public void setQueryOngoing(long l)
	{
		_query_ongoing = l;
	}
	public int getNumberExtraFields()
	{
		return _extraFields;
	}
}
