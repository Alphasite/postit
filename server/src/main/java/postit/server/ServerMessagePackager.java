package postit.server;

import org.json.JSONObject;
import postit.server.model.ServerAccount;
import postit.shared.MessagePackager;

import java.util.InputMismatchException;
import java.util.List;

import static postit.shared.MessagePackager.Asset.ACCOUNT;
import static postit.shared.MessagePackager.typeToString;

/**
 * Packages requests and responses in JsonObject to be sent between client and server
 * @author Ning
 *
 */
public class ServerMessagePackager {
	/**
	 * Takes inputs and package them into the string representation of a single JSONObject
	 * @param status - whether the response to the request is success or failure
	 * @param message - message to be displayed when response failed
	 * @param asset - the return type
	 * @param bean - returned object
	 * @return
	 */
	public static String createResponse(boolean status, String username, String message, MessagePackager.Asset asset, Object bean){
		JSONObject response = new JSONObject();
		response.put("username", username);
		if (status){
			response.put("status", "success");
			if (bean != null){
				if (asset == ACCOUNT || asset == MessagePackager.Asset.KEYCHAIN)
					response.put(typeToString(asset), new JSONObject(bean));
				else
					response.put(typeToString(asset), bean);
			}
		}
		else{
			response.put("status", "failure");
			response.put("message", message);
		}
		return response.toString();
	}
}
