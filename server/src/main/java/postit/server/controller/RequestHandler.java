package postit.server.controller;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.json.JSONObject;
import postit.server.model.ServerAccount;
import postit.server.model.ServerKeychain;
import postit.shared.EFactorAuth;
import postit.shared.MessagePackager;
import postit.shared.MessagePackager.Action;
import postit.shared.MessagePackager.Asset;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import static postit.server.ServerMessagePackager.createResponse;

/**
 *
 * @author Ning
 *
 */
public class RequestHandler extends SimpleChannelInboundHandler<String> {
	private final static Logger LOGGER = Logger.getLogger(RequestHandler.class.getName());

	private AccountHandler ah;
	private KeychainHandler kh;
	private LogController lc;

	private boolean authenticated;

	public RequestHandler(AccountHandler accountHandler, KeychainHandler keychainHandler, LogController logController) throws ExceptionInInitializerError {
		System.out.println("Initialised new Request Handler");
		ah = accountHandler;
		kh = keychainHandler;
		lc = logController;
		authenticated = false;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		System.out.println("Starting request handle...");
		msg = new String(Base64.getDecoder().decode(msg), StandardCharsets.UTF_8);
		String response = Base64.getEncoder().encodeToString(handleRequest(msg).getBytes(StandardCharsets.UTF_8));
		ChannelFuture send = ctx.writeAndFlush(response + "\r\n");
		send.sync();
		ctx.close();
		LOGGER.info("Request successfully handled.");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
	}

	/**
	 * Takes in request, process the request, and outputs the proper response
	 * @param request json as string
	 * @return response string
	 */
	public String handleRequest(String request) {
		//TODO refactor this gigantic thing using multiple engine classes that handle requests
		// associated to specific assets

		JSONObject json = new JSONObject(request);

		Action act = Action.valueOf(json.getString("action"));
		Asset asset = Asset.valueOf(json.getString("asset"));

		String username; // only empty for ADD ACCOUNT

		if (!json.has("username") || !json.has("password")) {
			if (act != Action.ADD && asset != Asset.ACCOUNT) {
				LOGGER.info("Malformed request missing login details");
				return createResponse(false, "", "Missing username or password as input", asset, null);
			}

			username = "";
		} else {
			String password;

			username = json.getString("username");
			password = json.getString("password");
			password = new String(Base64.getDecoder().decode(password),StandardCharsets.UTF_8);
			
			int numFails = lc.getLatestNumFailedLogins(username);
			if (numFails > 4){
				// disabled time is linear right now. may change to exponential
				long diff = (numFails - 4) * 30 - (System.currentTimeMillis() - lc.getLastLoginTime(username)) / 1000;
				if (diff > 0){
					LOGGER.info("Sign in disabled");
					return createResponse(false, username, String.format("Login is temporarily disabled. Try again in %d seconds.", diff), 
							asset, null);
				}
			}

			// TODO check this.
			if (/*!authenticated &&*/ !ah.authenticate(username, password)) {
				LOGGER.info("Incorrect sign in attempt");
				return createResponse(false, username, "Incorrect login information.", asset, null);
			} else {
				authenticated = true;
			}
		}

		String assetName = MessagePackager.typeToString(asset).toLowerCase();
		LOGGER.info("Handling request of type: " + act.toString() + " " + assetName);
		JSONObject obj = null;

		if (json.has(assetName)) {
			obj = json.getJSONObject(assetName);
		}

		JSONObject js;
		List<ServerKeychain> list;

		switch(act){
		case ADD:
			switch(asset){
			case ACCOUNT:
				ServerAccount serverAccount = new ServerAccount(
						obj.getString("username"),
						obj.getString("password"),
						obj.getString("email"),
						obj.getString("firstname"),
						obj.getString("lastname"),
						obj.getString("phoneNumber")
				);

				if (ah.addAccount(serverAccount)) {
					LOGGER.info("Success: Created serverAccount for " + serverAccount.getUsername());
					return createResponse(true, serverAccount.getUsername(), "", asset, serverAccount);
				}
				else
					return createResponse(false, "", "Failed to create new serverAccount", asset, null);
			case KEYCHAIN:
				js = kh.createKeychain(username, obj.getString("name"));
				if (js.getString("status").equals("success")){
					ServerKeychain keychain = new ServerKeychain(
							js.getInt("directoryEntryId"),
							username,
							-1,
							null,
							false,
							obj.getString("name"),
							""
					);

					LOGGER.info("Success: Created directory " + keychain.getName() + " with " + username);
					return createResponse(true, username, "", asset, keychain);
				} else {
					return js.toString();
				}
			case SHARED_KEYCHAIN:
				if (ah.getAccount(obj.getString("sharedUsername")) == null) {
					return createResponse(false, "", "Shared user does not exist.", asset, null);
				}

				js = kh.shareKeychain(
						username,
						obj.getString("sharedUsername"),
						obj.getBoolean("sharedHasWritePermission"),
						obj.getLong("ownerDirectoryEntryId")
				);

				if (js.getString("status").equals("success")) {
					ServerKeychain keychain = new ServerKeychain(
							js.getInt("directoryEntryId"),
							username,
							obj.getLong("ownerDirectoryEntryId"),
							obj.getString("sharedUsername"),
							obj.getBoolean("sharedHasWritePermission"),
							"",
							""
					);

					LOGGER.info("Success: Created directory " + keychain.getName() + " with " + username);
					return createResponse(true, username, "", asset, keychain);
				} else {
					return js.toString();
				}
			default:
				break;
			}
		case AUTHENTICATE:
			switch(asset){
			case ACCOUNT:
				return createResponse(true, username, "", asset, null);
			case KEYPAIR:
				// TODO fill this in Zhan.
				ServerAccount serverAccount = ah.getAccount(username);
				String otp = json.getString("keypair");
				String phoneNumber = serverAccount.getPhoneNumber();
				// check otp.
				boolean otpSuccessfullyAuthenticated = new EFactorAuth().verifyMsg(phoneNumber, otp);
				if (otpSuccessfullyAuthenticated) {
					String keypair = ah.getAccount(username).getKeypair();
					return createResponse(true, username, "", asset, keypair);
				} else {
					return createResponse(false, username, "otp wrong or expired.", asset, null);
				}
			default:
				break;
			}
		case GET:
			switch(asset){
			case ACCOUNT:
				if (!username.equals(obj.getString("username")))
					return createResponse(false, username, "ServerAccount information has wrong username", asset, null);
				ServerAccount serverAccount = ah.getAccount(username);
				if (serverAccount != null)
					return createResponse(true, username, "", asset, serverAccount);
				else
					return createResponse(false, username, "Unable to get serverAccount information of " + username, asset, null);
			case KEYCHAIN:
				int keychainId = obj.getInt("directoryEntryId");
				ServerKeychain keychain = null;
				if (keychainId != -1) keychain = kh.getKeychain(username, keychainId);
				if (keychain != null)
					return createResponse(true, username, "", asset, keychain);
				else
					return createResponse(false, username, "Unable to get keychain information of " + keychainId, asset, null);
			case KEYCHAINS:
				list = kh.getKeychains(username);
				return createResponse(true, username, "", asset, list);

			case SHARED_KEYCHAINS:
				list = kh.getSharedKeychains(username, obj.getLong("ownerDirectoryEntryId"));
				return createResponse(true, username, "", asset, list);

			case OWNER_KEYCHAIN:
				ServerKeychain keychain1 = kh.getOwnersKeychain(username, obj.getLong("directoryEntryId"));
				return createResponse(true, username, "got owners keychain", asset, keychain1);

			case KEYPAIR:
				// TODO fill this in Zhan.
				ServerAccount serverAccount1 = ah.getAccount(username);
				String phoneNumber = serverAccount1.getPhoneNumber();
				new EFactorAuth().sendMsg(phoneNumber);
				// send otp.
				return createResponse(true, username, "Please send otp.", asset, null);

			default:
				break;
			}
			break;
		case REMOVE:
			switch(asset){
			case ACCOUNT:
				if (! username.equals(obj.getString("username")))
					return createResponse(false, username, "ServerAccount information has wrong username", asset, null);
				if (ah.removeAccount(username, obj.getString("password")))
					return createResponse(true, username, "", asset, null);
				else
					return createResponse(false, username, "Unable to remove account " + username, asset, null);
			case KEYCHAIN:
				long deId;

				if (!obj.has("directoryEntryId") || obj.getInt("directoryEntryId") == -1){
					return createResponse(false, username, "Must provide id to remove keychain", asset, null);
				} else {
					deId = obj.getInt("directoryEntryId");
				}

				if (kh.removeKeychain(username, deId))
					return createResponse(true, username, "", asset, null);
				else
					return createResponse(false, username, "Unable to remove keychain " + deId, asset, null);
			case SHARED_KEYCHAIN:
				// TODO
			default:
				break;
			}
		case UPDATE:
			switch(asset){
			case ACCOUNT:
				ServerAccount serverAccount = new ServerAccount(obj.getString("username"), obj.getString("password"), obj.getString("email"),
						obj.getString("firstname"), obj.getString("lastname"), obj.getString("phoneNumber"));
				if (ah.updateAccount(serverAccount))
					return createResponse(true, username, "", asset, serverAccount);
				else
					return createResponse(false, username, "Unable to update serverAccount information of " + serverAccount.getUsername(),
							asset, null);
			case KEYCHAIN:
				ServerKeychain keychain = new ServerKeychain();
				if (obj.has("ownerUsername")) keychain.setOwnerUsername(obj.getString("ownerUsername"));
				if (obj.has("directoryEntryId")) keychain.setDirectoryEntryId(obj.getInt("directoryEntryId"));
				if (obj.has("ownerDirectoryEntryId")) keychain.setOwnerDirectoryEntryId(obj.getInt("ownerDirectoryEntryId"));
				if (obj.has("sharedUsername")) keychain.setSharedUsername(obj.getString("sharedUsername"));
				if (obj.has("sharedHasWritePermission")) keychain.setSharedHasWritePermission(obj.getBoolean("sharedHasWritePermission"));
				if (obj.has("name")) keychain.setName(obj.getString("name"));
				if (obj.has("data")) keychain.setData(obj.getString("data"));

				if (keychain.getDirectoryEntryId() == -1){
					return createResponse(false, username, "Keychain lacks server id", asset, keychain);
				}
				else if (kh.updateKeychain(username, keychain))
					return createResponse(true, username, "", asset, keychain);
				else
					return createResponse(false, username, "Unable to update keychain information of " + keychain.getName(), asset, null);

			case SHARED_KEYCHAIN:
				boolean applied = kh.setSharedKeychainWriteable(
						username,
						obj.getLong("ownerDirectoryEntryId"),
						obj.getString("sharedUsername"),
						obj.getBoolean("sharedHasWritePermission")
				);

				if (applied)
					return createResponse(true, username, "Successfully changed write ability", asset, null);
				else
					return createResponse(false, username, "Unable to update write ability information of " + obj.getLong("id"), asset, null);

			default:
				break;
			}
		default:
			break;
		}

		return createResponse(false, username, String.format("Invalid parameters: (%s, %s)", act, asset), null, null);
	}
}
