import github.com.gengyoubo.CE.skill.ResearchClock;

public class ResearchClockTest {
    static void check(boolean value) { if(!value)throw new AssertionError(); }
    public static void main(String[] args) {
        int progress=0,energy=0,spent=0;
        // A 3-second project needs exactly 3 whole payments, not an up-front budget.
        for(int second=0;second<10;second++) {
            var step=ResearchClock.second(progress,60,83,energy,true);
            check(!step.advanced() && step.energySpent()==0 && step.progressTicks()==0);
        }
        energy=82;
        var insufficient=ResearchClock.second(progress,60,83,energy,true);
        check(!insufficient.advanced() && insufficient.energySpent()==0);
        energy=83;
        var first=ResearchClock.second(progress,60,83,energy,true);
        check(first.advanced() && first.progressTicks()==20 && first.energySpent()==83 && !first.completed());
        progress=first.progressTicks();spent+=first.energySpent();
        // Pause, logout and unload all stop invoking paid work; disabled work is also a no-op.
        for(int i=0;i<1000;i++) {
            var paused=ResearchClock.second(progress,60,83,100_000,false);
            check(paused.progressTicks()==20 && paused.energySpent()==0 && !paused.advanced());
        }
        for(int i=0;i<2;i++) { var step=ResearchClock.second(progress,60,83,83,true);progress=step.progressTicks();spent+=step.energySpent(); }
        check(progress==60 && spent==249);
        var completed=ResearchClock.second(progress,60,83,100_000,true);
        check(completed.completed() && !completed.advanced() && completed.energySpent()==0);
        var free=ResearchClock.second(0,0,0,0,true);check(free.completed() && free.energySpent()==0);
        // Tiny rates never get rounded down by division into ticks.
        var tiny=ResearchClock.second(0,20,1,1,true);check(tiny.completed() && tiny.energySpent()==1);
        for(int duration:new int[]{-20,19}) {
            try { ResearchClock.second(0,duration,1,1,true);throw new AssertionError(); }
            catch(IllegalArgumentException expected) { }
        }
        System.out.println("PASS: whole-second WLP charging, low rates, insufficient supply, pause/resume, exact completion and free projects.");
    }
}
