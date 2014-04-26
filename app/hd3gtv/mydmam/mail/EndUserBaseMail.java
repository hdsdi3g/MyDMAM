package hd3gtv.mydmam.mail;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import javax.mail.internet.InternetAddress;

public class EndUserBaseMail implements Log2Dumpable {
	
	private static MailCenter mailcenter;
	// private static InternetAddress admin_addr;
	
	static {
		try {
			mailcenter = MailCenter.getGlobal();
			// admin_addr = new InternetAddress(Configuration.global.getValue("service", "administrator_mail", "root@localhost"));
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
		send(new HashMap<Object, Object>());
	}
	
	public void send(HashMap<Object, Object> mail_vars) {
		if (mail_vars == null) {
			throw new NullPointerException("\"mail_vars\" can't to be null");
		}
		try {
			MailTemplateEngine mte = new MailTemplateEngine(mail_template);
			mte.setLocale(locale);
			mte.process(mail_vars);
			MailContent mail = mailcenter.prepareMessage(mte.getSubject(), to);
			mail.setMailPriority(priority);
			mail.setHtmltext(mte.getHtml_text());
			mail.setPlaintext(mte.getPlain_text());
			mail.send();
			
			Log2Dump dump = getLog2Dump();
			for (Entry<Object, Object> entry : mail_vars.entrySet()) {
				dump.add("mail_vars: " + (String) entry.getKey(), entry.getValue());
			}
			Log2.log.info("Send an user mail", dump);
		} catch (Exception e) {
			Log2.log.error("Fail to send an user mail", e, this);
		}
	}
}
