package postit.shared;

import com.authy.*;
import com.authy.api.*;
/**
 * Created by zhanzhao on 4/18/17.
 */
public class EFactorAuth {
    AuthyApiClient client;
    private final String apiKey = "jnZu9K5nybYbjMbvRcvgsb2wJk7u4qOq";

    public EFactorAuth () {
        client = new AuthyApiClient("jnZu9K5nybYbjMbvRcvgsb2wJk7u4qOq");
    }
    public boolean sendMsg(String phoneNumber) {
        PhoneVerification phoneVerification = client.getPhoneVerification();

        Verification verification;
        Params params = new Params();
        params.setAttribute("locale", "en");

        verification = phoneVerification.start(phoneNumber, "1", "sms", params);

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

        System.out.println(verificationCode.getMessage());
        System.out.println(verificationCode.getIsPorted());
        System.out.println(verificationCode.getSuccess());
        System.out.print(verificationCode.isOk());

        return verificationCode.isOk();
    }

//    public static void main (String[] args) {
//        new EFactorAuth().sendMsg("3154508771");
//        new EFactorAuth().verifyMsg("3154508771", "9614");
//    }

}
