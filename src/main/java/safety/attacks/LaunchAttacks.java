package safety.attacks;

public class LaunchAttacks {
    public static void main(String[] args) {
        new Thread(DDoSAttack::new).start();
        new Thread(SYNFloodAttack::new).start();
        new Thread(PingOfDeathAttack::new).start();
        new Thread(MITMAttack::new).start();
        new Thread(PortScanningAttack::new).start();
    }
}