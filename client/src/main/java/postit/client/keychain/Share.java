package postit.client.keychain;

import sun.security.rsa.RSAPublicKeyImpl;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;

/**
 * Created by nishadmathur on 16/4/17.
 */
public class Share {
    public static final String SERVERID = "serverid";
    public static final String USERNAME = "username";
    public static final String CAN_WRITE = "canWrite";
    public static final String ENCRYPT_PUBLICKEY_MODULUS = "encrypt-publickey-modulus";
    public static final String ENCRYPT_PUBLICKEY_EXPONENT = "encrypt-publickey-exponent";
    public static final String SIGN_PUBLICKEY_MODULUS = "sign-publickey-modulus";
    public static final String SIGN_PUBLICKEY_EXPONENT = "sign-publickey-exponent";
    public static final String IS_OWNER = "isOwner";

    public long serverid;
    public String username;
    public boolean canWrite;
    public RSAPublicKey encryptionKey;
    public RSAPublicKey signatureKey;
    public boolean isOwner;

    public Share(long serverid, String username, boolean readWrite, RSAPublicKey encryptionKey, RSAPublicKey signatureKey, boolean isOwner) {
        this.serverid = serverid;
        this.username = username;
        this.canWrite = readWrite;
        this.encryptionKey = encryptionKey;
        this.signatureKey = signatureKey;
        this.isOwner = isOwner;
    }

    public Share(JsonObject object) throws InvalidKeyException {
        this.serverid = object.getJsonNumber(SERVERID).longValue();
        this.username = object.getString(USERNAME);
        this.canWrite = object.getBoolean(CAN_WRITE);
        this.encryptionKey = new RSAPublicKeyImpl(
                object.getJsonNumber(ENCRYPT_PUBLICKEY_MODULUS).bigIntegerValue(),
                object.getJsonNumber(ENCRYPT_PUBLICKEY_EXPONENT).bigIntegerValue()
        );
        this.signatureKey = new RSAPublicKeyImpl(
                object.getJsonNumber(SIGN_PUBLICKEY_MODULUS).bigIntegerValue(),
                object.getJsonNumber(SIGN_PUBLICKEY_EXPONENT).bigIntegerValue()
        );
        this.isOwner = object.getBoolean(IS_OWNER);
    }

    public JsonObjectBuilder dump() {
        return Json.createObjectBuilder()
                .add(SERVERID, serverid)
                .add(USERNAME, username)
                .add(CAN_WRITE, canWrite)
                .add(IS_OWNER, isOwner)
                .add(ENCRYPT_PUBLICKEY_MODULUS, encryptionKey.getModulus())
                .add(ENCRYPT_PUBLICKEY_EXPONENT, encryptionKey.getPublicExponent())
                .add(SIGN_PUBLICKEY_MODULUS, signatureKey.getModulus())
                .add(SIGN_PUBLICKEY_EXPONENT, signatureKey.getPublicExponent());
    }
}
