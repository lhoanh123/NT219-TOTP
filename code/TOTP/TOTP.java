package TOTP;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.GeneralSecurityException;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;


public class TOTP {

    private TOTP() {
    }
    
    //tính toán giá trị HMAC
    private static byte[] hmac_sha(String crypto, byte[] keyBytes,
            byte[] text) {
        try {
            Mac hmac;
            hmac = Mac.getInstance(crypto);
            SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
            hmac.init(macKey);
            return hmac.doFinal(text);
        } catch (GeneralSecurityException gse) {
            throw new UndeclaredThrowableException(gse);
        }
    }

    //chuyển đổi một chuỗi ký tự hex (hệ cơ số 16) thành mảng byte.
    private static byte[] hexStr2Bytes(String hex) {
        byte[] bArray = new BigInteger("10" + hex, 16).toByteArray();

        byte[] ret = new byte[bArray.length - 1];
        for (int i = 0; i < ret.length; i++)
            ret[i] = bArray[i + 1];
        return ret;
    }

    private static final int[] DIGITS_POWER
            = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000 };

    //phiên bản nâng cao của hàm generateTOTP, 
    //cho phép người dùng xác định thuật toán mã hóa (crypto) để tính toán mã OTP.
    public static String generateTOTP(String key,
            String time,
            String returnDigits,
            String crypto) {
        int codeDigits = Integer.decode(returnDigits).intValue();
        String result = null;

        while (time.length() < 16)
            time = "0" + time;

        byte[] msg = hexStr2Bytes(time);
        byte[] k = hexStr2Bytes(key);
        byte[] hash = hmac_sha(crypto, k, msg);

        int offset = hash[hash.length - 1] & 0xf;

        int binary = ((hash[offset] & 0x7f) << 24) |
                ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) << 8) |
                (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[codeDigits];

        result = Integer.toString(otp);
        while (result.length() < codeDigits) {
            result = "0" + result;
        }
        return result;
    }

    public static void main(String[] args) {
        String seed = "3132333435363738393031323334353637383930";
        long T0 = 0;
        long X = 30;

        String steps = "0";
        //DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //df.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            Date currentTime = new Date();
            long currentTimestamp = currentTime.getTime();
            System.out.println("curent time: " + currentTime);
            double a = (currentTimestamp - T0) / X;
            long T = (long) Math.floor(a);
            steps = Long.toHexString(T).toUpperCase();
            while (steps.length() < 16)
                steps = "0" + steps;
            System.out.println(generateTOTP(seed, steps, "6", "HmacSHA1"));
            
        } catch (final Exception e) {
            System.out.println("Error : " + e);
        }
    }

    //tạo mã TOTP dựa trên thời gian hiện tại
    public static String TOTP_now(String key, String returnDigits, String crypto)
    {
        String result = "0";
        long currentTimeMillis = System.currentTimeMillis();
        long currentTimeSeconds = currentTimeMillis / 1000L;
        long T0 = 0;
        long X = 30;
        String steps = "0";
        double a = (currentTimeSeconds - T0)/X;
        long T = (long)Math.floor(a);
        steps = Long.toHexString(T).toUpperCase();
        while (steps.length() < 16) steps = "0" + steps;
        result = TOTP.generateTOTP(key, steps, returnDigits, crypto);
        return result;
    }
}