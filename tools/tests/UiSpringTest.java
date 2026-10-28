import net.parkabird.changedsynergy.client.UiSpring;

public final class UiSpringTest {
    public static void main(String[] args) {
        var low = new UiSpring(0); var high = new UiSpring(0);
        low.snap(0,0); high.snap(0,0); low.target(1,0); high.target(1,0);
        for(int i=1;i<=15;i++)low.value(i*1_000_000_000L/15);
        for(int i=1;i<=240;i++)high.value(i*1_000_000_000L/240);
        check(Math.abs(low.value(1_000_000_000L)-high.value(1_000_000_000L))<1e-10,"frame independent response");
        low.snap(0,0);low.target(1,0);double position=low.value(70_000_000),velocity=low.velocity(70_000_000);
        low.target(0,70_000_000);check(low.value(70_000_000)==position&&low.velocity(70_000_000)==velocity,"retarget preserves momentum");
        check(low.settled(2_000_000_000L,.0001),"interrupted spring settles");
        check(Double.isFinite(low.value(60_000_000_000L)),"long suspension stays finite");
        check(UiSpring.clamp(-.2)==0&&UiSpring.clamp(1.2)==1,"opacity remains bounded");
        var geometry=new UiSpring(0,30,1);geometry.snap(0,0);geometry.target(220,0);double previous=0;
        for(int i=1;i<=240;i++) {
            double value=geometry.displayValue(i*1_000_000_000L/240,.01);
            check(value>=previous&&value<=220,"wheel geometry approaches without pixel-scale backtracking");previous=value;
        }
        check(geometry.displayValue(1_000_000_000L,.01)==220,"display uses an exact resting pixel");
        for(int i=1;i<=120;i++)check(geometry.displayValue(1_000_000_000L+i*1_000_000L,.01)==220,"resting geometry does not flicker");
        System.out.println("UI_SPRING_PASS: frame rate, interruption, settling, suspension and opacity");
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
