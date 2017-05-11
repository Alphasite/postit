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
        this.serverid = object.getJsonNumber("serverid").longValue();
        this.username = object.getString("username");
        this.canWrite = object.getBoolean("canWrite");
        this.encryptionKey = new RSAPublicKeyImpl(
                object.getJsonNumber("encrypt-publickey-modulus").bigIntegerValue(),
                object.getJsonNumber("encrypt-publickey-exponent").bigIntegerValue()
        );
        this.signatureKey = new RSAPublicKeyImpl(
                object.getJsonNumber("sign-publickey-modulus").bigIntegerValue(),
                object.getJsonNumber("sign-publickey-exponent").bigIntegerValue()
        );
        this.isOwner = object.getBoolean("isOwner");
    }

    public JsonObjectBuilder dump() {
        return Json.createObjectBuilder()
                .add("serverid", serverid)
                .add("username", username)
                .add("canWrite", canWrite)
                .add("isOwner", isOwner)
                .add("encrypt-publickey-modulus", encryptionKey.getModulus())
                .add("encrypt-publickey-exponent", encryptionKey.getPublicExponent())
                .add("sign-publickey-modulus", signatureKey.getModulus())
                .add("sign-publickey-exponent", signatureKey.getPublicExponent());
    }
}
