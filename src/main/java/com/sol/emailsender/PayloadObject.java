package com.sol.emailsender;
import java.util.Set;

public class PayloadObject implements java.io.Serializable {
	private static final long serialVersionUID = -307374798435248844L;
	public String emailAddressFrom;
	public Set<String> emailAddressTo;
	public String subject;
	public String message;
	public String host;
	public String port;
	public String auth;

	public PayloadObject() {}
	
	public PayloadObject(String emailAddressFrom, Set<String> emailAddressTo, String subject, String message, String host,
			String port, String auth) {
		super();
		this.emailAddressFrom = emailAddressFrom;
		this.emailAddressTo = emailAddressTo;
		this.subject = subject;
		this.message = message;
		this.host = host;
		this.port = port;
		this.auth = auth;
	}
	
	@Override
	public String toString() {
		return "Payload [emailAddressFrom=" + emailAddressFrom + ", emailAddressTo=" + emailAddressTo + ", subject="
				+ subject + ", message=" + message + ", host=" + host + ", port=" + port + ", auth=" + auth + "]";
	}
}
