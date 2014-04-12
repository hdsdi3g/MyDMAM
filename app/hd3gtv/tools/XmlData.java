/*
 * This file is part of Java Tools by hdsdi3g'.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2011-2014
 * 
*/package hd3gtv.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;

@SuppressWarnings("nls")
/**
 * @author hdsdi3g
 * @version 1.1 Add getTextContentValue()
 */
public class XmlData {
	
	public XmlData(Document document) throws NullPointerException {
		if (document == null) {
			throw new NullPointerException("\"document\" can't to be null");
		}
		this.document = document;
	}
	
	private Document document;
	
	public Element getDocumentElement() {
		return document.getDocumentElement();
	}
	
	public Document getDocument() {
		return document;
	}
	
	public static XmlData loadFromFile(File xmlfile) throws IOException {
		if (xmlfile == null) {
			throw new NullPointerException("\"xmlfile\" can't to be null");
		}
		if ((xmlfile.exists() == false) | (xmlfile.canRead() == false)) {
			throw new FileNotFoundException(xmlfile.getPath());
		}
		
		try {
			DocumentBuilderFactory xmlDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
			
			try {
				xmlDocumentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			} catch (ParserConfigurationException pce) {
			}
			try {
				xmlDocumentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			} catch (ParserConfigurationException pce) {
			}
			
			DocumentBuilder xmlDocumentBuilder = xmlDocumentBuilderFactory.newDocumentBuilder();
			xmlDocumentBuilder.setErrorHandler(null);
			return new XmlData(xmlDocumentBuilder.parse(xmlfile));
		} catch (ParserConfigurationException pce) {
			throw new IOException("DOM parser error", pce);
		} catch (SAXException se) {
			throw new IOException("XML Struct error", se);
		}
	}
	
	public void writeToFile(File xmlfile) throws IOException {
		try {
			PrintWriter outFilePW = new PrintWriter(xmlfile, "UTF-8");
			DOMSource domSource = new DOMSource(document);
			StringWriter stringwriter = new StringWriter();
			StreamResult streamresult = new StreamResult(stringwriter);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(domSource, streamresult);
			outFilePW.print(stringwriter.toString());
			outFilePW.close();
		} catch (UnsupportedEncodingException uee) {
			throw new IOException("Encoding XML is not supported", uee);
		} catch (TransformerException tc) {
			throw new IOException("Converting error between XML and String", tc);
		}
	}
	
	public static XmlData createEmptyDocument() {
		try {
			DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
			DocumentBuilder constructeur = fabrique.newDocumentBuilder();
			Document document = constructeur.newDocument();
			document.setXmlVersion("1.0");
			document.setXmlStandalone(true);
			return new XmlData(document);
		} catch (ParserConfigurationException pce) {
			/**
			 * Specific error : can't to be thrown !
			 */
			pce.printStackTrace();
			return null;
		}
	}
	
	public byte[] getBytes() throws IOException {
		try {
			DOMSource domSource = new DOMSource(document);
			StringWriter stringwriter = new StringWriter();
			StreamResult streamresult = new StreamResult(stringwriter);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.transform(domSource, streamresult);
			return stringwriter.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException uee) {
			throw new IOException("Encoding XML is not supported", uee);
		} catch (TransformerException tc) {
			throw new IOException("Converting error between XML and String", tc);
		}
	}
	
	public static XmlData loadFromBytes(byte[] data, int offset, int length) throws IOException {
		try {
			DocumentBuilderFactory xmlDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlDocumentBuilder = xmlDocumentBuilderFactory.newDocumentBuilder();
			xmlDocumentBuilder.setErrorHandler(null);
			ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, length);
			Document document = xmlDocumentBuilder.parse(bais);
			return new XmlData(document);
		} catch (ParserConfigurationException pce) {
			throw new IOException("DOM parser error", pce);
		} catch (SAXException se) {
			throw new IOException("XML Struct error", se);
		}
	}
	
	public static XmlData loadFromString(String content) throws IOException {
		try {
			DocumentBuilderFactory xmlDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlDocumentBuilder = xmlDocumentBuilderFactory.newDocumentBuilder();
			xmlDocumentBuilder.setErrorHandler(null);
			ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes());
			Document document = xmlDocumentBuilder.parse(bais);
			return new XmlData(document);
		} catch (ParserConfigurationException pce) {
			throw new IOException("DOM parser error", pce);
		} catch (SAXException se) {
			throw new IOException("XML Struct error", se);
		}
	}
	
	/**
	 * @param elementname is case insensitive.
	 * @return first Element or null
	 */
	public static Element getElementByName(Element node, String elementname) {
		return getElementByName(node.getChildNodes(), elementname);
	}
	
	/**
	 * @param elementname is case insensitive.
	 * @return first Element or null
	 */
	public static Element getElementByName(NodeList nodes, String elementname) {
		for (int pos = 0; pos < nodes.getLength(); pos++) {
			if (nodes.item(pos).getNodeType() == Node.ELEMENT_NODE) {
				if (nodes.item(pos) instanceof Element) {
					if (nodes.item(pos).getNodeName().equalsIgnoreCase(elementname)) {
						return (Element) nodes.item(pos);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * @param elementname is case insensitive.
	 * @return all Elements who have elementname as node name. Never null.
	 */
	public static ArrayList<Element> getElementsByName(Element node, String elementname) {
		return getElementsByName(node.getChildNodes(), elementname);
	}
	
	/**
	 * @param elementname is case insensitive.
	 * @return all Elements who have elementname as node name. Never null.
	 */
	public static ArrayList<Element> getElementsByName(NodeList nodes, String elementname) {
		ArrayList<Element> elements = new ArrayList<Element>();
		for (int pos = 0; pos < nodes.getLength(); pos++) {
			if (nodes.item(pos).getNodeType() == Node.ELEMENT_NODE) {
				if (nodes.item(pos) instanceof Element) {
					if (nodes.item(pos).getNodeName().equalsIgnoreCase(elementname)) {
						elements.add((Element) nodes.item(pos));
					}
				}
			}
		}
		return elements;
	}
	
	public static ArrayList<Element> getAllSubElements(NodeList nodes) {
		ArrayList<Element> elements = new ArrayList<Element>();
		if (nodes == null) {
			return elements;
		}
		for (int pos = 0; pos < nodes.getLength(); pos++) {
			if (nodes.item(pos).getNodeType() == Node.ELEMENT_NODE) {
				if (nodes.item(pos) instanceof Element) {
					elements.add((Element) nodes.item(pos));
				}
			}
		}
		return elements;
	}
	
	public static String getTextContentValue(NodeList nodes, String elementname) {
		Element e = getElementByName(nodes, elementname);
		if (e != null) {
			return e.getTextContent();
		}
		return null;
	}
	
	public static String getTextContentValue(Element node, String elementname) {
		return getTextContentValue(node.getChildNodes(), elementname);
	}
	
}
