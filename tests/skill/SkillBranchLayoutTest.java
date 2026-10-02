import github.com.gengyoubo.CE.client.*;
import java.nio.file.*;
import java.util.*;

public class SkillBranchLayoutTest {
    static void check(boolean condition) { if(!condition)throw new AssertionError(); }
    static void near(double a,double b) { check(Math.abs(a-b)<1e-8); }
    static SkillBranchLayout.Node node(String id,String branch,double x,double y) {
        return new SkillBranchLayout.Node(id,"changede:"+branch,x,y,false);
    }
    static final SkillBranchLayout.Node ROOT=new SkillBranchLayout.Node("root","changede:trunk",99,0,true);
    static SkillBranchLayout.Layout layout(List<SkillBranchLayout.Node> nodes) {
        return SkillBranchLayout.arrange(nodes,Map.of());
    }
    static void verify(List<SkillBranchLayout.Node> nodes,SkillBranchLayout.Layout layout) {
        check(layout.nodes().size()==nodes.size());
        Set<String> points=new HashSet<>();
        for(var n:nodes) {
            var point=layout.nodes().get(n.id());
            near(point.y(),n.y());
            if(n.global())near(point.x(),0);
            else check((point.x()<0)==(SkillBranchLayout.preferredSide(n.branch(),n.x())==SkillBranchLayout.Side.LEFT));
            check(points.add(point.x()+":"+point.y()));
        }
        var groups=new ArrayList<>(layout.groups().values());
        for(int i=0;i<groups.size();i++) {
            var a=groups.get(i);
            check(a.minX()>=SkillBranchLayout.TRUNK_GAP || a.maxX()<=-SkillBranchLayout.TRUNK_GAP);
            for(int j=i+1;j<groups.size();j++) {
                var b=groups.get(j);
                check(a.maxX()+SkillBranchLayout.BRANCH_GAP<=b.minX()+1e-8 || b.maxX()+SkillBranchLayout.BRANCH_GAP<=a.minX()+1e-8);
            }
        }
        var left=groups.stream().filter(b->b.maxX()<0).sorted(Comparator.comparingDouble(SkillBranchLayout.Bounds::maxX).reversed()).toList();
        var right=groups.stream().filter(b->b.minX()>0).sorted(Comparator.comparingDouble(SkillBranchLayout.Bounds::minX)).toList();
        if(!left.isEmpty())near(-left.get(0).maxX(),SkillBranchLayout.TRUNK_GAP);
        if(!right.isEmpty())near(right.get(0).minX(),SkillBranchLayout.TRUNK_GAP);
        for(int i=1;i<left.size();i++)near(left.get(i-1).minX()-left.get(i).maxX(),SkillBranchLayout.BRANCH_GAP);
        for(int i=1;i<right.size();i++)near(right.get(i).minX()-right.get(i-1).maxX(),SkillBranchLayout.BRANCH_GAP);
        var reversed=new ArrayList<>(nodes);Collections.reverse(reversed);
        check(layout(reversed).equals(layout)); // Incoming snapshot order never controls placement.
    }
    public static void main(String[] args) throws Exception {
        check(layout(List.of()).nodes().isEmpty());
        near(layout(List.of(ROOT)).nodes().get("root").x(),0);
        var cat=node("cat","feline",-23,11);
        var land=node("land","land",-19,11);
        var dragon=node("dragon","dragon",-27,11);
        var sparse=layout(List.of(ROOT,cat,land));
        var added=layout(List.of(ROOT,cat,land,dragon));
        near(sparse.nodes().get("land").x(),added.nodes().get("land").x());
        near(sparse.nodes().get("cat").x(),added.nodes().get("cat").x());
        check(added.nodes().get("dragon").x()<added.nodes().get("cat").x());
        // Width derives from extents, not the number of vertically stacked nodes.
        var shortBranch=List.of(land,cat);
        var tallBranch=new ArrayList<>(shortBranch);
        for(int i=0;i<20;i++)tallBranch.add(node("land"+i,"land",-19,12+i));
        near(layout(shortBranch).nodes().get("cat").x(),layout(tallBranch).nodes().get("cat").x());
        tallBranch.add(node("wide","land",-22,40));
        check(layout(tallBranch).nodes().get("cat").x()<layout(shortBranch).nodes().get("cat").x());
        var arth=node("arth","arthropod",-35,11);
        var insect=node("insect","insect",-32,13);
        var arachnid=node("arachnid","arachnid",-38,13);
        var nested=layout(List.of(ROOT,land,arth,insect,arachnid));
        check(nested.groups().size()==2);
        near(nested.nodes().get("insect").x()-nested.nodes().get("arth").x(),3);
        near(nested.nodes().get("arachnid").x()-nested.nodes().get("arth").x(),-3);
        check(layout(List.of(arth)).groups().get("changede:arthropod").width()<nested.groups().get("changede:arthropod").width());
        verify(List.of(ROOT,land,arth,insect,arachnid),nested);
        var style=new SkillRegionTemplate("cat","changede:feline","dark",.6,10,.75,Map.of(),Set.of());
        var region=style.resolve(added.branches().get("changede:feline"));
        near(region.x(),added.nodes().get("cat").x()-.75);
        near(region.width(),1.5);near(region.height(),1.5);
        near(SkillRegionBlend.weights(List.of(region),"lab",added.nodes().get("cat").x(),11).get("dark"),1);
        check(!SkillRegionBlend.weights(List.of(region),"lab",cat.x(),11).containsKey("dark"));
        var padded=SkillBranchLayout.arrange(List.of(cat),Map.of("changede:feline",2.0));
        near(padded.groups().get("changede:feline").maxX(),-SkillBranchLayout.TRUNK_GAP);
        check(!padded.branches().containsKey("changede:sea"));
        if(args.length>0) {
            List<SkillBranchLayout.Node> all=new ArrayList<>();
            List<String[]> forms=new ArrayList<>();
            for(String line:Files.readAllLines(Path.of(args[0]))) {
                String[] p=line.split("\t",-1);
                if(p[0].equals("node"))all.add(new SkillBranchLayout.Node(p[1],p[2],Double.parseDouble(p[3]),Double.parseDouble(p[4]),Boolean.parseBoolean(p[5])));
                else forms.add(p);
            }
            verify(all,layout(all)); // All-preview mode.
            for(var form:forms) {
                Set<String> branches=form[2].isEmpty() ? Set.of() : new HashSet<>(Arrays.asList(form[2].split(",")));
                var visible=all.stream().filter(n->n.global() || branches.contains(n.branch())).toList();
                var current=layout(visible);verify(visible,current);
                check(current.branches().keySet().equals(branches));
                // Hidden sibling branches do not contribute width or background bounds.
                for(var n:visible) if(!n.global()) {
                    var template=new SkillRegionTemplate(n.branch(),n.branch(),"lab",.6,10,.75,Map.of(),Set.of());
                    var r=template.resolve(current.branches().get(n.branch()));
                    var pos=current.nodes().get(n.id());
                    check(r.x()<pos.x() && pos.x()<r.x()+r.width() && r.y()<pos.y() && pos.y()<r.y()+r.height());
                }
            }
            System.out.println("PASS: "+all.size()+" real nodes, "+forms.size()+" Form layouts and all-preview layout.");
        }
        System.out.println("PASS: fixed sides/order, width packing, nested groups, centered trunk, dynamic regions and padding.");
    }
}
