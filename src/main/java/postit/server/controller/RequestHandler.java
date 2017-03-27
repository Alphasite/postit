package postit.server.controller;

import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.json.JSONObject;

import postit.server.model.ServerAccount;
import postit.server.model.ServerKeychain;
import postit.shared.MessagePackager;
import postit.shared.MessagePackager.*;

/**
 * 
 * @author Ning
 *
 */
public class RequestHandler extends SimpleChannelInboundHandler<String> {
	private final static Logger LOGGER = Logger.getLogger(RequestHandler.class.getName());

	private AccountHandler ah;
	private KeychainHandler kh;

	public RequestHandler(AccountHandler accountHandler, KeychainHandler keychainHandler) throws ExceptionInInitializerError {
		System.out.println("Initialised new Request Handler");
		ah = accountHandler;
		kh = keychainHandler;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		System.out.println("Starting request handle...");
		msg = new String(Base64.getDecoder().decode(msg));
		String response = Base64.getEncoder().encodeToString(handleRequest(msg).getBytes());
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
	 * @param request
	 * @return
	 */
	public String handleRequest(String request) {
		//TODO refactor this gigantic thing using multiple engine classes that handle requests
		// associated to specific assets
		
		JSONObject json = new JSONObject(request);

		Action act = Action.valueOf(json.getString("action"));
		Asset asset = Asset.valueOf(json.getString("asset"));

		String username; // only empty for ADD ACCOUNT
		String password;

		if (!json.has("username") || !json.has("password")) {
			if (act != Action.ADD && asset != Asset.ACCOUNT) {
				LOGGER.info("Malformed request missing login details");
				return MessagePackager.createResponse(false, "", "Missing username or password as input", asset, null);
			}

			username = "";
			password = "";
		} else {
			username = json.getString("username");
			password = json.getString("password");
			password = new String(Base64.getDecoder().decode(password));

			// TODO check this.
			if (!ah.authenticate(username, password)) {
				LOGGER.info("Incorrect sign in attempt");
				return MessagePackager.createResponse(false, username, "Incorrect login information.", asset, null);
			}
		}

		String assetName = MessagePackager.typeToString(asset).toLowerCase();
		LOGGER.info("Handling request of type: " + act.toString() + " " + assetName);
		JSONObject obj = null;
		if (json.has(assetName)) {
			obj = json.getJSONObject(assetName);
		}

		switch(act){
		case ADD:
			switch(asset){
			case ACCOUNT:
				ServerAccount serverAccount = new ServerAccount(obj.getString("username"), obj.getString("password"), obj.getString("email"),
						obj.getString("firstname"), obj.getString("lastname")); 
				if (ah.addAccount(serverAccount)) {
					LOGGER.info("Success: Created serverAccount for " + serverAccount.getUsername());
					return MessagePackager.createResponse(true, serverAccount.getUsername(), "", asset, serverAccount);
				}
				else
					return MessagePackager.createResponse(false, "", "Failed to create new serverAccount", asset, null);
			case KEYCHAIN:
				JSONObject js = kh.createKeychain(username, obj.getString("name"));
				if (js.getString("status").equals("success")){
					ServerKeychain keychain = new ServerKeychain(
							js.getInt("directoryEntryId"),
							username,
							obj.getString("name"),
							""
					);

					LOGGER.info("Success: Created directory " + keychain.getName() + " for " + username);
					return MessagePackager.createResponse(true, username, "", asset, keychain);
				}
				else 
					return js.toString();
			default:
				break;
			}
		case AUTHENTICATE:
			switch(asset){
			case ACCOUNT:
				return MessagePackager.createResponse(true, username, "", asset, null);
			default:
				break;
			}
		case GET:
			switch(asset){
			case ACCOUNT:
				if (!username.equals(obj.getString("username")))
					return MessagePackager.createResponse(false, username, "ServerAccount information has wrong username", asset, null);
				ServerAccount serverAccount = ah.getAccount(username);
				if (serverAccount != null)
					return MessagePackager.createResponse(true, username, "", asset, serverAccount);
				else
					return MessagePackager.createResponse(false, username, "Unable to get serverAccount information of " + username, asset, null);
			case KEYCHAIN:
				int keychainId = obj.getInt("directoryEntryId");
				ServerKeychain keychain;
				if (keychainId != -1) keychain = kh.getKeychain(username, keychainId);
				else keychain = kh.getKeychain(username, obj.getString("name"));
				if (keychain != null)
					return MessagePackager.createResponse(true, username, "", asset, keychain);
				else
					return MessagePackager.createResponse(false, username, "Unable to get keychain information of " + keychainId, asset, null);
			case KEYCHAINS:
				List<ServerKeychain> list = kh.getKeychains(username);
				if (list != null)
					return MessagePackager.createResponse(true, username, "", asset, list);
				else
					return MessagePackager.createResponse(false, username, "Unable to get keychains of " + username, asset, null);
			default:
				break;
			}
			break;
		case REMOVE:
			switch(asset){
			case ACCOUNT:
				if (! username.equals(obj.getString("username")))
					return MessagePackager.createResponse(false, username, "ServerAccount information has wrong username", asset, null);
				if (ah.removeAccount(username, obj.getString("password"))) 
					return MessagePackager.createResponse(true, username, "", asset, null);
				else
					return MessagePackager.createResponse(false, username, "Unable to remove account " + username, asset, null);
			case KEYCHAIN:
				long deId;

				if (!obj.has("directoryEntryId") || obj.getInt("directoryEntryId") == -1){
					return MessagePackager.createResponse(false, username, "Must provide id to remove keychain", asset, null);
				} else {
					deId = obj.getInt("directoryEntryId");
				}

				if (kh.removeKeychain(deId))
					return MessagePackager.createResponse(true, username, "", asset, null);
				else
					return MessagePackager.createResponse(false, username, "Unable to remove keychain " + deId, asset, null);
			default:
				break;
			}
		case UPDATE:
			switch(asset){
			case ACCOUNT:
				ServerAccount serverAccount = new ServerAccount(obj.getString("username"), obj.getString("password"), obj.getString("email"),
						obj.getString("firstname"), obj.getString("lastname"));
				if (ah.updateAccount(serverAccount))
					return MessagePackager.createResponse(true, username, "", asset, serverAccount);
				else
					return MessagePackager.createResponse(false, username, "Unable to update serverAccount information of " + serverAccount.getUsername(),
							asset, null);
			case KEYCHAIN:
				ServerKeychain keychain = new ServerKeychain();
				if (obj.has("ownerUsername")) keychain.setOwnerUsername(obj.getString("ownerUsername"));
				if (obj.has("directoryEntryId")) keychain.setDirectoryEntryId(obj.getInt("directoryEntryId"));
				if (obj.has("name")) keychain.setName(obj.getString("name"));
				if (obj.has("data")) keychain.setData(obj.getString("data"));

				if (keychain.getDirectoryEntryId() == -1){
					return MessagePackager.createResponse(false, username, "Keychain lacks server id", asset, keychain);
				}
				else if (kh.updateKeychain(username, keychain))
					return MessagePackager.createResponse(true, username, "", asset, keychain);
				else 
					return MessagePackager.createResponse(false, username, "Unable to update keychain information of " + keychain.getName(), asset, null);
			default:
				break;
			}
		default:
			break;
		}
		
		return MessagePackager.createResponse(false, username, String.format("Invalid parameters: (%s, %s)", act, asset), null, null);
	}
}
