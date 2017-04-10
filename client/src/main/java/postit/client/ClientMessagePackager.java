package postit.client;

import org.json.JSONObject;
import postit.shared.MessagePackager;

import java.util.Base64;

import static postit.shared.MessagePackager.typeToString;

/**
 * Packages requests and responses in JsonObject to be sent between client and server
 * @author Ning
 *
 */
public class ClientMessagePackager {
	/**
	 * Takes inputs and package them into the string representation of a single JSONObject
	 * @param req
	 * @param bean
	 * @return
	 */
	public static String createRequest(MessagePackager.Action req, postit.client.keychain.Account account, MessagePackager.Asset asset, Object bean){
		JSONObject request = new JSONObject();
		request.put("action", req);
		request.put("asset", asset);

		if (account != null) {
			request.put("username", account.getUsername());
			request.put("password", Base64.getEncoder().encodeToString(account.getSecretKey().getEncoded()));
		}

		if (bean != null)
			request.put(typeToString(asset), new JSONObject(bean));
		return request.toString();
	}
	
	public static String createTimeOutResponse(){
		JSONObject response = new JSONObject();
		response.put("status", "failure");
		response.put("message", "Time out: no response from server");
		return response.toString();
	}
}
