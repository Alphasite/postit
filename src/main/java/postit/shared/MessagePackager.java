package postit.shared;

import java.util.*;

import org.json.JSONObject;

import postit.server.model.*;
import postit.shared.model.Account;

/**
 * Packages requests and responses in JsonObject to be sent between client and server
 * @author Ning
 *
 */
public class MessagePackager {

	/**
	 * Currently supported requests:
	 * - add account
	 * - add keychain
	 * - update account
	 * - update keychain
	 * - remove account
	 * - remove keychain
	 * - get account
	 * - get keychain
	 * - get keychains
	 * - authenticate account
	 *
	 */
	
	public enum Action {
		ADD,
		UPDATE,
		REMOVE,
		GET,
		AUTHENTICATE
	};
	
	public enum Asset {
		ACCOUNT,
		ACCOUNTS,
		KEYCHAIN,
		KEYCHAINS
	};
	
	/**
	 * Takes inputs and package them into the string representation of a single JSONObject
	 * @param req
	 * @param obj
	 * @param bean
	 * @return
	 */
	public static String createRequest(Action req, String username, Asset asset, Object bean){
		JSONObject request = new JSONObject();
		request.put("action", req);
		request.put("asset", asset);
		request.put("username", username);
		if (bean != null)
			request.put(typeToString(asset), new JSONObject(bean));
		return request.toString();
	}
	
	/**
	 * Takes inputs and package them into the string representation of a single JSONObject
	 * @param status - whether the response to the request is success or failure
	 * @param message - message to be displayed when response failed
	 * @param asset - the return type
	 * @param bean - returned object
	 * @return
	 */
	public static String createResponse(boolean status, String username, String message, Asset asset, Object bean){
		JSONObject response = new JSONObject();
		response.put("username", username);
		if (status){
			response.put("status", "success");
			if (bean != null){
				if (asset == Asset.ACCOUNT || asset == Asset.KEYCHAIN)
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
	
	public static void checkInputTypes(Asset asset, Object bean) throws InputMismatchException{
		switch(asset){
		case ACCOUNT:
			if (bean instanceof Account) return;
		case KEYCHAIN:
			if (bean instanceof Keychain) return;
		case ACCOUNTS:
			if (bean instanceof List){
				List<?> list = (List<?>) bean;
				if (list.isEmpty()) throw new InputMismatchException("Input list cannot be empty.");
				if (list.iterator().next().getClass().isInstance(new Account("","","","",""))) return;				
			}
			break;
		case KEYCHAINS:
			break;
		default:
			break;
		}
		throw new InputMismatchException("Input type of data not match header.");
	}
	
	public static String typeToString(Asset asset){
		switch(asset){
		case ACCOUNT:
			return "account";
		case ACCOUNTS:
			return "accounts";
		case KEYCHAIN:
			return "keychain";
		case KEYCHAINS:
			return "keychains";
		default:
			return null;
		}
	}
	
	/*
	public static void main(String[] args){
		List<Account> acts = new ArrayList<Account>();
		Account act = new Account("ning", "1234", null, "ning", "wang");
		acts.add(act);
		String res = createResponse(true, "ning", "", Asset.ACCOUNT, act);
		System.out.println(res);
		System.out.println(new JSONObject(new JSONObject(res).getString("account")).getString("email") == null);
		List<String> hi = new ArrayList<String>();
		hi.add("lol");
		System.out.println("Type is string? " + hi.iterator().next().getClass().isInstance(new String()));
		System.out.println(Asset.ACCOUNT == Asset.valueOf("ACCOUNT"));
	}
	*/
}
