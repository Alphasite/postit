package postit.shared;

import com.authy.AuthyApiClient;
import com.authy.api.Params;
import com.authy.api.PhoneVerification;
import com.authy.api.Verification;

import javax.swing.*;
import java.security.NoSuchAlgorithmException;

/**
 * Created by zhanzhao on 4/18/17.
 */
public class EFactorAuth {
    AuthyApiClient client;
    private final static String apiKey = "jnZu9K5nybYbjMbvRcvgsb2wJk7u4qOq";

    public EFactorAuth () {
        client = new AuthyApiClient("jnZu9K5nybYbjMbvRcvgsb2wJk7u4qOq");
    }
    public boolean sendMsg(String phoneNumber) {
        PhoneVerification phoneVerification = client.getPhoneVerification();

        Verification verification;
        Params params = new Params();
        params.setAttribute("locale", "en");

        verification = phoneVerification.start(phoneNumber, "1", "sms", params);

        System.out.println("SEND");
        System.out.println(verification.getMessage());
        System.out.println(verification.getIsPorted());
        System.out.println(verification.getSuccess());
        System.out.println(verification.isOk());

        return verification.isOk();
    }

    public boolean verifyMsg (String phoneNumber, String code) {
        PhoneVerification phoneVerification = client.getPhoneVerification();

        Verification verificationCode;
        verificationCode = phoneVerification.check(phoneNumber, "1", code);

        System.out.println("VERIFY");
        System.out.println(verificationCode.getMessage());
        System.out.println(verificationCode.getIsPorted());
        System.out.println(verificationCode.getSuccess());
        System.out.print(verificationCode.isOk());

        return verificationCode.isOk();
    }

    public static void main (String[] args) throws NoSuchAlgorithmException {
        Crypto.init();
        new EFactorAuth().sendMsg("6073794979");
        String code = JOptionPane.showInputDialog("PIN");
        new EFactorAuth().verifyMsg("6073794979", code);
    }

}
