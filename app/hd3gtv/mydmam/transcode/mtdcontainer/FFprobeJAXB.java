/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.transcode.mtdcontainer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.ffmpeg.ffprobe.FfprobeType;
import org.xml.sax.SAXException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.ExecprocessGettext;

public class FFprobeJAXB {
	
	private Unmarshaller unmarshaller;
	private DocumentBuilder xml_document_builder;
	
	public FFprobeJAXB() throws JAXBException, ParserConfigurationException {
		/**
		 * Load JAXB classes
		 */
		JAXBContext jc = JAXBContext.newInstance("org.ffmpeg.ffprobe");
		unmarshaller = jc.createUnmarshaller();
		
		/**
		 * Prepare an error catcher if trouble are catched during import.
		 */
		unmarshaller.setEventHandler((ValidationEventHandler) e -> {
			ValidationEventLocator localtor = e.getLocator();
			Loggers.Transcode.warn("FFprobe XML validation: " + e.getMessage() + " [s" + e.getSeverity() + "] at line " + localtor.getLineNumber() + ", column " + localtor.getColumnNumber()
					+ " offset " + localtor.getOffset() + " node: " + localtor.getNode() + ", object " + localtor.getObject());
			return true;
		});
		
		/**
		 * Load XML engine
		 */
		DocumentBuilderFactory xmlDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
		xml_document_builder = xmlDocumentBuilderFactory.newDocumentBuilder();
		xml_document_builder.setErrorHandler(null);
	}
	
	public FfprobeType getFromXML(byte[] ffprobe_stdout_raw_xml) throws JAXBException, SAXException, IOException {
		Document document = xml_document_builder.parse(new ByteArrayInputStream(ffprobe_stdout_raw_xml));
		return unmarshaller.unmarshal(document, FfprobeType.class).getValue();
	}
	
	/**
	 * You should check before use the ffprobe exe binary presence.
	 */
	public FfprobeType analystFile(File local_file) throws IOException, JAXBException, SAXException {
		CopyMove.checkExistsCanRead(local_file);
		CopyMove.checkIsFile(local_file);
		
		/**
		 * ffprobe -print_format xml -show_streams -show_format -hide_banner -i <my-media-file>
		 */
		ArrayList<String> params = new ArrayList<String>();
		params.add("-print_format");
		params.add("xml");
		params.add("-show_streams");
		params.add("-show_format");
		params.add("-show_format");
		params.add("-show_chapters");
		params.add("-hide_banner");
		params.add("-i");
		params.add(local_file.getCanonicalPath());
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("ffprobe"), params);
		process.setEndlinewidthnewline(true);
		process.start();
		
		return getFromXML(process.getResultstdout().toString().getBytes("UTF-8"));
	}
	
}
