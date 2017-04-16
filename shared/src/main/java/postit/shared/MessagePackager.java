package postit.shared;

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
	 * - add shared keychain
	 * - update account
	 * - update keychain
	 * - remove account
	 * - remove keychain
	 * - get account
	 * - get keychain
	 * - get keychains
	 * - get shared keychains
	 * - authenticate account
	 *
	 */
	
	public enum Action {
		ADD,
		UPDATE,
		REMOVE,
		GET,
		AUTHENTICATE
	}

	public enum Asset {
		ACCOUNT,
		ACCOUNTS,
		KEYCHAIN,
		KEYCHAINS,
		SHARED_KEYCHAINS
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
		case SHARED_KEYCHAINS:
			return "shared keychains";
		default:
			return null;
		}
	}
	
	/*
	public static void main(String[] args){
		List<ServerAccount> acts = new ArrayList<ServerAccount>();
		ServerAccount act = new ServerAccount("ning", "1234", null, "ning", "wang");
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
