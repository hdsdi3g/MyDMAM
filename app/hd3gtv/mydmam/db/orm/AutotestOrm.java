/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.db.orm;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.MyDMAM;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.StringSerializer;

public class AutotestOrm extends OrmModel {
	
	public static final ColumnFamily<String, String> CF = new ColumnFamily<String, String>("InternalMyDmamTest", StringSerializer.get(), StringSerializer.get());
	
	public String strvalue;
	public byte[] bytvalue;
	public int intvalue;
	public long lngvalue;
	public boolean bolvalue;
	public Date dtevalue;
	public Float fltvalue;
	public Double dlbvalue;
	public UUID uuivalue;
	public JSONObject jsovalue;
	public JSONArray jsavalue;
	public InetAddress addressvalue;
	public StringBuffer sbuvalue;
	public Calendar calendarvalue;
	public String[] strarrayvalue;
	public HashMap<String, String> serializvalue;
	public String iamempty;
	public String iamnull;
	
	@CassandraIndexed
	public int iamanindex;
	
	public transient String donttouchme;
	
	public enum MyEnum {
		CALL, ME, MAYBE;
	}
	
	public MyEnum enumvalue;
	
	public static AutotestOrm populate(int index) {
		AutotestOrm result = new AutotestOrm();
		
		result.key = "THISISMYKEY" + String.valueOf(index);
		result.strvalue = "Hello world with àcçënts";
		result.bytvalue = result.strvalue.toUpperCase().getBytes();
		result.intvalue = 42;
		result.lngvalue = -3329447494103907027L;
		result.bolvalue = true;
		result.fltvalue = 6.55957f;
		result.dlbvalue = (double) result.lngvalue / 11d;
		result.uuivalue = UUID.fromString("110E8400-E29B-11D4-A716-446655440000");
		result.jsovalue = new JSONObject();
		result.jsovalue.put("Hello", "world");
		result.jsovalue.put("Count", index);
		result.jsavalue = new JSONArray();
		result.jsavalue.add("One");
		result.jsavalue.add(42);
		result.jsavalue.add("Un");
		try {
			result.addressvalue = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		result.sbuvalue = new StringBuffer();
		result.sbuvalue.append(result.key);
		result.sbuvalue.append(result.strvalue);
		result.sbuvalue.append(result.lngvalue);
		result.calendarvalue = Calendar.getInstance();
		result.calendarvalue.set(1995, 05, 23, 10, 04, 8);
		result.calendarvalue.set(Calendar.MILLISECOND, 666);
		result.dtevalue = result.calendarvalue.getTime();
		result.strarrayvalue = new String[2];
		result.strarrayvalue[0] = result.strvalue;
		result.strarrayvalue[1] = result.key;
		result.serializvalue = new HashMap<String, String>();
		result.serializvalue.put("Some var", result.strvalue);
		result.serializvalue.put("Other var", result.uuivalue.toString());
		
		result.enumvalue = MyEnum.ME;
		
		result.iamempty = "";
		result.iamanindex = index;
		result.donttouchme = "F*ck you";
		return result;
	}
	
	public boolean check(int index) {
		AutotestOrm result = populate(index);
		Log2Dump dump = new Log2Dump();
		
		boolean checkresult = true;
		if (result.key.equals(key) == false) {
			dump.add("key", key);
			dump.add("origin", result.key);
			checkresult = false;
		}
		if (result.strvalue.equals(strvalue) == false) {
			dump.add("strvalue", strvalue);
			dump.add("origin", result.strvalue);
			checkresult = false;
		}
		if (result.intvalue != intvalue) {
			dump.add("intvalue", intvalue);
			dump.add("origin", result.intvalue);
			checkresult = false;
		}
		if (result.lngvalue != lngvalue) {
			dump.add("lngvalue", lngvalue);
			dump.add("origin", result.lngvalue);
			checkresult = false;
		}
		if (result.fltvalue.equals(fltvalue) == false) {
			dump.add("fltvalue", fltvalue);
			dump.add("origin", result.fltvalue);
			checkresult = false;
		}
		if (result.dlbvalue.equals(dlbvalue) == false) {
			dump.add("dlbvalue", dlbvalue);
			dump.add("origin", result.dlbvalue);
			checkresult = false;
		}
		if (MyDMAM.byteToString(result.bytvalue).equals(MyDMAM.byteToString(bytvalue)) == false) {
			dump.add("bytvalue", MyDMAM.byteToString(bytvalue));
			dump.add("origin", MyDMAM.byteToString(result.bytvalue));
			checkresult = false;
		}
		if (result.sbuvalue.toString().equals(sbuvalue.toString()) == false) {
			dump.add("sbuvalue", sbuvalue);
			dump.add("origin", result.sbuvalue);
			checkresult = false;
		}
		if (result.bolvalue == false) {
			dump.add("bolvalue", bolvalue);
			checkresult = false;
		}
		if (result.iamempty.equals("") == false) {
			dump.add("iamempty", iamempty);
			checkresult = false;
		}
		if (donttouchme != null) {
			dump.add("donttouchme", donttouchme);
			checkresult = false;
		}
		if (iamnull != null) {
			dump.add("iamnull", iamnull);
			checkresult = false;
		}
		if (iamanindex != index) {
			dump.add("iamanindex", iamanindex);
			dump.add("origin", index);
			checkresult = false;
		}
		if (result.uuivalue.toString().equals(uuivalue.toString()) == false) {
			dump.add("uuivalue", uuivalue);
			dump.add("origin", result.uuivalue);
			checkresult = false;
		}
		if (result.jsavalue.toJSONString().equals(jsavalue.toJSONString()) == false) {
			dump.add("jsavalue", jsavalue.toJSONString());
			dump.add("origin", result.jsavalue.toJSONString());
			checkresult = false;
		}
		if (result.addressvalue.equals(addressvalue) == false) {
			dump.add("addressvalue", addressvalue);
			dump.add("origin", result.addressvalue);
			checkresult = false;
		}
		if (result.calendarvalue.getTimeInMillis() != calendarvalue.getTimeInMillis()) {
			dump.add("calendarvalue", calendarvalue);
			dump.add("origin", result.calendarvalue);
			checkresult = false;
		}
		if (result.dtevalue.getTime() != dtevalue.getTime()) {
			dump.addDate("dtevalue", dtevalue.getTime());
			dump.addDate("origin", result.dtevalue.getTime());
			checkresult = false;
		}
		if (result.strarrayvalue[0].equals(strarrayvalue[0]) == false) {
			dump.add("strarrayvalue", strarrayvalue[0]);
			dump.add("origin", result.strarrayvalue[0]);
			checkresult = false;
		}
		if (result.strarrayvalue[1].equals(strarrayvalue[1]) == false) {
			dump.add("strarrayvalue", strarrayvalue[1]);
			dump.add("origin", result.strarrayvalue[1]);
			checkresult = false;
		}
		if (result.serializvalue.equals(serializvalue) == false) {
			dump.add("serializvalue", serializvalue);
			dump.add("origin", result.serializvalue);
			checkresult = false;
		}
		if (result.enumvalue.equals(enumvalue) == false) {
			dump.add("enumvalue", enumvalue.toString());
			dump.add("origin", result.enumvalue.toString());
			System.out.println(result.enumvalue);
			checkresult = false;
		}
		
		if (checkresult == false) {
			Log2.log.info("Result compare", dump);
		}
		return checkresult;
	}
}
