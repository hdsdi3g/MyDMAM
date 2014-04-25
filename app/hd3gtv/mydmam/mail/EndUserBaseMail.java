package hd3gtv.mydmam.mail;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.util.Locale;

import javax.mail.internet.InternetAddress;

public class EndUserBaseMail implements Log2Dumpable {
	
	private static MailCenter mailcenter;
	private static InternetAddress admin_addr;
	
	static {
		try {
			mailcenter = MailCenter.getGlobal();
			admin_addr = new InternetAddress(Configuration.global.getValue("service", "administrator_mail", "root@localhost"));
		} catch (Exception e) {
			Log2.log.error("Can't init message", e);
		}
	}
	
	private EndUserBaseMail() {
	}
	
	private Locale locale;
	private InternetAddress to;
	private String mail_template;
	private MailPriority priority;
	
	public static EndUserBaseMail create(Locale locale, InternetAddress to, String mail_template) {
		EndUserBaseMail mail = new EndUserBaseMail();
		mail.locale = locale;
		if (locale == null) {
			throw new NullPointerException("\"locale\" can't to be null");
		}
		mail.to = to;
		if (to == null) {
			throw new NullPointerException("\"to\" can't to be null");
		}
		mail.mail_template = mail_template;
		if (mail_template == null) {
			throw new NullPointerException("\"mail_template\" can't to be null");
		}
		
		return mail;
	}
	
	public EndUserBaseMail setMailPriority(MailPriority priority) {
		this.priority = priority;
		return this;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("to", to);
		dump.add("locale", locale);
		dump.add("mail_template", mail_template);
		return dump;
	}
	
	public void send() {
		try {
			/**
			 * Subject
			 */
			/*if (fatal_alert) {
				subject.append("Important ! ");
			}
			if (serviceinformations != null) {
				subject.append("[");
				subject.append(serviceinformations.getApplicationShortName());
				subject.append("] ");
			}
			subject.append("General error: ");
			subject.append(basemessage);
			
			*/
			MailContent mail = mailcenter.prepareMessage("", admin_addr);
			
			/**
			 * Message
			 */
			/*StringBuffer plaintext = new StringBuffer();
			plaintext.append("An error occurred when running the application.");
			plaintext.append("\r\n");
			
			if (serviceinformations != null) {
				plaintext.append(serviceinformations.getApplicationName());
				plaintext.append(" - version ");
				plaintext.append(serviceinformations.getApplicationVersion());
				plaintext.append("\r\n");
				plaintext.append(serviceinformations.getApplicationCopyright());
				plaintext.append("\r\n");
			}
			
			mail.setPlaintext(plaintext.toString());
			
			if (fatal_alert) {
				mail.setMailPriority(MailPriority.HIGHEST);
			}
			
			mail.setFiles(files);*/
			
			// mail.send();
			
			Log2.log.info("Send an user mail", this);
		} catch (Exception e) {
			Log2.log.error("Fail to send an user mail", e, this);
		}
	}
	
}
