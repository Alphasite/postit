package postit.client.keychain;

import sun.security.rsa.RSAPublicKeyImpl;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Created by nishadmathur on 16/4/17.
 */
public class Share {
    public long serverid;
    public String username;
    public boolean canWrite;
    public RSAPublicKey publicKey;

    public Share(long serverid, String username, boolean readWrite, RSAPublicKey publicKey) {
        this.serverid = serverid;
        this.username = username;
        this.canWrite = readWrite;
        this.publicKey = publicKey;
    }

    public Share(JsonObject object) throws InvalidKeyException {
        this.serverid = object.getJsonNumber("serverid").longValue();
        this.username = object.getString("username");
        this.canWrite = object.getBoolean("canWrite");
        this.publicKey = new RSAPublicKeyImpl(
                object.getJsonNumber("publickey-modulus").bigIntegerValue(),
                object.getJsonNumber("publickey-exponent").bigIntegerValue()
        );
    }

    public JsonObjectBuilder dump() {
        return Json.createObjectBuilder()
                .add("serverid", serverid)
                .add("username", username)
                .add("canWrite", canWrite)
                .add("publickey-modulus", publicKey.getModulus())
                .add("publickey-exponent", publicKey.getPublicExponent());
    }
}
