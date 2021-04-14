package network.nerve.swap;

import io.nuls.core.log.Log;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Niels
 */
public class JunitUtils {
    public static void execute(List<JunitCase> caseList, JunitExecuter executer) {
        for (JunitCase item : caseList) {
            try {
                Object get = executer.execute(item);

                if (null == get && item.getWant() == null) {
                    continue;
                } else if (null == get && item.getWant() != null) {
                    assertTrue(item.getKey() + " : " + item.getMessage() + " want:" + item.getWant() + ",bug get:" + get, false);
                } else if (null != get && item.getWant() == null) {
                    assertTrue(item.getKey() + " : " + item.getMessage() + " want:" + item.getWant() + ",bug get:" + get, false);
                } else if (get.equals(item)) {
                    //什么也不做
                } else if (!Arrays.deepEquals(new Object[]{get}, new Object[]{item.getWant()})) {
                    assertTrue(item.getKey() + " : " + item.getMessage() + " want:" + item.getWant() + ",bug get:" + get, false);
                }
            } catch (Exception e) {

                if (item.isWantEx()) {
                    if (!item.getExClass().equals(e.getClass())) {
                        Log.error(e);
                        assertTrue(item.getKey() + " : " + item.getMessage(), false);

                    }
                } else {
                    Log.error(e);
                    assertTrue(item.getKey() + " : " + item.getMessage(), false);
                }
            }
        }
    }
}
