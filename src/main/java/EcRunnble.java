import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.concurrent.atomic.AtomicBoolean;

public class EcRunnble {
    protected MutableBoolean kill;

    public EcRunnble() {
        this.kill = new MutableBoolean(false);
    }

    public void setKill() {
        this.kill.setTrue();
    }

}
