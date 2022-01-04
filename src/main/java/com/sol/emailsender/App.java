package com.sol.emailsender;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.codec.digest.DigestUtils;
import com.google.crypto.tink.subtle.AesGcmJce;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;

public class App {
	static Logger log = Logger.getLogger("EmailSender");
	static String thisDirectory = System.getProperty("user.dir");
	public static void main(String[] args) {
		init();
		String configFilePath = thisDirectory + File.separator + "config.json";
		String manFilePath = thisDirectory + File.separator + "help.json";
		String configFileRaw = readFile(configFilePath);
		if (configFileRaw.isEmpty()) {
			System.exit(0);
		}
		@SuppressWarnings("unchecked")
		Map<String, String> configMap = new Gson().fromJson(configFileRaw, Map.class);
		String mainPath = configMap.get("path");
		if (mainPath.equals("user.dir") || mainPath.equals("user.home")) {
			mainPath = System.getProperty(mainPath);
		}
		String payloadName = configMap.get("payloadName");
		String keyPath = mainPath + File.separator + configMap.get("dataName");
		String payloadPath = mainPath + File.separator + payloadName;
		boolean isHelpTextDisplayed = Boolean.valueOf(configMap.get("isHelpTextDisplayed"));
		
		String manFileRaw = readFile(manFilePath);
		if (manFileRaw.isEmpty()) {
			System.exit(0);
		}
		@SuppressWarnings("unchecked")
		Map<String, String> manMap = new Gson().fromJson(manFileRaw, Map.class);
		String createHelp = manMap.get("createHelp");
		String encryptHelp = manMap.get("encryptHelp");
		String sendHelp = manMap.get("sendHelp").replaceAll("payloadName", payloadName);
		String configCreated = manMap.get("configCreated").replaceAll("payloadPath", payloadPath);
		String passwordPrompt = manMap.get("passwordPrompt");
		String passwordEncrypted = manMap.get("passwordEncrypted");
		String readConfgMsg = manMap.get("readConfing");
		String dataNotFoundMsg = manMap.get("dataNotFound").replaceAll("encryptHelp", new String(encryptHelp));
		String dataErrMsg = manMap.get("dataErrMsg").replaceAll("encryptHelp", new String(encryptHelp));
		String configErrMsg = manMap.get("configErrMsg").replaceAll("createHelp", createHelp);
		String parsErrMsg = manMap.get("parsErrMsg").replaceAll("payloadName", payloadName);
		String sendMsg = manMap.get("sendMsg");
		String sendMsgOK = manMap.get("sendMsgOK");
		String helpText = manMap.get("help");
		
		if (args.length > 0) {
			if (args[0].equals("--help")) {
				System.out.println(createHelp);
				System.out.println(encryptHelp);
				System.out.println(sendHelp);
			}

			try {

				if (args[0].equals("-c") || args[0].equals("--create")) {
					PayloadObject payload = new PayloadObject("emailAddressFrom@mail.com",
							new HashSet<String>(Arrays.asList("emailAddressTo1@mail.com")), "your subject",
							"your message", "smtp.gmail.com", "465", "true");
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					String json = gson.toJson(payload);
					writeFile(payloadPath, json);
					if (isHelpTextDisplayed) System.out.println(configCreated);
				}
				if (args[0].equals("-e") || args[0].equals("--encrypt")) {
					Console console = System.console();
					char[] password = console.readPassword(passwordPrompt);
					String aad = new String(DigestUtils.sha256(String.valueOf(System.currentTimeMillis())));
					String key = new String(DigestUtils.sha256(new String(password)));
					AesGcmJce agjEncryption = null;
					try {
						agjEncryption = new AesGcmJce(key.getBytes());
					} catch (GeneralSecurityException e) {
						log.error(e.getMessage());
					}
					byte[] encrypted = null;
					try {
						encrypted = agjEncryption.encrypt(new String(password).getBytes(), aad.getBytes());
					} catch (GeneralSecurityException e) {
						log.error(e.getMessage());
					}
					Arrays.fill(password, ' ');
					byte[] data = createData(encrypted, key.getBytes(), aad.getBytes());
					try {
						Files.write(Paths.get(keyPath), data);
					} catch (IOException e) {
						log.error(e.getMessage());
					}
					System.out.println(passwordEncrypted);
				}
				if (args[0].equals("-s") || args[0].equals("--send")) {
					if (isHelpTextDisplayed) System.out.println(readConfgMsg);

					byte[] data2 = null;
					try {
						data2 = Files.readAllBytes(Paths.get(keyPath));
					} catch (IOException e) {
							log.error(e.getMessage());							
					}
					if (data2 == null) {
						if (isHelpTextDisplayed) System.err.println(dataNotFoundMsg);
						System.exit(0);
					}
					
					byte[][] edata = fetchData(data2);
					AesGcmJce agjDecryption = null;
					try {
						agjDecryption = new AesGcmJce(edata[1]);
					} catch (GeneralSecurityException e) {
						log.error(e.getMessage());
					}
					byte[] decrypted = null;
					try {
						decrypted = agjDecryption.decrypt(edata[2], edata[0]);
					} catch (GeneralSecurityException e) {
						log.error(e.getMessage());
					}
					if (decrypted == null) {
						if (isHelpTextDisplayed) System.err.println(dataErrMsg);
						System.exit(0);
					} 
					String rawResult = readFile(payloadPath);
					if (rawResult.isEmpty()) {
						if (isHelpTextDisplayed) System.err.println(configErrMsg);
						System.exit(0);
					}
					PayloadObject payloadRes = new Gson().fromJson(rawResult, PayloadObject.class);
					if (payloadRes == null) {
						if (isHelpTextDisplayed) System.err.println(parsErrMsg);
						System.exit(0);
					}
					
					if (isHelpTextDisplayed) System.out.println(sendMsg);

					boolean result = sendEmails(payloadRes, new String(decrypted));
					
					if (isHelpTextDisplayed && result) System.out.println(sendMsgOK);
				}

			} catch (Exception e) {
				log.error(e.getMessage());
			}
		} else {
			System.out.println(helpText);
			System.exit(0);
		}
	}

	private static void init() {
		String configFilePath = thisDirectory + File.separator + "config.json";
		File configFile = new File(configFilePath);
		if(!configFile.exists()) { 
			Map<String, String> configs = new HashMap<String, String>();
			configs.put("path", "user.dir");
			configs.put("dataName", "data.ebf");
			configs.put("payloadName", "payload.json");
			configs.put("isHelpTextDisplayed", "true");
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
	        String configJson = gson.toJson(configs);
			writeFile(configFilePath, configJson);
		}
		
		String manFilePath = thisDirectory + File.separator + "help.json";
		File manFile = new File(manFilePath);
		if(!manFile.exists()) { 
			Map<String, String> manMap = new HashMap<String, String>();
			manMap.put("createHelp", "Use -c or --create to create a new config file.");
			manMap.put("encryptHelp", "Use -e or --encrypt to encrypt email password.");
			manMap.put("sendHelp", "Use -s or --send to send an email with config from payloadName file.");
			manMap.put("configCreated", "A new config file was created: payloadPath.");
			manMap.put("passwordPrompt", "Enter email password: ");
			manMap.put("passwordEncrypted", "Password was encrypted.");
			manMap.put("readConfing", "Reading the config file...");
			manMap.put("dataNotFound", "A data file not found. encryptHelp");
			manMap.put("dataErrMsg", "The data file is corrupted. encryptHelp");
			manMap.put("configErrMsg", "The config file not found. createHelp");
			manMap.put("parsErrMsg", "The config file not valid. Try to check if the payloadName file has valid JSON format.");
			manMap.put("sendMsg", "Sending an email...");
			manMap.put("sendMsgOK", "The email was successfully sent.");
			manMap.put("help", "Add --help as an argument to see list of available commands and some concept guides.");
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
	        String manJson = gson.toJson(manMap);
			writeFile(manFilePath, manJson);
		}

	}

	private static boolean sendEmails(final PayloadObject payload, final String pdata) {
		Properties prop = new Properties();
		prop.put("mail.smtp.host", payload.host);
		prop.put("mail.smtp.port", payload.port);
		prop.put("mail.smtp.auth", payload.auth);
		prop.put("mail.smtp.socketFactory.port", payload.port);
		prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

		List<String> emailAddressTo = new ArrayList<String>(payload.emailAddressTo);
		InternetAddress addresses[] = new InternetAddress[emailAddressTo.size()];
		for (int i = 0; i < addresses.length; i++) {
			try {
				addresses[i] = new InternetAddress(emailAddressTo.get(i).trim().toLowerCase());
			} catch (AddressException e) {
				log.error(e.getMessage());
			}
		}

		Session session = Session.getInstance(prop, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(payload.emailAddressFrom, pdata);
			}
		});

		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(payload.emailAddressFrom));
			message.setRecipients(Message.RecipientType.TO, addresses);
			message.setSubject(payload.subject);
			message.setText(payload.message);
			Transport.send(message);
		} catch (MessagingException e) {
			log.error(e.getMessage());
		}
		return true;
	}

	private static void writeFile(String path, String json) {
		try {
			FileWriter file = new FileWriter(path);
			file.write(json);
			file.close();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private static String readFile(String path) {
		StringBuilder stringBuilder = new StringBuilder();
		try {
			FileReader reader = new FileReader(path);
			int character;
			while ((character = reader.read()) != -1) {
				stringBuilder.append((char) character);
			}
			reader.close();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		return stringBuilder.toString();
	}

	private static byte[] createData(byte[] cipher, byte[] key, byte[] aad) {
		List<Byte> data = new ArrayList<Byte>();
		for (int i = 0; i < aad.length; i++)
			data.add(aad[i]);
		for (int i = 0; i < key.length; i++)
			data.add(key[i]);
		for (int i = 0; i < cipher.length; i++)
			data.add(cipher[i]);
		byte[] adata = new byte[data.size()];
		for (int i = 0; i < data.size(); i++)
			adata[i] = data.get(i);
		return adata;
	}

	private static byte[][] fetchData(byte[] data) {
		int len = new String(DigestUtils.sha256(String.valueOf(System.currentTimeMillis()))).getBytes().length;
		List<Byte> aad = new ArrayList<Byte>();
		List<Byte> key = new ArrayList<Byte>();
		List<Byte> cipher = new ArrayList<Byte>();
		for (int i = 0; i < data.length; i++) {
			if (i < len) {
				aad.add(data[i]);
			} else if (i < len * 2) {
				key.add(data[i]);
			} else {
				cipher.add(data[i]);
			}
		}
		byte[] aada = new byte[aad.size()];
		for (int i = 0; i < aad.size(); ++i)
			aada[i] = aad.get(i);
		byte[] keya = new byte[key.size()];
		for (int i = 0; i < key.size(); ++i)
			keya[i] = key.get(i);
		byte[] ciphera = new byte[cipher.size()];
		for (int i = 0; i < cipher.size(); ++i)
			ciphera[i] = cipher.get(i);
		byte[][] pdata = new byte[data.length][3];
		pdata[0] = aada;
		pdata[1] = keya;
		pdata[2] = ciphera;
		return pdata;
	}
}
