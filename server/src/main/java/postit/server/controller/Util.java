package postit.server.controller;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.bouncycastle.util.encoders.Base64;

public class Util {

	private static final int hashIters = 4096;
	private static final int keyLength = 256;
	
	public static String generateSalt(SecureRandom rand, int len){
		byte[] s = new byte[len];
		rand.nextBytes(s);
		return Base64.toBase64String(s);
	}
	
	public static String hashPassword(String password, String salt){
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.decode(salt), hashIters, keyLength);
	    SecretKeyFactory skf;
		try {
			skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			return Base64.toBase64String(skf.generateSecret(spec).getEncoded());
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
	    
		return null;
	}
	
	/**
	 * Given plaintext password1 and hashed password2, check if they match
	 * @param password1
	 * @param salt
	 * @param password2
	 * @return
	 */
	public static boolean comparePasswords(String password1, String salt, String password2){
		String hashed = hashPassword(password1, salt);
		return hashed.equals(password2);
	}
}
